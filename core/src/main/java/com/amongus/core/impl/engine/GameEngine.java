package com.amongus.core.impl.engine;

import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.EventBus;
import com.amongus.core.api.map.GameMap;
import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.session.GameSession;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.event.EventBusImpl;
import com.amongus.core.impl.map.SimpleMap;
import com.amongus.core.impl.player.ColorAssigner;
import com.amongus.core.impl.player.PlayerImpl;
import com.amongus.core.impl.session.GameSessionImpl;
import com.amongus.core.impl.voting.VotingSystemImpl;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;

import java.util.*;

/**
 * Fachada principal del core del juego.
 *
 * <p>Único punto de entrada para que los módulos externos (UI, red, persistencia)
 * interactúen con el dominio. Ningún módulo externo debe conocer las clases internas.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Crear y mantener la sesión de juego</li>
 *   <li>Exponer operaciones de alto nivel (casos de uso)</li>
 *   <li>Delegar la lógica real a {@link GameSession}</li>
 *   <li>Gestionar el {@link EventBus}</li>
 * </ul>
 *
 * <p>No contiene reglas de negocio complejas ni conoce detalles de UI o red.
 */
public class GameEngine {

    // ── Infraestructura ───────────────────────────────────────────────
    private final UUID          sessionId;
    private final EventBus      eventBus;
    private final GameSession   session;
    private final GameMap       gameMap;
    private final ColorAssigner colorAssigner = new ColorAssigner();

    // ── Sistemas de dominio ───────────────────────────────────────────
    private final VotingSystemImpl votingSystem = new VotingSystemImpl();

    // ── Estado global ─────────────────────────────────────────────────
    private PlayerId localPlayerId = null;
    private String   gameResult    = null;

    // ══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════

