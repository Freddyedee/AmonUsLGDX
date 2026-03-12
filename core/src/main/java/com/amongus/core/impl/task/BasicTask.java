package com.amongus.core.impl.task;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.model.Position;

public class BasicTask implements Task {

    private static final float INTERACTION_RADIUS = 50f;

    //Atributos de Task
    private final TaskId taskId;
    private final String taskName;
    private final TaskType taskType;
    private final Position taskPosition;
    private final MinigameProvider minigameProvider;

    public BasicTask(TaskId id,
                     Position location,
                     TaskType taskType,
                     String enterAscendingCode,
                     MinigameProvider provider) {
        this.taskId = id;
        this.taskType = taskType;
        this.taskName = enterAscendingCode;
        this.taskPosition = location;
        this.minigameProvider = provider;
    }

    @Override
    public TaskId getId() {
        return taskId;
    }

    @Override
    public String getName() {
        return taskName;
    }

    @Override
    public Position getPosition() {
        return taskPosition;
    }

    @Override
    public TaskType getTaskType() {
        return taskType;
    }

    // Método común para interacción básica (distancia, etc.)
    @Override
    public boolean canInteract(PlayerId playerId, Position playerPos) {
        // lógica de distancia euclidiana
        float dx = playerPos.x() - taskPosition.x();
        float dy = playerPos.y() - taskPosition.y();
        return Math.sqrt(dx*dx + dy*dy)<= INTERACTION_RADIUS;
            //BUSCAR ESTO --------------------------------------------------------------->   <= INTERACTION_RADIUS;
    }

    //WIP
    @Override
    public void interact(PlayerId playerId) {

    }

    @Override
    public MinigameProvider getMinigameProvider() {
        return minigameProvider;
    }
}
