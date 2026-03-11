package com.amongus.core.api.session;

import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.sabotage.SabotageManager;

import java.util.Collection;
import java.util.List;

/**
 * Contrato que representa una sesión de juego (una partida).
 *
 * Una GameSession define el conjunto de operaciones válidas
 * que pueden realizarse durante el ciclo de vida del juego.
 *
 * Esta interfaz:
 *  - NO contiene lógica
 *  - NO conoce implementaciones
 *  - define QUÉ se puede hacer, no CÓMO
 *
 * Es el punto central de comunicación entre:
 *  - UI
 *  - Application
 *  - Core
 */
public interface GameSession {

    // --------------------------------------------------
    // GESTIÓN DE JUGADORES
    // --------------------------------------------------

    /**
     * Agrega un jugador a la partida.
     * Restricción:
     *  - Solo debe ser permitido cuando la partida
     *    aún no ha comenzado (estado LOBBY).
     * @param player jugador a agregar
     */
    void addPlayer(Player player);

    /**
     * Inicia la partida.
     * Restricciones típicas:
     *  - Debe existir un número mínimo de jugadores
     *  - El estado del juego debe ser LOBBY
     */
    void startGame();

    // --------------------------------------------------
    // MOVIMIENTO
    // --------------------------------------------------

    /**
     * Solicita el movimiento de un jugador a una nueva posición.
     *
     * Restricciones:
     *  - El jugador debe existir
     *  - El jugador debe estar vivo
     *  - El estado del juego debe permitir movimiento
     *
     * @param playerId identificador del jugador
     * @param newPosition nueva posición destino (abstracción del mapa)
     */
    void movePlayer(PlayerId playerId, Object newPosition);

    // --------------------------------------------------
    // ACCIONES DE JUEGO
    // --------------------------------------------------

    /**
     * Intenta realizar un asesinato.
     *
     * Esta operación puede fallar silenciosamente
     * si no se cumplen las reglas del juego
     * (cooldown, rol incorrecto, estado inválido).
     *
     * @param killer identificador del atacante
     * @param victim identificador de la víctima
     */
    void attemptKill(PlayerId killer, PlayerId victim);

    /**
     * Reporta un cuerpo y activa una reunión.
     *
     * Normalmente provoca:
     *  - cambio de estado
     *  - inicio de votación
     *
     * @param reporter jugador que reporta
     * @param victim jugador reportado
     */
    void reportBody(PlayerId reporter, PlayerId victim);

    // --------------------------------------------------
    // VOTACIÓN
    // --------------------------------------------------

    /**
     * Registra un voto durante una reunión.
     * Restricción:
     *  - Solo válido durante estado MEETING
     *
     * @param vote voto emitido por un jugador
     */
    void castVote(Vote vote);

    /**
     * Resuelve la votación actual.
     *
     * Normalmente:
     *  - determina si un jugador es expulsado
     *  - devuelve el estado a PLAYING
     */
    void resolveVoting();
    // --------------------------------------------------
    // TAREAS
    // --------------------------------------------------


    // --------------------------------------------------
    // CONSULTAS
    // --------------------------------------------------

    /**
     * Devuelve el estado actual de la partida.
     *
     * @return estado del juego
     */
    GameState getCurrentState();

    /**
     * Devuelve todos los jugadores de la partida.
     *
     * Se recomienda devolver una colección inmodificable
     * desde la implementación.
     *
     * @return jugadores actuales
     */
    Collection<Player> getPlayers();

    // Devuelve las tareas pendientes del jugador
    List<Task> getTasksForPlayer(PlayerId playerId);

    void initiateTask(PlayerId localPlayerId, TaskId taskId);

    TaskProgressTracker getProgressTracker();

    boolean isTaskCompleted(PlayerId localPlayerId, TaskId id);

    List<Task> getAllTasksForPlayer(PlayerId localPlayerId);

    void activateSabotageTask(SabotageManager.SabotageType type);

    Player getPlayer(PlayerId localPlayerId);

    List<Task> getAllTasks();

    boolean isImpostor(PlayerId localPlayerId);
}


