package com.amongus.core.api.events;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.TaskId;

public record TaskInteractionStartedEvent(PlayerId playerId, TaskId taskId) implements GameEvent {
}
