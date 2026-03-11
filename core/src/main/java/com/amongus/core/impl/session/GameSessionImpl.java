package com.amongus.core.impl.session;

import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.*;
import com.amongus.core.api.map.GameMap;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.session.GameSession;
import com.amongus.core.api.session.TaskProgressTracker;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.player.PlayerImpl;
import com.amongus.core.impl.state.GameStateMachine;
import com.amongus.core.impl.task.TaskFactory;
import com.amongus.core.model.Position;

import java.util.*;
import java.util.stream.Stream;

public class GameSessionImpl implements GameSession {

    private final GameEngine engine;
    private final UUID sessionId;
    private final EventBus eventBus;
    private GameMap gameMap;
    private final GameStateMachine stateMachine;
    private final Map<PlayerId, Player> players;
    private final Map<PlayerId, Vote> currentVotes;

    // --------------------------------------------------
    // ATRIBUTOS RELACIONADOS CON TASK (De Eliuber)
    // -------------------------------------------------
    private final Map<TaskId, Task> allTasks;
    private final Map<PlayerId, Set<TaskId>> assignedTaskIdsByPlayer;   // solo las que aún debe hacer
    private final Map<PlayerId, Set<TaskId>> completedTaskIdsByPlayer;  // las que ya terminó
    private final TaskFactory taskFactory;
    private TaskProgressTracker progressTracker;

    public GameSessionImpl(UUID sessionId, EventBus eventBus, GameMap gameMap, GameEngine engine){
        this.engine = engine;
        this.sessionId = UUID.randomUUID();
        this.eventBus = eventBus;
        this.gameMap = gameMap;

        this.stateMachine = new GameStateMachine();
        this.players = new HashMap<>();
        this.currentVotes = new HashMap<>();

        // Inicialización de Tareas
        this.taskFactory = new TaskFactory(engine);
        this.allTasks = new HashMap<>();
        this.assignedTaskIdsByPlayer = new HashMap<>();
        this.completedTaskIdsByPlayer = new HashMap<>();

        // Escuchar eventos de tareas completadas
        eventBus.subscribe(TaskCompletedEvent.class, event -> {
            PlayerId pid = event.getPlayerId();
            TaskId   tid = event.getTaskId();

            assignedTaskIdsByPlayer.getOrDefault(pid, new HashSet<>()).remove(tid);
            completedTaskIdsByPlayer.computeIfAbsent(pid, k -> new HashSet<>()).add(tid);

            // SOLO AVANZA LA BARRA SI EL JUGADOR ES TRIPULANTE
            Player player = players.get(pid);
            if (player != null && player.getRole() == Role.CREWMATE && progressTracker != null) {
                progressTracker.taskCompleted();
                System.out.println("[progreso] pendientes: " + progressTracker.getPending()
                    + "/" + progressTracker.getTotal());
            }
        });
    }

    // --------------------------------------------------
    // GESTIÓN DE JUGADORES
    // --------------------------------------------------

    @Override
    public void addPlayer (Player player){
        if(stateMachine.getCurrentState() != GameState.LOBBY && stateMachine.getCurrentState() != GameState.IN_GAME){
            throw new IllegalStateException("No se pueden unir jugadores en este estado");
        }
        players.put(player.getId(), player);
        eventBus.publish(new PlayerJoinedEvent(player.getId()));
    }

