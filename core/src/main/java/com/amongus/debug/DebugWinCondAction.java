package com.amongus.debug;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;

public class DebugWinCondAction implements GameAction {
    private final PlayerId playerId;
    private final boolean ignoreWins;

    public DebugWinCondAction(PlayerId playerId, boolean ignoreWins) {
        this.playerId = playerId;
        this.ignoreWins = ignoreWins;
    }

    @Override
    public ActionType getType() { return ActionType.DEBUG_WIN_COND; }

    @Override
    public PlayerId getPlayerId() { return playerId; }

    public boolean isIgnoreWins() { return ignoreWins; }
}
