package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.impl.voting.VoteImpl;

public class NetworkActionSender implements ActionSender {
    private final GameEngine engine;
    private final GameClient client;

    public NetworkActionSender(GameEngine engine, GameClient client) {
        this.engine = engine;
        this.client = client;
    }

    @Override
    public void send(GameAction action) {
        // 1. Ejecución Local (Cero lag para tu pantalla)
        switch (action.getType()) {
            case MOVE   -> engine.movePlayer(action.getPlayerId(), ((MoveAction) action).getDestination());
            case KILL   -> engine.requestKill(action.getPlayerId(), ((KillAction) action).getVictimId());
            case REPORT -> engine.reportBody(action.getPlayerId(), ((ReportAction) action).getCorpseId());
            case VOTE   -> engine.castVote(new VoteImpl(action.getPlayerId(), ((VoteAction) action).getTargetId()));
        }

        // 2. Envío por Red
        if (client != null) {
            switch (action.getType()) {
                case MOVE -> {
                    MoveAction ma = (MoveAction) action;
                    String msg = "MOVE:" + ma.getPlayerId().value() + ":" + ma.getDestination().x() + ":" + ma.getDestination().y();
                    client.enviarMensaje(msg);
                }
                case KILL -> {
                    KillAction ka = (KillAction) action;
                    String msg = "KILL:" + ka.getPlayerId().value() + ":" + ka.getVictimId().value();
                    client.enviarMensaje(msg);
                }
                case REPORT -> {
                    ReportAction ra = (ReportAction) action;
                    String msg = "REPORT:" + ra.getPlayerId().value() + ":" + ra.getCorpseId().value();
                    client.enviarMensaje(msg);
                }
                case VOTE -> {
                    VoteAction va = (VoteAction) action;
                    String target = va.isSkip() ? "SKIP" : va.getTargetId().value().toString();
                    String msg = "VOTE:" + va.getPlayerId().value() + ":" + target;
                    client.enviarMensaje(msg);
                }
            }
        }
    }

    public void sendStop(PlayerId id, int direction) {
        if (client != null) {
            client.enviarMensaje("STOP:" + id.value() + ":" + direction);
        }
    }
}
