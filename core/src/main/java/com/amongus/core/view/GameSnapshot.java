package com.amongus.core.view;

import com.amongus.core.api.state.GameState;
import com.amongus.core.api.player.PlayerId; // Añadimos esto
import com.amongus.core.api.task.Task;

import java.util.List;

public final class GameSnapshot {

    private final GameState state;
    private final List<PlayerView> players;
    private final PlayerId localPlayerId; // Para saber a quién sigue la cámara
    private final List<TaskView> tasks;
    private final int totalTasks;
    private final int completedTasks;

    public GameSnapshot(GameState state, List<PlayerView> players, PlayerId localPlayerId, List<TaskView> tasks, int totalTasks, int completedTasks) {
        this.state = state;
        this.players = List.copyOf(players);
        this.localPlayerId = localPlayerId;
        this.tasks = List.copyOf(tasks);
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
    }

    public GameState getState() {
        return state;
    }

    public List<PlayerView> getPlayers() {
        return players;
    }

    public PlayerId getLocalPlayerId() {
        return localPlayerId;
    }

    // NUEVO: devuelve las tareas (solo las asignadas al jugador local)
    public List<TaskView> getTasks() {
        return tasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }
}
