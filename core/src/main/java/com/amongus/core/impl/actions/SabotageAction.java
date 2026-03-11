package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.impl.sabotage.SabotageManager.SabotageType;

public class SabotageAction implements GameAction {
    private final PlayerId playerId;
    private final SabotageType type;

    public SabotageAction(PlayerId playerId, SabotageType type) {
        this.playerId = playerId;
        this.type = type;
    }

    @Override public ActionType getType() { return ActionType.SABOTAGE; } // ¡Añade SABOTAGE al enum!
    @Override public PlayerId getPlayerId() { return playerId; }
    public SabotageType getSabotageType() { return type; }
}
