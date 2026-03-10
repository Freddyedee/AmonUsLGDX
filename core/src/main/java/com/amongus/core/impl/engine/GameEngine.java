package com.amongus.core.impl.engine;

import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.EventBus;
import com.amongus.core.api.map.GameMap;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.minigame.MinigameScreen; // Nuevo de Eliuber
import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.session.GameSession;
import com.amongus.core.api.session.TaskProgressTracker; // Nuevo de Eliuber
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.Task; // Nuevo de Eliuber
import com.amongus.core.api.task.TaskId; // Nuevo de Eliuber
import com.amongus.core.impl.event.EventBusImpl;
import com.amongus.core.impl.map.MaskCollisionMap;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.impl.player.ColorAssigner;
import com.amongus.core.impl.session.GameSessionImpl;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.amongus.core.impl.player.PlayerImpl;
import com.amongus.core.impl.voting.VotingSystemImpl;
import com.amongus.core.view.TaskView; // Nuevo de Eliuber

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.fromString;

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

    // Pantalla de minijuego (Nuevo de Eliuber)
    private MinigameScreen activeMinigame = null;

    // Flag de debug (Nuevo de Eliuber)
    private static final boolean DEBUG_ROLES = true;

    public GameEngine(MapType mapType){
        this.sessionId = UUID.randomUUID();
        this.eventBus = new EventBusImpl();
        this.mapType = mapType;

        // Pasamos 1500x1000 para la Sala de Espera
        this.gameMap = new MaskCollisionMap("mapas/SalaEsperaColisiones.png");
        this.session = new GameSessionImpl(sessionId, eventBus, gameMap, this);
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

    // Nuevo método de Eliuber para interactuar con tareas
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
        return newId;
    }

    public PlayerId spawnPlayerWithId(String uuidStr, String name, SkinColor networkColor) {
        PlayerId newId = new PlayerId(java.util.UUID.fromString(uuidStr));
        SkinColor assignedColor = colorAssigner.assignForce(networkColor);
        Player player = new PlayerImpl(newId, name, assignedColor);
        session.addPlayer(player);
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
        if (distance <= 150.0) {
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
        session.reportBody(reporterId, victimId);
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

        long alive = session.getPlayers().stream().filter(Player::alive).count();
        long impostors = session.getPlayers().stream().filter(Player::alive)
            .filter(p->p.getRole() == Role.IMPOSTOR).count();

        if(impostors >= alive - impostors){
            gameResult = "IMPOSTOR";
        }else if(impostors == 0){
            gameResult = "CREWMATE";
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
        votingSystem.clearVotes();

        if (this.gameMap instanceof MaskCollisionMap) {
            ((MaskCollisionMap) this.gameMap).dispose();
        }
        MaskCollisionMap newMap = new MaskCollisionMap("mapas/SalaEsperaColisiones.png");
        this.gameMap = newMap;

        if (session instanceof GameSessionImpl) {
            ((GameSessionImpl) session).setGameMap(newMap);
            ((GameSessionImpl) session).resetToLobby();
        }

        for (Player p : session.getPlayers()) {
            movePlayer(p.getId(), new Position(1920f, 1080f));
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

    // --- MÉTODOS DE MINIJUEGOS (Nuevos de Eliuber) ---
    public void setActiveMinigame(MinigameScreen screen) {
        this.activeMinigame = screen;
        if (screen != null) screen.show();
    }
    public MinigameScreen getActiveMinigame() { return activeMinigame; }
    public void clearActiveMinigame()        { this.activeMinigame = null; }

    public String getGameResult() { return gameResult; }
    public java.util.Set<PlayerId> getVotedPlayers() { return votingSystem.getVotedPlayers(); }
}
