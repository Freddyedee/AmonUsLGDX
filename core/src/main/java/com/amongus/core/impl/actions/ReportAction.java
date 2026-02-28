package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;

public class ReportAction implements GameAction {
    private final PlayerId playerId;
    private final PlayerId corpseId;

    public ReportAction(PlayerId playerId, PlayerId corpseId) {
        this.playerId = playerId;
        this.corpseId = corpseId;
    }

    @Override public PlayerId   getPlayerId() { return playerId; }
    @Override public ActionType getType()     { return ActionType.REPORT; }
    public    PlayerId          getCorpseId() { return corpseId; }
}
