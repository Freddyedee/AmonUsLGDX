package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;

public class KillAction implements GameAction {
    private final PlayerId playerId;
    private final PlayerId victimId;

    public KillAction(PlayerId playerId, PlayerId victimId) {
        this.playerId = playerId;
        this.victimId = victimId;
    }

    @Override public PlayerId   getPlayerId() { return playerId; }
    @Override public ActionType getType()     { return ActionType.KILL; }
    public    PlayerId          getVictimId() { return victimId; }
}