    @Override
    public void startGame(){
        if (players.size() < 1){
            throw new IllegalStateException("No hay suficientes jugadores");
        }

        // --- 1. LIMPIAR DATOS ANTERIORES ---
        allTasks.clear();
        assignedTaskIdsByPlayer.clear();
        completedTaskIdsByPlayer.clear();

        int totalTasks = 0;

        // Obtenemos el mapa actual
        MapType currentMap = engine.getMapType();

        // --- 2. ASIGNAR 4 TAREAS ALEATORIAS A TODOS LOS JUGADORES ---
        for (Player p : players.values()) {

            // Generamos la piscina de misiones según el mapa
            List<List<Task>> taskPool = new ArrayList<>();

            if (currentMap == com.amongus.core.api.map.MapType.MAPA_1) {
                // --- MAPA 1: Aulas Principal ---
                taskPool.add(List.of(taskFactory.createToiletTask(new Position(3175f, 1874f))));
                taskPool.add(List.of(taskFactory.createWhiteBoardTask(new Position(1800f, 1663f))));
                taskPool.add(List.of(taskFactory.createWiresTask(new Position(763f, 107f))));
                taskPool.add(List.of(taskFactory.createBotellonTask(new Position(545f, 577f))));
                taskPool.add(List.of(taskFactory.createNumberTask(new Position(2495f, 589f))));
                taskPool.add(taskFactory.createTrashTask(new Position(2065f, 609f), new Position(1646f, 331f)));
                taskPool.add(List.of(taskFactory.createLibraryTask(new Position(2203f, 1740f))));
            } else {
                // --- MAPA 2: Cancha y Estacionamiento ---
                taskPool.add(List.of(taskFactory.createToiletTask(new Position(55f, 1339f))));
                taskPool.add(List.of(taskFactory.createWhiteBoardTask(new Position(981f, 940f))));
                taskPool.add(List.of(taskFactory.createWiresTask(new Position(1670f, 2055f))));
                taskPool.add(List.of(taskFactory.createBotellonTask(new Position(3212f, 548f))));
                taskPool.add(List.of(taskFactory.createNumberTask(new Position(1560f, 510f))));
                taskPool.add(taskFactory.createTrashTask(new Position(1740f, 1086f), new Position(1902f, 1537f)));
                taskPool.add(List.of(taskFactory.createBasketTask(new Position(2797f, 552f))));
            }

            // Mezclamos la piscina de misiones
            Collections.shuffle(taskPool);

            // Seleccionamos solo las primeras 4 misiones
            List<List<Task>> chosenMissions = taskPool.subList(0, 4);
            Set<TaskId> pending = new HashSet<>();

            for (List<Task> mission : chosenMissions) {
                for (Task t : mission) {
                    allTasks.put(t.getId(), t);
                    pending.add(t.getId());

                    if (p.getRole() == Role.CREWMATE) {
                        totalTasks++;
                    }
                }
            }
            assignedTaskIdsByPlayer.put(p.getId(), pending);
        }

        // --- 3. INICIALIZAR LA BARRA DE PROGRESO ---
        final int finalTotalTasks = totalTasks;
        this.progressTracker = new TaskProgressTracker(totalTasks) {
            private int completed = 0;
            @Override public void taskCompleted() { completed++; }
            @Override public int getTotal() { return finalTotalTasks; }
            @Override public int getCompleted() { return completed; }
            @Override public int getPending() { return finalTotalTasks - completed; }
        };

        stateMachine.transitionTo(GameState.IN_GAME);
        eventBus.publish(new GameStartedEvent(sessionId));
    }

    // --------------------------------------------------
    // MOVIMIENTO
    // --------------------------------------------------

    @Override
    public void movePlayer(PlayerId playerId, Object newPosition) {
        if (stateMachine.getCurrentState() != GameState.IN_GAME &&
            stateMachine.getCurrentState() != GameState.LOBBY) {
            throw new IllegalStateException("Acción inválida en estado: " + stateMachine.getCurrentState());
        }

        Player player = getAlivePlayer(playerId);
        Position targetPos = (Position) newPosition;

        float cx = player.getPosition().x();
        float cy = player.getPosition().y();
        float dx = targetPos.x() - cx;
        float dy = targetPos.y() - cy;

        if (dx == 0 && dy == 0) return;

        if (Math.hypot(dx, dy) > 150) {
            return;
        }

        if (gameMap.canMove(player.getPosition(), targetPos)) {
            player.updatePosition(targetPos);
            eventBus.publish(new PlayerMovedEvent(playerId, targetPos));
            return;
        }

        double length = Math.hypot(dx, dy);
        double originalAngleRad = Math.atan2(dy, dx);
        int[] angleOffsetsDeg = {15, -15, 30, -30, 45, -45, 55, -55};

        for (int offsetDeg : angleOffsetsDeg) {
            double offsetRad = Math.toRadians(offsetDeg);
            double testAngleRad = originalAngleRad + offsetRad;

            float testX = cx + (float) (length * Math.cos(testAngleRad));
            float testY = cy + (float) (length * Math.sin(testAngleRad));

            Position testPos = new Position(testX, testY);

            if (gameMap.canMove(player.getPosition(), testPos)) {
                player.updatePosition(testPos);
                eventBus.publish(new PlayerMovedEvent(playerId, testPos));
                return;
            }
        }
    }

    // --------------------------------------------------
    // ASESINATOS, REPORTE Y VOTACIÓN
    // --------------------------------------------------

    @Override
    public void attemptKill(PlayerId killerId, PlayerId victimId){
        requireState(GameState.IN_GAME);
        Player killer = getAlivePlayer(killerId);
        Player victim = getAlivePlayer(victimId);

        if(killer.getRole() != Role.IMPOSTOR){ return; }

        victim.kill();
        eventBus.publish(new KillAttemptedEvent(killerId, victimId));
    }

