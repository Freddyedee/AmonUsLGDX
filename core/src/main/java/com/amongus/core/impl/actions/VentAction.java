package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.model.Position;

public class VentAction implements GameAction {
    private final PlayerId playerId;
    private final Position targetVent; // A qué ventilación saltamos
    private final boolean exiting;     // ¿Estamos saliendo de la ventilación?

    public VentAction(PlayerId playerId, Position targetVent, boolean exiting) {
        this.playerId = playerId;
        this.targetVent = targetVent;
        this.exiting = exiting;
    }

    @Override public PlayerId getPlayerId() { return playerId; }
    @Override public ActionType getType() { return ActionType.VENT; }
    public Position getTargetVent() { return targetVent; }
    public boolean isExiting() { return exiting; }
}
