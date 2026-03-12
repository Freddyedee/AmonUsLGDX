package com.amongus.core.impl.actions;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.actions.GameAction;
import com.amongus.core.api.events.TaskCompletedEvent;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.impl.session.GameSessionImpl;
import com.amongus.core.impl.voting.VoteImpl;
import com.amongus.core.view.PlayerView;
import com.amongus.debug.DebugConfig;
import com.amongus.debug.DebugWinCondAction;

import static com.amongus.core.api.actions.ActionType.CHANGE_COLOR;

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
            case VENT -> engine.processVentAction(action.getPlayerId(), ((VentAction)action).getTargetVent(), ((VentAction)action).isExiting());
            case CHANGE_COLOR -> engine.changePlayerColor(action.getPlayerId(), ((ChangeColorAction) action).getNewColor());
            case SABOTAGE -> {
                SabotageAction sa = (SabotageAction) action;
                engine.getSabotageManager().forceActivateSabotage(sa.getSabotageType());
                engine.activateSabotageTask(sa.getSabotageType());
            }
            case DEBUG_WIN_COND -> DebugConfig.IGNORE_WIN_CONDITIONS = ((DebugWinCondAction) action).isIgnoreWins();
        }

        // 2. Envío por Red
        if (client != null) {
            switch (action.getType()) {
                case MOVE -> {
                    MoveAction ma = (MoveAction) action;
                    // Obtenemos la dirección actual de nuestro jugador local
                    int dir = engine.getSnapshot().getPlayers().stream()
                        .filter(p -> p.getId().equals(ma.getPlayerId()))
                        .findFirst().map(PlayerView::getDirection).orElse(1);
                    client.enviarMensaje("MOVE:" + ma.getPlayerId().value() + ":" + ma.getDestination().x() + ":" + ma.getDestination().y() + ":" + dir);
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
                case VENT -> {
                    VentAction va = (VentAction) action;
                    // Formato: VENT:UUID:EXITING:X:Y
                    String msg = "VENT:" + va.getPlayerId().value() + ":" + va.isExiting() + ":"
                        + (va.getTargetVent() != null ? va.getTargetVent().x() + ":" + va.getTargetVent().y() : "0:0");
                    client.enviarMensaje(msg);
                }
                case CHANGE_COLOR -> {
                    ChangeColorAction ca = (ChangeColorAction) action;
                    client.enviarMensaje("COLOR:" + ca.getPlayerId().value() + ":" + ca.getNewColor().name());
                }
                case SABOTAGE -> {
                    SabotageAction sa = (SabotageAction) action;
                    client.enviarMensaje("SABOTAGE:" + sa.getPlayerId().value() + ":" + sa.getSabotageType().name());
                }
                case DEBUG_WIN_COND -> {
                    DebugWinCondAction dwa = (DebugWinCondAction) action;
                    client.enviarMensaje("DEBUG_WIN_COND:" + dwa.isIgnoreWins());
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
