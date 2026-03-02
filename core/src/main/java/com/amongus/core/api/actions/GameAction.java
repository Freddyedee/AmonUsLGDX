package com.amongus.core.api.actions;

import com.amongus.core.api.player.PlayerId;

public interface GameAction {
    PlayerId getPlayerId();
    ActionType getType();
}