    public GameEngine() {
        sessionId = UUID.randomUUID();
        eventBus  = new EventBusImpl();
        gameMap   = new SimpleMap();
        session   = new GameSessionImpl(sessionId, eventBus, gameMap);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ══════════════════════════════════════════════════════════════════

    /**
     * Devuelve una instantánea inmutable del estado actual del juego.
     * La UI solo debe leer desde aquí — nunca modificar el estado directamente.
     *
     * @return snapshot con el estado y la lista de {@link PlayerView}
     */
    public GameSnapshot getSnapshot() {
        List<PlayerView> views = session.getPlayers().stream()
            .map(p -> {
                SkinColor color = (p instanceof PlayerImpl pi)
                    ? pi.getSkinColor() : SkinColor.WHITE;
                PlayerView view = new PlayerView(
                    p.getId(), p.alive(), p.getPosition(), p.getName(), color);
                if (p instanceof PlayerImpl pi) {
                    view.setMoving(pi.isMoving());
                    view.setDirection(pi.getDirection());
                    view.setRole(pi.getRole());
                }
                return view;
            })
            .toList();

        return new GameSnapshot(session.getCurrentState(), views, localPlayerId);
    }

    /** @return bus de eventos para suscribirse a cambios del juego */
    public EventBus getEventBus() { return eventBus; }

    /** @return estado actual de la sesión (LOBBY, IN_GAME, MEETING, ENDED) */
    public GameState getGameState() { return session.getCurrentState(); }

    /**
     * @return {@code "IMPOSTOR"} si ganó el impostor, {@code "CREWMATE"} si
     *         ganaron los crewmates, {@code null} si la partida sigue en curso
     */
    public String getGameResult() { return gameResult; }

    /** @return ID del jugador controlado por este cliente */
    public PlayerId getLocalPlayerId() { return localPlayerId; }

    // ══════════════════════════════════════════════════════════════════
    //  GESTIÓN DE JUGADORES
    // ══════════════════════════════════════════════════════════════════

    /**
     * Registra un nuevo jugador con color asignado automáticamente.
     * El primer jugador registrado se convierte en el jugador local.
     *
     * @param name nombre visible en pantalla
     * @return ID único asignado al jugador
     */
    public PlayerId spawnPlayer(String name) {
        PlayerId id    = PlayerId.random();
        SkinColor color = colorAssigner.assign();
        session.addPlayer(new PlayerImpl(id, name, color));
        if (localPlayerId == null) localPlayerId = id;
        return id;
    }

    /**
     * Asigna un rol a un jugador existente.
     *
     * @param playerId jugador a asignar
     * @param role     {@link Role#IMPOSTOR} o {@link Role#CREWMATE}
     */
    public void assignRole(PlayerId playerId, Role role) {
        session.getPlayers().stream()
            .filter(p -> p.getId().equals(playerId))
            .filter(p -> p instanceof PlayerImpl)
            .map(p -> (PlayerImpl) p)
            .findFirst()
            .ifPresent(pi -> pi.assignRole(role));
    }

    /**
     * Asigna roles aleatoriamente entre los jugadores registrados.
     * Garantiza exactamente 1 impostor por partida.
     * Debe llamarse antes de startGame().
     */
    public void assignRolesRandomly() {
        List<Player> players = new ArrayList<>(session.getPlayers());
        Collections.shuffle(players);

        // El primero tras el shuffle es el impostor
        for (int i = 0; i < players.size(); i++) {
            Role role = (i == 0) ? Role.IMPOSTOR : Role.CREWMATE;
            if (players.get(i) instanceof PlayerImpl pi) {
                pi.assignRole(role);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  CICLO DE PARTIDA
    // ══════════════════════════════════════════════════════════════════

    /** Inicia la partida. Requiere estado LOBBY. */
    public void startGame() {
        if(session.getPlayers().size() < 5){
            throw new IllegalStateException(
                "Se necesitan minimo 5 jugadores para iniciar. " + "Actuale: " + session.getPlayers().size());
        }
        session.startGame();
    }

    /**
     * Mueve un jugador a una posición destino.
     *
     * @param playerId    jugador a mover
     * @param destination posición destino ({@link com.amongus.core.model.Position})
     */
    public void movePlayer(PlayerId playerId, Object destination) {
        session.movePlayer(playerId, destination);
    }

    /**
     * Actualiza el estado de movimiento y dirección visual de un jugador.
     * Usado por el sistema de animaciones del renderer.
     *
     * @param id        jugador a actualizar
     * @param moving    true si se está moviendo
     * @param direction 1 = derecha, -1 = izquierda
     */
    public void setPlayerMoving(PlayerId id, boolean moving, int direction) {
        session.getPlayers().stream()
            .filter(p -> p.getId().equals(id))
            .filter(p -> p instanceof PlayerImpl)
            .map(p -> (PlayerImpl) p)
            .findFirst()
            .ifPresent(pi -> {
                pi.setMoving(moving);
                pi.setDirection(direction);
            });
    }

    // ══════════════════════════════════════════════════════════════════
    //  KILL Y REPORTE
    // ══════════════════════════════════════════════════════════════════

    /**
     * Intenta ejecutar un kill. Valida que ambos jugadores estén vivos
     * y dentro del rango permitido (150 unidades).
     *
     * @param killerId ID del impostor atacante
     * @param victimId ID de la víctima objetivo
     */
    public void requestKill(PlayerId killerId, PlayerId victimId) {
        Player killer = findPlayer(killerId);
        Player victim = findPlayer(victimId);

        if (killer == null || victim == null
            || !killer.alive() || !victim.alive()) return;

        double dist = Math.hypot(
            killer.getPosition().x() - victim.getPosition().x(),
            killer.getPosition().y() - victim.getPosition().y());

        if (dist <= 150.0) {
            session.attemptKill(killerId, victimId);
        }
    }

    /**
     * Registra el reporte de un cadáver e inicia una reunión de emergencia.
     *
     * @param reporterId ID del jugador que reporta
     * @param victimId   ID del cadáver reportado
     */
    public void reportBody(PlayerId reporterId, PlayerId victimId) {
        session.reportBody(reporterId, victimId);
    }

    // ══════════════════════════════════════════════════════════════════
    //  VOTACIÓN
    // ══════════════════════════════════════════════════════════════════

    /**
     * Registra el voto de un jugador. Un voto con {@code target == null} es skip.
     *
     * @param vote voto a registrar
     */
    public void castVote(Vote vote) {
        session.castVote(vote);
        votingSystem.castVote(vote);
    }

    /**
     * Resuelve la votación, expulsa al jugador más votado (si aplica)
     * y verifica las condiciones de victoria.
     *
     * @return ID del jugador expulsado, o {@link Optional#empty()} si hubo empate o skip
     */
    public Optional<PlayerId> resolveVoting() {
        Optional<PlayerId> expelled = votingSystem.resolve();

        expelled.ifPresent(id -> {
            session.getPlayers().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .ifPresent(Player::kill);
        });

        session.resolveVoting();
        checkVictoryConditions();
        return expelled;
    }

    // ══════════════════════════════════════════════════════════════════
    //  LÓGICA INTERNA
    // ══════════════════════════════════════════════════════════════════

    /**
     * Evalúa las condiciones de victoria tras cada kill o expulsión.
     * <ul>
     *   <li>Impostores ganan si {@code impostors >= crewmates vivos}</li>
     *   <li>Crewmates ganan si no quedan impostores vivos</li>
     * </ul>
     */
    private void checkVictoryConditions() {
        long alive     = session.getPlayers().stream()
            .filter(Player::alive).count();
        long impostors = session.getPlayers().stream()
            .filter(Player::alive)
            .filter(p -> p.getRole() == Role.IMPOSTOR)
            .count();

        if (impostors >= alive - impostors) {
            gameResult = "IMPOSTOR";
        } else if (impostors == 0) {
            gameResult = "CREWMATE";
        }
    }

    /** Busca un jugador por ID en la sesión actual. */
    private Player findPlayer(PlayerId id) {
        return session.getPlayers().stream()
            .filter(p -> p.getId().equals(id))
            .findFirst().orElse(null);
    }
}
