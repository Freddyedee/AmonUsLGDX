package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.voting.VoteImpl;

public class LocalActionSender implements ActionSender {

    private final GameEngine engine;

    public LocalActionSender(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void send(GameAction action) {
        switch (action.getType()) {

            case MOVE   -> engine.movePlayer(action.getPlayerId(), ((MoveAction) action).getDestination());

            case KILL   -> engine.requestKill(action.getPlayerId(), ((KillAction) action).getVictimId());

            case REPORT -> engine.reportBody(action.getPlayerId(), ((ReportAction) action).getCorpseId());

            case VOTE   -> engine.castVote(new VoteImpl(action.getPlayerId(), ((VoteAction) action).getTargetId()));
        }
    }
}
