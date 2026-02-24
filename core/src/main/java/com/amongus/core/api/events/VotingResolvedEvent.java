package com.amongus.core.api.events;

import com.amongus.core.api.player.PlayerId;

public record VotingResolvedEvent(PlayerId ejectedPlayer)
        implements GameEvent {
}
