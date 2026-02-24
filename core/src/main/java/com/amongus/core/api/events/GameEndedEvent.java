package com.amongus.core.api.events;

public record GameEndedEvent(String reason) implements GameEvent { //puede ser un enum la razón.

}
