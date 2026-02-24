package com.amongus.core.api.events;
import com.amongus.core.api.player.PlayerId;

public record KillAttemptedEvent(PlayerId killer, PlayerId victim) implements GameEvent{


}
