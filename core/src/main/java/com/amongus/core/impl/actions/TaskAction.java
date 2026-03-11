package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.TaskId;

public class TaskAction implements GameAction {
    private final PlayerId playerId;
    private final TaskId taskId;

    public TaskAction(PlayerId playerId, TaskId taskId) {
        this.playerId = playerId;
        this.taskId = taskId;
    }

    @Override public ActionType getType() { return ActionType.TASK; } // Asegúrate de añadir TASK a tu enum ActionType
    @Override public PlayerId getPlayerId() { return playerId; }
    public TaskId getTaskId() { return taskId; }
}
