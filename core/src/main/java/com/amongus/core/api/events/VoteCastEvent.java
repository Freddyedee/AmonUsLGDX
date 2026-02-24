package com.amongus.core.api.events;

import com.amongus.core.api.player.PlayerId;

public record VoteCastEvent(PlayerId voter, PlayerId target) implements GameEvent { //if voted is null -> abstention.
}
