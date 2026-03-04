package com.amongus.core.impl.player;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.impl.actions.*;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InputHandler {

    private final ActionSender actionSender;
    private final GameEngine engine;
    private final PlayerId localPlayerId;


    // ── Constantes de juego ──────────────────────────────────
    private static final float KILL_RANGE    = 150f;
    private static final float KILL_COOLDOWN = 15.0f;
    private static final float REPORT_RANGE  = 120f;

    // ── Teclas configurables ─────────────────────────────────
    private int keyKill   = Input.Keys.Q;
    private int keyReport = Input.Keys.F;
    private int keySkip   = Input.Keys.S;
    private int keyVoteConfirm = Input.Keys.ENTER;


    private int direccion = 1;
    private float killCooldown = 0;
    private PlayerId reporterId       = null;
    private PlayerId reportedCorpseId = null;
    private float bloodOverlay = 0;
    private float shakeTimer   = 0;


    public InputHandler(ActionSender actionSender, GameEngine engine, PlayerId localPlayerId) {
        this.actionSender  = actionSender;
        this.engine        = engine;
        this.localPlayerId = localPlayerId;
    }

    public void handleGameInput(GameSnapshot snapshot, float delta) {
        // ── Bloquear TODO el input de juego si hay minijuego activo ──
        if (engine.getActiveMinigame() != null) return;

        handleMovement();
        updateTimers(delta);
        handleKillInput(engine.getSnapshot());
        handleReportInput(engine.getSnapshot());
        //TaskImput
        handleTaskInput(engine.getSnapshot());
    }

    public void handleMeetingInput(GameSnapshot snapshot) {
        handleVoteInput(snapshot);
    }

    private void handleMovement() {
        int speed = 5;
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) { dx -= speed; direccion = -1; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { dx += speed; direccion =  1; }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) { dy += speed; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { dy -= speed; }

        boolean moving = dx != 0 || dy != 0;
        engine.setPlayerMoving(localPlayerId, moving, direccion);

        if (moving) {
            PlayerView me = findLocalPlayer(engine.getSnapshot());
            if (me != null) {
                Position next = new Position(
                    (int)(me.getPosition().x() + dx),
                    (int)(me.getPosition().y() + dy)
                );
                actionSender.send(new MoveAction(localPlayerId, next));
            }
        }
    }

    private void handleKillInput(GameSnapshot snapshot) {
        if (!Gdx.input.isKeyJustPressed(keyKill)) return;
        if (killCooldown > 0) return;

        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return;

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.getId().equals(localPlayerId) || !pv.isAlive()) continue;

            float dist = Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                pv.getPosition().x(), pv.getPosition().y()
            );

            if (dist < KILL_RANGE) {
                actionSender.send(new KillAction(localPlayerId, pv.getId()));
                bloodOverlay = 0.6f;
                shakeTimer   = 0.2f;
                killCooldown = KILL_COOLDOWN;
                break;
            }
        }
    }

    public PlayerView handleReportInput(GameSnapshot snapshot) {
        if (snapshot.getState() != com.amongus.core.api.state.GameState.IN_GAME) return null;
        PlayerView nearbyCorpse = detectNearbyCorpse(snapshot);

        if (nearbyCorpse != null && Gdx.input.isKeyJustPressed(keyReport)) {
            reporterId       = localPlayerId;
            reportedCorpseId = nearbyCorpse.getId();
            actionSender.send(new ReportAction(localPlayerId, nearbyCorpse.getId()));
        }

        return nearbyCorpse;
    }

    private PlayerView detectNearbyCorpse(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return null;

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive() || pv.getId().equals(localPlayerId)) continue;

            float dist = Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                pv.getPosition().x(), pv.getPosition().y()
            );
            if (dist < REPORT_RANGE) return pv;
        }
        return null;
    }

    private void handleVoteInput(GameSnapshot snapshot) {
        List<PlayerView> votable = snapshot.getPlayers().stream()
            .filter(PlayerView::isAlive)
            .collect(Collectors.toList());

        for (int i = 0; i < votable.size(); i++) {
            if (!Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) continue;

            PlayerId targetId = votable.get(i).getId();
            if (targetId.equals(localPlayerId)) {
                System.out.println("[VOTO] No puedes votarte a ti mismo");
                continue;
            }
            actionSender.send(new VoteAction(localPlayerId, targetId));
        }

        if (Gdx.input.isKeyJustPressed(keySkip)) {
            actionSender.send(new VoteAction(localPlayerId, null));
        }

        if (Gdx.input.isKeyJustPressed(keyVoteConfirm)) {
            Optional<PlayerId> expelled = engine.resolveVoting();
            expelled.ifPresent(id -> System.out.println("[RESULTADO] Expulsado: " + id));
        }
    }

    //AÑADIDO BOTON DE E PARA INTERACCION CON TASK
    private void handleTaskInput(GameSnapshot snapshot) {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return;

        snapshot.getTasks().stream()
            .filter(tv -> Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y()) <= 150f)
            .min(Comparator.comparingDouble(tv -> Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y())))
            .ifPresent(tv -> actionSender.send(new TaskAction(localPlayerId, tv.getId())));
    }

    private void updateTimers(float delta) {
        if (killCooldown > 0) killCooldown -= delta;
        if (bloodOverlay > 0) bloodOverlay -= delta;
        if (shakeTimer   > 0) shakeTimer   -= delta;
    }

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(localPlayerId))
            .findFirst().orElse(null);
    }

    public int      getDireccion()        { return direccion;    }
    public float    getBloodOverlay()     { return bloodOverlay; }
    public PlayerId getReporterId()       { return reporterId;   }
    public PlayerId getReportedCorpseId() { return reportedCorpseId; }

    //Seters para teclas

    public void setKeyKill(int key)        { this.keyKill        = key; }
    public void setKeyReport(int key)      { this.keyReport      = key; }
    public void setKeySkip(int key)        { this.keySkip        = key; }
    public void setKeyVoteConfirm(int key) { this.keyVoteConfirm = key; }
}
