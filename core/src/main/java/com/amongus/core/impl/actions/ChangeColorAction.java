package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.SkinColor;

public class ChangeColorAction implements GameAction {
    private final PlayerId playerId;
    private final SkinColor newColor;

    public ChangeColorAction(PlayerId playerId, SkinColor newColor) {
        this.playerId = playerId;
        this.newColor = newColor;
    }

    @Override public PlayerId getPlayerId() { return playerId; }
    @Override public ActionType getType() { return ActionType.CHANGE_COLOR; }
    public SkinColor getNewColor() { return newColor; }
}
