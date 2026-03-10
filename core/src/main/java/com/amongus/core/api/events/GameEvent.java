package com.amongus.core.api.events;

public sealed interface GameEvent
    permits BodyReportedEvent, GameEndedEvent, GameStartedEvent, KillAttemptedEvent, PlayerJoinedEvent, PlayerMovedEvent, TaskCompletedEvent, TaskInteractionCancelledEvent, TaskInteractionStartedEvent, VoteCastEvent, VotingResolvedEvent, VotingStartedEvent {
}
