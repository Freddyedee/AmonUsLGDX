package com.amongus.core.impl.session;

import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.*;
import com.amongus.core.api.map.GameMap;
import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.session.GameSession;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.Task;
import com.amongus.core.model.Position;

import java.util.*;

public class GameSesionImpl implements GameSession {


    /**
     * Identificador único en la partida.
     * Permite distinguir sesiones en escenarios de multijugador o pruebas.
     * */
    private final UUID sessionId;

    /**}
     * Canal de eventos del dominio.
     * Se utiliza para notificar al exterior lo que ocurre en el juego.
     * sin acoplar esta clase a UI o infraestructura.
     * */

    private final EventBus eventBus;

    /**
     * Mapa del juego.
     * El GameSession no sabe como funciona el mapa internamente,
     * solo consulta si una acción es válida.
     * */
    private final GameMap gameMap;

    /**
     * Estado actual de la partida
     * Controla en que fase del juego nos encontramos
     * ya sea lobby, in game, meeting, ended, etc.
     * */
    private GameState currentState;

    /*
     *Conjunto de jugadores de la partida.
     * Se indexan por id, es decir, PlayerId para accesar rapido y seguro.
     * */
    private final Map<PlayerId, Player> players;


    /**
     * Tareas asignadas a los jugadores.
     * No todas las partidas las usan de inmediato,
     * pero el core mantiene la estructura preparada.
     */
    //private final Map<PlayerId, List<Task>> taskByPlayer;

    /*
    * Votos emitidos durante una fase de votación.
    * Solo son válidos cuando el estado del juego lo permite.
    * */

    private final Map<PlayerId, Vote> currentVotes;

    /*
     *Constructor principal de la sesión.
     *
     * Aquí se implementan todas las dependencias necesarias.
     * Esto facilita pruebas, reemplazos y extensiones.
     * */

    public GameSesionImpl(UUID sessionId, EventBus eventBus, GameMap gameMap){
        this.sessionId = UUID.randomUUID();
        this.eventBus = eventBus;
        this.gameMap = gameMap;

        this.currentState = GameState.LOBBY;

        this.currentState = GameState.LOBBY;
        this.players = new HashMap<>();
        //this.taskByPlayer = new HashMap<>();
        this.currentVotes = new HashMap<>();
    }

    // --------------------------------------------------
    // GESTIÓN DE JUGADORES
    // --------------------------------------------------

    /**
     * Agrega un jugador a la partida.
     *
     * Regla:
     *  - Solo se pueden unir jugadores en estado LOBBY.
     */

    @Override
    public void addPlayer(Player player){
        if(currentState != GameState.LOBBY){
            throw new IllegalStateException("No se pueden unir jugadores una vez iniciada la partida");
        }

        players.put(player.getId(), player);

        //Se notifica al exterior que un jugador se ha unido.
        eventBus.publish(new PlayerJoinedEvent(player.getId()));

    }

    /**
     * Inicia la partida.
     *
     * Regla:
     *  - Debe haber al menos un número mínimo de jugadores
     *  - El estado pasa de LOBBY a PLAYING
     */
    @Override
    public void startGame(){
        if(currentState != GameState.LOBBY){
            throw new IllegalStateException("Partida en curso");
        }

        if(players.size() < 4){
            throw new IllegalStateException("No hay suficientes jugadores para iniciar la partida");
        }

        currentState = GameState.IN_GAME;
        eventBus.publish(new GameStartedEvent(sessionId));
    }

    // --------------------------------------------------
    // MOVIMIENTO
    // --------------------------------------------------

    /**
     * Mueve a un jugador en el mapa.
     *
     * Reglas:
     *  - El jugador debe estar vivo
     *  - El estado del juego debe permitir movimiento
     *  - El mapa debe autorizar el desplazamiento
     */

