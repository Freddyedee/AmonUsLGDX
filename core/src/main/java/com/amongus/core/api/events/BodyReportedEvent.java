package com.amongus.core.api.events;

import com.amongus.core.api.player.PlayerId;

public record BodyReportedEvent(PlayerId reporter, PlayerId victim) implements GameEvent {
}
