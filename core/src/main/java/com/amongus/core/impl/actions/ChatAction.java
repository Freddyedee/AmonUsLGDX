package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;

public class ChatAction implements GameAction {
    private final PlayerId playerId;
    private final String message;

    public ChatAction(PlayerId playerId, String message) {
        this.playerId = playerId;
        this.message = message;
    }

    @Override public PlayerId getPlayerId() { return playerId; }
    @Override public ActionType getType() { return ActionType.CHAT; }
    public String getMessage() { return message; }
}
