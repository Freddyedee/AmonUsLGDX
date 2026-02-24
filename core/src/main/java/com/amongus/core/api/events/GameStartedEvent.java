package com.amongus.core.api.events;

import java.util.UUID;

public record GameStartedEvent(UUID sessionId) implements GameEvent {

}
