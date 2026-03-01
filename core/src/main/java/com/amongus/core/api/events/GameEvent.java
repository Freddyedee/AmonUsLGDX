package com.amongus.core.api.events;

public sealed interface GameEvent
        permits
        BodyReportedEvent,
        GameEndedEvent,
        GameStartedEvent,
        KillAttemptedEvent,
        PlayerJoinedEvent,
        PlayerMovedEvent,
        VoteCastEvent,
        VotingResolvedEvent,
        VotingStartedEvent,
        TaskCompletedEvent,
        TaskInteractionCancelledEvent,
        TaskInteractionStartedEvent {
}
