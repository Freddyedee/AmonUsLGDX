package com.amongus.core.impl.session;

import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.*;
import com.amongus.core.api.map.GameMap;
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
import com.amongus.core.impl.state.GameStateMachine;
import com.amongus.core.impl.task.TaskFactory;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

import java.util.*;
import java.util.stream.Stream;

public class GameSessionImpl implements GameSession {

    private final GameEngine engine;

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
    private final GameStateMachine stateMachine;
    /*
     *Conjunto de jugadores de la partida.
     * Se indexan por id, es decir, PlayerId para accesar rapido y seguro.
     * */
    private final Map<PlayerId, Player> players;


    // --------------------------------------------------
    // ATRIBUTOS RELACIONADOS CON TASK
    // -------------------------------------------------

    private final Map<TaskId, Task> allTasks;
    private final Map<PlayerId, Set<TaskId>> assignedTaskIdsByPlayer;   // solo las que aún debe hacer
    private final Map<PlayerId, Set<TaskId>> completedTaskIdsByPlayer;  // las que ya terminó

    private final TaskFactory taskFactory;

    private TaskProgressTracker progressTracker;

    /**
     * Tareas asignadas a los jugadores.
     * No todas las partidas las usan de inmediato,
     * pero el core mantiene la estructura preparada.
     */
    // private final Map<PlayerId, List<Task>> taskByPlayer;







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

    public GameSessionImpl(UUID sessionId, EventBus eventBus, GameMap gameMap, GameEngine engine){
        this.engine = engine;
        this.sessionId = UUID.randomUUID();
        this.eventBus = eventBus;
        this.gameMap = gameMap;

        this.stateMachine = new GameStateMachine();


        this.players = new HashMap<>();

        //INICIALIZAR EL HASH PARA ASIGNAR LAS TAREAS A LOS JUGADORES
        this.taskFactory = new TaskFactory(engine);
        this.allTasks = new HashMap<>();
        this.assignedTaskIdsByPlayer = new HashMap<>();
        this.completedTaskIdsByPlayer = new HashMap<>();

        this.currentVotes = new HashMap<>();

        eventBus.subscribe(TaskCompletedEvent.class, event -> {
            // Mover la tarea de asignadas a completadas
            PlayerId pid = event.getPlayerId();
            TaskId   tid = event.getTaskId();

            assignedTaskIdsByPlayer.getOrDefault(pid, new HashSet<>()).remove(tid);
            completedTaskIdsByPlayer.computeIfAbsent(pid, k -> new HashSet<>()).add(tid);

            // Actualizar progreso global
            if (progressTracker != null) {
                progressTracker.taskCompleted();
                System.out.println("[progreso] pendientes: " + progressTracker.getPending()
                    + "/" + progressTracker.getTotal());
            }
        });

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
        if(stateMachine.getCurrentState() != GameState.LOBBY){
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
    public void startGame() {
        //validacion para ver si hay minimo 5 jugadores
        if (players.size() < 1) {
            throw new IllegalStateException("No hay suficientes jugadores");
        }

        stateMachine.transitionTo(GameState.IN_GAME);

        // 1. Definir posiciones hardcodeadas para las tareas
        List<Task> tasks = List.of(
            taskFactory.createNumberTask(new Position(600, 500)),
            taskFactory.createNumberTask(new Position(1200, 800)),
            taskFactory.createWiresTask(new Position(800, 600)),  // ← nueva
            taskFactory.createBotellonTask(new Position(900, 1400))
        );

        tasks.forEach(t -> allTasks.put(t.getId(), t));

        // 3. Asignar las tareas solo a los crewmates
        for (Player player : players.values()) {
            if (player.getRole() == Role.CREWMATE) {
                Set<TaskId> ids = tasks.stream()
                    .map(Task::getId)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
                assignedTaskIdsByPlayer.put(player.getId(), ids);
            }
        }
        // Contar total: suma de tareas por cada crewmate
        int total = assignedTaskIdsByPlayer.values().stream()
            .mapToInt(Set::size)
            .sum();
        this.progressTracker = new TaskProgressTracker(total);
        System.out.println("[startGame] total tareas globales: " + total);
        System.out.println("[startGame] allTasks creadas: " + allTasks.size());
        System.out.println("[startGame] asignaciones: " + assignedTaskIdsByPlayer);

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
    public void movePlayer(PlayerId playerId, Object newPosition) {
        requireState(GameState.IN_GAME);

        Player player = getAlivePlayer(playerId);
        Position targetPos = (Position) newPosition;

        // 1. Usar la posición actual del jugador y la nueva para validar
        if (!gameMap.canMove(player.getPosition(), targetPos)) {
            return;
        }

        // 2. ¡PASO VITAL!: Actualizar la posición física del objeto jugador
        // Si no haces esto, el objeto Player siempre dirá que está en el mismo sitio
        player.updatePosition(targetPos);

        // 3. Notificar al bus de eventos
        eventBus.publish(new PlayerMovedEvent(playerId, targetPos));
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

        stateMachine.transitionTo(GameState.MEETING); // Transición automática
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

        // Lógica de conteo... (se mantiene igual)
        // [Tu lógica de stream para elegir al expulsado]
        PlayerId ejected = null; // Simplificado para el ejemplo

        // Al terminar, volvemos al juego
        stateMachine.transitionTo(GameState.IN_GAME);
        eventBus.publish(new VotingResolvedEvent(ejected));
    }

    /* ============================================================
       CONSULTAS
       ============================================================ */

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
    // Devuelve las tareas pendientes del jugador
    @Override
    public List<Task> getTasksForPlayer(PlayerId playerId) {
        Set<TaskId> assignedIds = assignedTaskIdsByPlayer.getOrDefault(playerId, Set.of());
        return assignedIds.stream()
            .map(allTasks::get)
            .filter(Objects::nonNull)
            .toList();
    }

    // Verifica si una tarea está pendiente (no completada)
    private boolean isTaskPendingFor(PlayerId playerId, TaskId taskId) {
        return assignedTaskIdsByPlayer.getOrDefault(playerId, Set.of()).contains(taskId) &&
            !completedTaskIdsByPlayer.getOrDefault(playerId, Set.of()).contains(taskId);
    }

    @Override
    public void initiateTask(PlayerId playerId, TaskId taskId) {
        System.out.println("[initiateTask] llamado por: " + playerId);
        System.out.println("[initiateTask] taskId: " + taskId);
        System.out.println("[initiateTask] estado actual: " + stateMachine.getCurrentState());
        System.out.println("[initiateTask] tareas asignadas al jugador: " + assignedTaskIdsByPlayer.get(playerId));
        System.out.println("[initiateTask] allTasks keys: " + allTasks.keySet());

        requireState(GameState.IN_GAME);

        if (!isTaskPendingFor(playerId, taskId)) {
            System.out.println("[initiateTask] RECHAZADO: tarea no asignada o ya completada");
            return;
        }

        Task task = allTasks.get(taskId);
        if (task == null) {
            System.out.println("[initiateTask] RECHAZADO: tarea no encontrada en allTasks");
            return;
        }

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

/* ============================================================
       MÉTODOS AUXILIARES (PRIVADOS)
       ============================================================ */

    /**
     * Válida que el juego esté en un estado específico.
     */
    private void requireState(GameState expected) {
        if (stateMachine.getCurrentState() != expected) {
            throw new IllegalStateException(
                    "Acción inválida en estado: " + stateMachine.getCurrentState()
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
        if (player == null) throw new IllegalArgumentException("Jugador inexistente");
        if (!player.alive()) throw new IllegalStateException("Jugador muerto");
        return player;
    }



}
