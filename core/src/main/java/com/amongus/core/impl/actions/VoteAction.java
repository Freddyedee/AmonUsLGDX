package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;

public class VoteAction implements GameAction {
    private final PlayerId playerId;
    private final PlayerId targetId; // null = skip

    public VoteAction(PlayerId playerId, PlayerId targetId) {
        this.playerId = playerId;
        this.targetId = targetId;
    }

    @Override public PlayerId   getPlayerId() { return playerId; }
    @Override public ActionType getType()     { return ActionType.VOTE; }
    public    PlayerId          getTargetId() { return targetId; }
    public    boolean           isSkip()      { return targetId == null; }
}
