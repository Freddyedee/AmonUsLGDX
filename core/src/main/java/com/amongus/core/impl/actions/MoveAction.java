package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionType;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.model.Position;

public class MoveAction implements GameAction {

    private final PlayerId playerId;
    private final Position destination;

    public MoveAction(PlayerId playerId, Position destination) {
        this.playerId    = playerId;
        this.destination = destination;
    }

    @Override public PlayerId   getPlayerId()   { return playerId;    }
    @Override public ActionType getType()       { return ActionType.MOVE; }
    public    Position          getDestination(){ return destination;  }

}