    @Override
    public void movePlayer(PlayerId playerId, Object newPosition){
        Player player = players.get(playerId);

        if(player == null){
            throw new IllegalArgumentException("Jugador inexistente");
        }

        if(!player.alive()){
            throw new IllegalStateException("Jugador muerto. Imposible moverse");
        }

        if(currentState != GameState.IN_GAME){
            throw new IllegalStateException("Estaod actual no permite movimientos");
        }

        player = getAlivePlayer(playerId);

        if(!gameMap.canMove(null, null)){
            return;
        }

        eventBus.publish(new PlayerMovedEvent(playerId, (Position) newPosition));


    }

     /* ============================================================
       ASESINATOS
       ============================================================ */

    /**
     * Intenta realizar un asesinato.
     *
     * La sesión valida:
     * - estado del juego
     * - roles
     * - vida de los jugadores
     */

    @Override
    public void attemptKill(PlayerId killerId, PlayerId victimId){
        requireState(GameState.IN_GAME);

        Player killer = getAlivePlayer(killerId);
        Player victim = getAlivePlayer(victimId);

        if(killer.getRole() != Role.IMPOSTOR){
            return;
        }

        victim.kill();
        eventBus.publish(new KillAttemptedEvent(killerId, victimId));
    }

    /* ============================================================
       REPORTE Y REUNIÓN
       ============================================================ */

    /**
     * Reporta un cuerpo y activa una reunión.
     */

    @Override
    public void reportBody(PlayerId reporter, PlayerId victim){
        requireState(GameState.IN_GAME);

        currentState = GameState.MEETING;
        currentVotes.clear();

        eventBus.publish(new BodyReportedEvent(reporter, victim));
        eventBus.publish(new VotingStartedEvent());
    }

    /* ============================================================
       VOTACIÓN
       ============================================================ */

    /**
     * Registra un voto durante una reunión.
     */

    @Override
    public void castVote(Vote vote){
        requireState(GameState.MEETING);

        Player voter = getAlivePlayer(vote.getVoterId());
        currentVotes.put(voter.getId(), vote);

        eventBus.publish(new VoteCastEvent(vote.getVoterId(), vote.getTargetId()));
    }

    /**
     * Resuelve la votación actual.
     */
    @Override
    public void resolveVoting(){
        requireState(GameState.MEETING);

        Map<PlayerId, Integer> count = new HashMap<>();

        for (Vote vote : currentVotes.values()){
            if(vote.isSkip() || vote.getTargetId() == null){
                continue;
            }

            count.merge(vote.getTargetId(), 1, Integer::sum);
        }

        PlayerId ejected = count.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if(ejected != null){
            players.get(ejected).kill();
        }

        currentState = GameState.IN_GAME;
        eventBus.publish(new VotingResolvedEvent(ejected));
    }

    /* ============================================================
       CONSULTAS
       ============================================================ */

    @Override
    public GameState getCurrentState() {
        return currentState;
    }

    @Override
    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

/* ============================================================
       MÉTODOS AUXILIARES (PRIVADOS)
       ============================================================ */

    /**
     * Válida que el juego esté en un estado específico.
     */
    private void requireState(GameState expected) {
        if (currentState != expected) {
            throw new IllegalStateException(
                    "Acción inválida en estado: " + currentState
            );
        }
    }


    /**
     * Obtiene un jugador válido y vivo.
     * Este méthod centraliza una invariante del juego:
     * muchas acciones solo pueden ser realizadas por jugadores vivos.
     *
     * Centralizar esta lógica:
     * - evita duplicación de código
     * - mejora legibilidad
     * - facilita mantenimiento y explicación académica
     *
     * @param playerId identificador del jugador
     * @return jugador vivo
     * @throws IllegalArgumentException si el jugador no existe
     * @throws IllegalStateException si el jugador está muerto
     */
    private Player getAlivePlayer(PlayerId playerId) {

        Player player = players.get(playerId);

        if (player == null) {
            throw new IllegalArgumentException("Jugador inexistente");
        }

        if (!player.alive()) {
            throw new IllegalStateException("Jugador muerto");
        }

        return player;
    }



}
