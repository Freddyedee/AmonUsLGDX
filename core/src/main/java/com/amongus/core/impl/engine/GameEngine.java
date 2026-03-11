package com.amongus.core.impl.engine;

import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.EventBus;
import com.amongus.core.api.events.KillAttemptedEvent;
import com.amongus.core.api.events.TaskCompletedEvent;
import com.amongus.core.api.map.GameMap;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.session.GameSession;
import com.amongus.core.api.session.TaskProgressTracker;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.event.EventBusImpl;
import com.amongus.core.impl.map.MaskCollisionMap;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.impl.player.ColorAssigner;
import com.amongus.core.impl.rules.GameRules;
import com.amongus.core.impl.session.GameSessionImpl;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.amongus.core.impl.player.PlayerImpl;
import com.amongus.core.impl.voting.VotingSystemImpl;
import com.amongus.core.view.TaskView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GameEngine {

    private final UUID sessionId;
    private final EventBus eventBus;
    private final GameSession session;
    private GameMap gameMap;
    private MapType mapType;
    private PlayerId localPlayerId;
    private final VotingSystemImpl votingSystem = new VotingSystemImpl();
    private String gameResult = null;
    private final ColorAssigner colorAssigner = new ColorAssigner();
    // --- VARIABLES DE REUNIÓN ---
    private PlayerId currentReporterId = null;
    private PlayerId currentVictimId = null;

    public PlayerId getCurrentReporterId() { return currentReporterId; }
    public boolean isEmergencyMeeting() {
        return currentVictimId != null && currentVictimId.value().equals(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    // Pantalla de minijuego (Nuevo de Eliuber)
    private MinigameScreen activeMinigame = null;

    // Flag de debug (Nuevo de Eliuber)
    private static final boolean DEBUG_ROLES = true;

    public GameEngine(MapType mapType){
        this.sessionId = UUID.randomUUID();
        this.eventBus = new EventBusImpl();
        this.mapType = mapType;

        this.gameMap = new MaskCollisionMap("mapas/SalaEsperaColisiones.png");
        this.session = new GameSessionImpl(sessionId, eventBus, gameMap, this);

        // Escuchamos eventos clave para evaluar si el juego terminó
        this.eventBus.subscribe(KillAttemptedEvent.class, event -> {
            checkWinConditions();
        });
        this.eventBus.subscribe(TaskCompletedEvent.class, event -> {
            checkWinConditions();
        });
    }

    // Getter para que la vista gráfica sepa qué dibujar
    public MapType getMapType() {
        return mapType;
    }
    public void setMapType(MapType mapType) {
        this.mapType = mapType;
    }

    /* ===================== CONSULTAS ===================== */

    // Modificado por Eliuber para incluir las tareas en el Snapshot
    public GameSnapshot getSnapshot() {
        List<PlayerView> playerViews = session.getPlayers().stream()
            .map(p -> {
                SkinColor color = (p instanceof PlayerImpl pi) ? pi.getSkinColor() : SkinColor.AZUL;
                PlayerView view = new PlayerView(p.getId(), p.alive(), p.getPosition(), p.getName(), color, p.getRole());

                if (p instanceof PlayerImpl pi) {
                    view.setMoving(pi.isMoving());
                    view.setDirection(pi.getDirection());
                    view.setVenting(pi.isVenting());
                }
                return view;
            })
            .toList();

        // Extraer tareas del jugador local
        List<Task> playerTasks = session.getAllTasksForPlayer(localPlayerId);

        // Convertir a TaskView
        List<TaskView> taskViews = playerTasks.stream()
            .map(t -> {
                boolean done = session.isTaskCompleted(localPlayerId, t.getId());
                return new TaskView(t, done);
            })
            .toList();

        // Extraer progreso general
        TaskProgressTracker tracker = session.getProgressTracker();
        int total     = (tracker != null) ? tracker.getTotal()     : 0;
        int completed = (tracker != null) ? tracker.getCompleted() : 0;

        // Retornar Snapshot con los nuevos parámetros de tareas
        return new GameSnapshot(
            session.getCurrentState(),
            playerViews,
            localPlayerId,
            taskViews,
            total,
            completed
        );
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public GameState getGameState() {
        return session.getCurrentState();
    }

    // Nuevo metodo para interactuar con tareas
    public void initiateTask(TaskId taskId) {
        session.initiateTask(localPlayerId, taskId);
    }

    /* ===================== CASOS DE USO ===================== */

    public PlayerId spawnPlayer(String name, SkinColor preferredColor) {
        PlayerId newId = new PlayerId(java.util.UUID.randomUUID());
        SkinColor assignedColor = colorAssigner.assign(preferredColor);
        Player player = new PlayerImpl(newId, name, assignedColor);
        session.addPlayer(player);
        if (this.localPlayerId == null) this.localPlayerId = newId;
        forceMovePlayer(newId, new Position(1920f, 1080f));
        return newId;
    }

    public PlayerId spawnPlayerWithId(String uuidStr, String name, SkinColor networkColor) {
        PlayerId newId = new PlayerId(java.util.UUID.fromString(uuidStr));
        SkinColor assignedColor = colorAssigner.assignForce(networkColor);
        Player player = new PlayerImpl(newId, name, assignedColor);
        session.addPlayer(player);
        forceMovePlayer(newId, new Position(1920f, 1080f));
        return newId;
    }

    public void assignRole(PlayerId playerId, Role role){
        Player player = session.getPlayers().stream()
            .filter(p -> p.getId().equals(playerId))
            .findFirst().orElse(null);

        if(player instanceof  PlayerImpl){
            ((PlayerImpl) player).assignRole(role);
        }
    }

    public void requestKill(PlayerId killerId, PlayerId victimId) {
        Player killer = session.getPlayers().stream()
            .filter(p -> p.getId().equals(killerId))
            .findFirst().orElse(null);

        Player victim = session.getPlayers().stream()
            .filter(p -> p.getId().equals(victimId))
            .findFirst().orElse(null);

        if (killer == null || victim == null || !killer.alive() || !victim.alive()) {
            return;
        }

        double distance = Math.hypot(
            killer.getPosition().x() - victim.getPosition().x(),
            killer.getPosition().y() - victim.getPosition().y()
        );
        if (distance <= 100.0) {
            session.attemptKill(killerId, victimId);
        }
    }

    public void startGameHost(GameClient client) {
        java.util.List<Player> players = new java.util.ArrayList<>(session.getPlayers());
        PlayerId impostorId = players.get(new java.util.Random().nextInt(players.size())).getId();

        Position spawnPoint = getSpawnPositionForMap();

        for (Player p : players) {
            assignRole(p.getId(), p.getId().equals(impostorId) ? Role.IMPOSTOR : Role.CREWMATE);
            session.movePlayer(p.getId(), spawnPoint);
            forceMovePlayer(p.getId(), spawnPoint);
            setPlayerMoving(p.getId(), false, 1);
        }

        transitionToGameMap();
        session.startGame();

        if (client != null) {
            client.enviarMensaje("START_GAME:" + impostorId.value().toString());
        }
    }

    public void startGameClient(String impostorIdStr) {
        PlayerId impostorId = new PlayerId(java.util.UUID.fromString(impostorIdStr));
        Position spawnPoint = getSpawnPositionForMap();

        for (Player p : session.getPlayers()) {
            assignRole(p.getId(), p.getId().equals(impostorId) ? Role.IMPOSTOR : Role.CREWMATE);
            session.movePlayer(p.getId(), spawnPoint);
            forceMovePlayer(p.getId(), spawnPoint);
            setPlayerMoving(p.getId(), false, 1);
        }
        transitionToGameMap();
        session.startGame();
    }

    private void transitionToGameMap() {
        if (this.gameMap instanceof MaskCollisionMap) {
            ((MaskCollisionMap) this.gameMap).dispose();
        }

        MaskCollisionMap newMap = new MaskCollisionMap(mapType.getCollisionPath());
        this.gameMap = newMap;

        if (session instanceof GameSessionImpl) {
            ((GameSessionImpl) session).setGameMap(newMap);
        }
    }

    public void movePlayer(PlayerId playerId, Object destination) {
        session.movePlayer(playerId, destination);
    }

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

    public void castVote(Vote vote) {
        session.castVote(vote);
        votingSystem.castVote(vote);
    }

    public void reportBody(PlayerId reporterId, PlayerId victimId) {
        this.currentReporterId = reporterId;
        this.currentVictimId = victimId;

        if (isEmergencyMeeting()) {
            if (session instanceof GameSessionImpl) {
                ((GameSessionImpl) session).callEmergencyMeeting(reporterId);
            }
        } else {
            session.reportBody(reporterId, victimId);
        }
    }

    public PlayerId getLocalPlayerId() {
        return localPlayerId;
    }

    public Optional<PlayerId> resolveVoting(){
        Optional<PlayerId> expelled = votingSystem.resolve();

        expelled.ifPresent(id->{
            session.getPlayers().stream()
                .filter(p->p.getId().equals(id)).findFirst().ifPresent(Player::kill);
            System.out.println("[VOTACION] Expulsado: " + id);
        });

        session.resolveVoting();

        checkWinConditions();

        // Limpiamos los datos de la reunión
        this.currentReporterId = null;
        this.currentVictimId = null;

        // Teletransportamos a todos vivos a la mesa inicial al terminar
        Position spawnPoint = getSpawnPositionForMap();
        for (Player p : session.getPlayers()) {
            if (p.alive()) {
                forceMovePlayer(p.getId(), spawnPoint);
            }
        }

        return expelled;
    }

    public Position getNearestVent(Position pos, float maxDist) {
        return gameMap.getNearestVent(pos, maxDist);
    }

    public Position getNextVent(Position current, int dir) {
        return gameMap.getNextVentInNetwork(current, dir);
    }

    public void processVentAction(PlayerId pId, Position targetVent, boolean exiting) {
        Player player = session.getPlayers().stream().filter(p -> p.getId().equals(pId)).findFirst().orElse(null);
        if (player == null || !player.alive() || player.getRole() != Role.IMPOSTOR) return;

        player.setVenting(!exiting);
        if (!exiting && targetVent != null) {
            session.movePlayer(pId, targetVent);
        }
    }

    private Position getSpawnPositionForMap() {
        if (mapType == MapType.MAPA_1) {
            return new Position(1650f, 150f);
        } else {
            return new Position(1700f, 1600f);
        }
    }

    public void restartToLobby() {
        this.gameResult = null;
        this.currentReporterId = null;
        this.currentVictimId = null;
        votingSystem.clearVotes();

        // 2. Teletransportamos a TODOS al centro del autobús
        Position lobbySpawn = new Position(1920f, 1080f);
        for (Player p : session.getPlayers()) {
            // Los mandamos al centro
            forceMovePlayer(p.getId(), lobbySpawn);
            // Les apagamos cualquier estado de caminar fantasma
            setPlayerMoving(p.getId(), false, 1);
        }

        if (this.gameMap instanceof MaskCollisionMap) {
            ((MaskCollisionMap) this.gameMap).dispose();
        }
        MaskCollisionMap newMap = new MaskCollisionMap("mapas/SalaEsperaColisiones.png");
        this.gameMap = newMap;

        if (session instanceof GameSessionImpl) {
            ((GameSessionImpl) session).setGameMap(newMap);
            ((GameSessionImpl) session).resetToLobby();
        }

    }

    // --- NUESTROS MÉTODOS DE RED QUE MANTUVIMOS INTACTOS ---
    public void removePlayer(PlayerId id) {
        if (session instanceof GameSessionImpl) {
            ((GameSessionImpl) session).removePlayer(id);
        }
    }

    public void forceMovePlayer(PlayerId id, Position pos) {
        session.getPlayers().stream()
            .filter(p -> p.getId().equals(id))
            .filter(p -> p instanceof PlayerImpl)
            .map(p -> (PlayerImpl) p)
            .findFirst()
            .ifPresent(pi -> pi.setPosition(pos));
    }

    public void updatePlayerDirection(PlayerId id, int dir) {
        session.getPlayers().stream()
            .filter(p -> p.getId().equals(id))
            .filter(p -> p instanceof PlayerImpl)
            .map(p -> (PlayerImpl) p)
            .findFirst()
            .ifPresent(pi -> pi.setDirection(dir));
    }

    public void forceGameResult(String result) {
        this.gameResult = result;
    }

    public void changePlayerColor(PlayerId id, SkinColor newColor) {
        session.getPlayers().stream()
            .filter(p -> p.getId().equals(id))
            .filter(p -> p instanceof PlayerImpl)
            .map(p -> (PlayerImpl) p)
            .findFirst()
            .ifPresent(pi -> pi.setSkinColor(newColor));
    }

    // --- Metodo para verificar condiciones de victoria ---
    public void checkWinConditions() {
        if (com.amongus.debug.DebugConfig.IGNORE_WIN_CONDITIONS) return;
        if (gameResult != null) return; // Si ya terminó, no hacemos nada

        // 1. Usamos tu clase GameRules para verificar muertes/votos
        if (GameRules.gameOver(session.getPlayers())) {
            long impostorsAlive = session.getPlayers().stream()
                .filter(p -> p.alive() && p.getRole() == Role.IMPOSTOR)
                .count();

            if (impostorsAlive == 0) {
                gameResult = "CREWMATE";
                System.out.println("[FIN] ¡Los tripulantes ganan por expulsar al impostor!");
            } else {
                gameResult = "IMPOSTOR";
                System.out.println("[FIN] ¡El impostor gana por aniquilación!");
            }
            return;
        }

        // 2. Verificamos victoria por tareas completadas
        TaskProgressTracker tracker = session.getProgressTracker();
        if (tracker != null && tracker.getTotal() > 0 && tracker.getPending() == 0) {
            gameResult = "CREWMATE";
            System.out.println("[FIN] ¡Los tripulantes ganan por completar todas las tareas!");
        }
    }

    /**
     * Spawnea un bot de prueba que no se mueve.
     * Útil para probar asesinatos y reportes en solitario.
     */
    public void spawnTestingBot(String name, SkinColor color) {
        // Generamos un ID manual para el bot
        PlayerId botId = new PlayerId(java.util.UUID.randomUUID());
        Player bot = new PlayerImpl(botId, "[BOT] " + name, color);

        session.addPlayer(bot);

        session.getPlayers().stream()
            .filter(p -> p.getId().equals(localPlayerId))
            .findFirst()
            .ifPresent(me -> {
                forceMovePlayer(botId, new Position(me.getPosition().x() + 100, me.getPosition().y() + 100));
            });

        System.out.println("[DEBUG] Bot '" + name + "' invocado para pruebas.");
    }

    /**
     * Llama a este metodo cuando llegue un mensaje de red de que alguien terminó una tarea.
     * Esto actualizará la barra de progreso de TODOS los clientes simultáneamente.
     */
    public void notifyTaskCompletedByNetwork(PlayerId playerId, TaskId taskId) {
        // Al publicar este evento, el GameSessionImpl de quien lo reciba sumará 1 a la barra verde
        // y verificará si con esta tarea se ganó la partida.
        eventBus.publish(new TaskCompletedEvent(playerId, taskId));
    }

    // --- MÉTODOS DE MINIJUEGOS ---
    public void setActiveMinigame(MinigameScreen screen) {
        this.activeMinigame = screen;
        if (screen != null) screen.show();
    }
    public MinigameScreen getActiveMinigame() { return activeMinigame; }
    public void clearActiveMinigame()        { this.activeMinigame = null; }

    public String getGameResult() { return gameResult; }
    public java.util.Set<PlayerId> getVotedPlayers() { return votingSystem.getVotedPlayers(); }
}
