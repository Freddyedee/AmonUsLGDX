package com.amongus.core.api.events;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.model.Position;

public record PlayerMovedEvent(PlayerId playerId, Position to) implements GameEvent {

}
