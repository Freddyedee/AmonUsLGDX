package com.amongus.core.api.events;
import com.amongus.core.api.player.PlayerId;

public record PlayerJoinedEvent(PlayerId playerId) implements GameEvent {

}