    @Override
    public void reportBody(PlayerId reporter, PlayerId victim){
        requireState(GameState.IN_GAME);
        stateMachine.transitionTo(GameState.MEETING);
        currentVotes.clear();
        eventBus.publish(new BodyReportedEvent(reporter, victim));
        eventBus.publish(new VotingStartedEvent());
    }

    // Metodo exclusivo para emergencias
    public void callEmergencyMeeting(PlayerId caller){
        requireState(GameState.IN_GAME);
        stateMachine.transitionTo(GameState.MEETING);
        currentVotes.clear();
        // Publicamos el evento con 'null' en la víctima para indicar que es emergencia
        eventBus.publish(new BodyReportedEvent(caller, null));
        eventBus.publish(new VotingStartedEvent());
    }

    @Override
    public void castVote(Vote vote){
        requireState(GameState.MEETING);
        Player voter = getAlivePlayer(vote.getVoterId());
        currentVotes.put(voter.getId(), vote);
        eventBus.publish(new VoteCastEvent(vote.getVoterId(), vote.getTargetId()));
    }

    @Override
    public void resolveVoting(){
        requireState(GameState.MEETING);
        PlayerId ejected = null;
        stateMachine.transitionTo(GameState.IN_GAME);
        eventBus.publish(new VotingResolvedEvent(ejected));
    }

    // --------------------------------------------------
    // CONSULTAS
    // --------------------------------------------------

    @Override
    public GameState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    @Override
    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /* ============================================================
     TAREAS TASK
    ============================================================ */
    @Override
    public List<Task> getTasksForPlayer(PlayerId playerId) {
        Set<TaskId> assignedIds = assignedTaskIdsByPlayer.getOrDefault(playerId, Set.of());
        return assignedIds.stream()
            .map(allTasks::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private boolean isTaskPendingFor(PlayerId playerId, TaskId taskId) {
        return assignedTaskIdsByPlayer.getOrDefault(playerId, Set.of()).contains(taskId) &&
            !completedTaskIdsByPlayer.getOrDefault(playerId, Set.of()).contains(taskId);
    }

    @Override
    public void initiateTask(PlayerId playerId, TaskId taskId) {
        requireState(GameState.IN_GAME);

        if (!isTaskPendingFor(playerId, taskId)) return;

        Task task = allTasks.get(taskId);
        if (task == null) return;

        Player player = players.get(playerId);
        if (!task.canInteract(playerId, player.getPosition())) return;

        System.out.println("[initiateTask] ABRIENDO minijuego: " + task.getName());
        MinigameScreen screen = task.getMinigameProvider().createScreen(playerId, task);
        engine.setActiveMinigame(screen);
        eventBus.publish(new TaskInteractionStartedEvent(playerId, task.getId()));
    }

    public boolean isTaskCompleted(PlayerId playerId, TaskId taskId) {
        return completedTaskIdsByPlayer
            .getOrDefault(playerId, Set.of())
            .contains(taskId);
    }

    public TaskProgressTracker getProgressTracker() { return progressTracker; }

    public List<Task> getAllTasksForPlayer(PlayerId playerId) {
        Set<TaskId> assigned  = assignedTaskIdsByPlayer.getOrDefault(playerId, Set.of());
        Set<TaskId> completed = completedTaskIdsByPlayer.getOrDefault(playerId, Set.of());

        return Stream.concat(assigned.stream(), completed.stream())
            .distinct()
            .map(allTasks::get)
            .filter(Objects::nonNull)
            .toList();
    }

    // --------------------------------------------------
    // MÉTODOS AUXILIARES Y NUESTROS FIXES DEL LOBBY
    // --------------------------------------------------

    private void requireState(GameState expected) {
        if (stateMachine.getCurrentState() != expected) {
            throw new IllegalStateException("Acción inválida en estado: " + stateMachine.getCurrentState());
        }
    }

    private Player getAlivePlayer(PlayerId playerId) {
        Player player = players.get(playerId);
        if (player == null) throw new IllegalArgumentException("Jugador inexistente");
        if (!player.alive()) throw new IllegalStateException("Jugador muerto");
        return player;
    }

    // --- MÉTODOS DE RED QUE MANTUVIMOS ---
    public void removePlayer(PlayerId id) {
        if (this.players != null) {
            this.players.remove(id);
        }
    }

    public void resetToLobby() {
        stateMachine.reset();
        currentVotes.clear();

        // Limpiamos las tareas al volver al lobby
        allTasks.clear();
        assignedTaskIdsByPlayer.clear();
        completedTaskIdsByPlayer.clear();

        for (Player p : players.values()) {
            if (p instanceof PlayerImpl pi) {
                pi.revive();
                pi.assignRole(Role.CREWMATE);
            }
        }
    }

    public void setGameMap(GameMap gameMap) {
        this.gameMap = gameMap;
    }
}
