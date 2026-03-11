package com.amongus.core.impl.player;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.actions.*;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.HudRenderer;
import com.amongus.core.view.PlayerView;
import com.amongus.core.api.player.Role;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class InputHandler {
    private final ActionSender actionSender;
    private final GameEngine engine;
    private final PlayerId localPlayerId;

    private static final float KILL_RANGE = 150f;
    private static final float KILL_COOLDOWN = 15.0f;
    private static final float REPORT_RANGE = 120f;
    // Posiciones de los botones de emergencia
    private static final Position EMERGENCY_MAP1 = new Position(339f, 1240f);
    private static final Position EMERGENCY_MAP2 = new Position(2331f, 1275f);
    private static final float EMERGENCY_RADIUS = 150f;

    private int keyKill = Input.Keys.Q;
    private int keyReport = Input.Keys.F;
    private int keySkip = Input.Keys.S;
    private int keyVoteConfirm = Input.Keys.ENTER;
    private int keyVent = Input.Keys.V;
    private final java.util.Set<PlayerId> staleCorpses = new java.util.HashSet<>();

    private Position getEmergencyButtonPos() {
        return engine.getMapType() == MapType.MAPA_1 ? EMERGENCY_MAP1 : EMERGENCY_MAP2;
    }

    private int direccion = 1;
    private float killCooldown = 0;
    private PlayerId reporterId = null;
    private PlayerId reportedCorpseId = null;

    private PlayerId spectatedPlayerId = null;

    private boolean canVent = false; // NUEVO: Para decirle al HUD si dibujar el botón

    public InputHandler(ActionSender actionSender, GameEngine engine, PlayerId localPlayerId) {
        this.actionSender = actionSender;
        this.engine = engine;
        this.localPlayerId = localPlayerId;
    }

    public void handleGameInput(GameSnapshot snapshot, float delta) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return;

        if (!me.isAlive()) {
            handleSpectatorInput(snapshot);
            return;
        }

        spectatedPlayerId = localPlayerId;

        // ── LÓGICA DE CAMBIO DE COLOR EN EL LOBBY  ──
        if (snapshot.getState() == GameState.LOBBY) {
            staleCorpses.clear(); // Limpiamos la memoria de cuerpos al iniciar partida
            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                com.amongus.core.api.player.SkinColor[] colors = com.amongus.core.api.player.SkinColor.values();
                int currentIndex = 0;

                for (int i = 0; i < colors.length; i++) {
                    if (colors[i] == me.getSkinColor()) currentIndex = i;
                }

                int nextIndex = (currentIndex + 1) % colors.length;
                com.amongus.core.api.player.SkinColor nextColor = colors[nextIndex];

                actionSender.send(new ChangeColorAction(localPlayerId, nextColor));

                com.amongus.core.utils.SettingsManager.playerColor = nextColor.name();
                com.amongus.core.utils.SettingsManager.save();
            }
        }

        // ── LÓGICA DE VENTILACIÓN ──
        canVent = false;
        if (me.getRole() == Role.IMPOSTOR) {
            Position nearest = engine.getNearestVent(me.getPosition(), 50f);
            canVent = me.isVenting() || nearest != null;

            if (me.isVenting()) {
                if (Gdx.input.isKeyJustPressed(keyVent)) {
                    executeVent(snapshot); // Sale
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                    Position next = engine.getNextVent(me.getPosition(), 1);
                    if (next != null) actionSender.send(new VentAction(localPlayerId, next, false));
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                    Position prev = engine.getNextVent(me.getPosition(), -1);
                    if (prev != null) actionSender.send(new VentAction(localPlayerId, prev, false));
                }

                updateTimers(delta);
                return; // Cortamos el flujo para que no camine
            } else {
                if (Gdx.input.isKeyJustPressed(keyVent)) {
                    executeVent(snapshot); // Entra
                    return;
                }
            }
        }

        // --- LÓGICA GENERAL ---
        handleMovement(delta);
        updateTimers(delta);
        handleKillInput(snapshot);
        handleReportInput(snapshot);

        // --- INTERACCIÓN CON TAREAS (Teclado) ---
        // Restauramos el uso de la tecla 'E' (como el Among Us original)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            executeInteraction(snapshot);
        }
    }
    public void handleMeetingInput(GameSnapshot snapshot, float meetingTimer) {
        for (PlayerView pv : snapshot.getPlayers()) {
            if (!pv.isAlive()) {
                staleCorpses.add(pv.getId());
            }
        }
        handleVoteInput(snapshot, meetingTimer);
    }
    private void handleMovement(float delta) {
        float speed = 250f;
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) { dx -= speed * delta; direccion = -1; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { dx += speed * delta; direccion =  1; }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) { dy += speed * delta; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { dy -= speed * delta; }

        boolean moving = dx != 0 || dy != 0;
        engine.setPlayerMoving(localPlayerId, moving, direccion);

        if (moving) {
            PlayerView me = findLocalPlayer(engine.getSnapshot());
            if (me != null) {
                Position next = new Position((int)(me.getPosition().x() + dx), (int)(me.getPosition().y() + dy));
                actionSender.send(new MoveAction(localPlayerId, next));
            }
        } else {
            if (actionSender instanceof NetworkActionSender) {
                ((NetworkActionSender) actionSender).sendStop(localPlayerId, direccion);
            }
        }
    }

    // ── LÓGICA MODO ESPECTADOR ──
    private void handleSpectatorInput(GameSnapshot snapshot) {
        if (spectatedPlayerId == null) {
            spectatedPlayerId = localPlayerId;
        }

        List<PlayerView> players = snapshot.getPlayers();
        if (players.isEmpty()) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            cycleSpectator(players, 1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            cycleSpectator(players, -1);
        }
    }

    private void cycleSpectator(List<PlayerView> players, int directionStep) {
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(spectatedPlayerId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) currentIndex = 0;

        int nextIndex = (currentIndex + directionStep) % players.size();
        if (nextIndex < 0) nextIndex += players.size();

        spectatedPlayerId = players.get(nextIndex).getId();
    }

    public PlayerId getSpectatedPlayerId() {
        return spectatedPlayerId != null ? spectatedPlayerId : localPlayerId;
    }

    private void handleKillInput(GameSnapshot snapshot) {
        if (!Gdx.input.isKeyJustPressed(keyKill)) return;
        if (killCooldown > 0) return;
        executeKill(snapshot);
    }

    public void executeKill(GameSnapshot snapshot) {
        if (killCooldown > 0) return;
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return;

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.getId().equals(localPlayerId) || !pv.isAlive()) continue;

            float dist = Vector2.dst(me.getPosition().x(), me.getPosition().y(), pv.getPosition().x(), pv.getPosition().y());
            if (dist < KILL_RANGE) {
                actionSender.send(new KillAction(localPlayerId, pv.getId()));
                killCooldown = KILL_COOLDOWN;
                break;
            }
        }
    }

    public void executeReport(GameSnapshot snapshot) {
        if (snapshot.getState() != GameState.IN_GAME) return;
        PlayerView nearbyCorpse = detectNearbyCorpse(snapshot);
        if (nearbyCorpse != null) {
            reporterId = localPlayerId;
            reportedCorpseId = nearbyCorpse.getId();
            actionSender.send(new ReportAction(localPlayerId, nearbyCorpse.getId()));
        }
    }

    public void executeVent(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive() || me.getRole() != Role.IMPOSTOR) return;

        if (me.isVenting()) {
            actionSender.send(new VentAction(localPlayerId, null, true)); // SALIR
        } else {
            Position nearest = engine.getNearestVent(me.getPosition(), 50f);
            if (nearest != null) {
                actionSender.send(new VentAction(localPlayerId, nearest, false)); // ENTRAR
            }
        }
    }

    public void handleHudClick(HudRenderer hud, GameSnapshot snapshot) {
        if (hud.isKillClicked()) executeKill(snapshot);
        if (hud.isReportClicked()) executeReport(snapshot);
        if (hud.isVentClicked()) executeVent(snapshot);
        if (hud.isConfigurationClicked());
    }

    public PlayerView handleReportInput(GameSnapshot snapshot) {
        if (snapshot.getState() != GameState.IN_GAME) return null;
        PlayerView nearbyCorpse = detectNearbyCorpse(snapshot);
        if (nearbyCorpse != null && Gdx.input.isKeyJustPressed(keyReport)) {
            executeReport(snapshot);
        }
        return nearbyCorpse;
    }

    private PlayerView detectNearbyCorpse(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return null;
        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive() || pv.getId().equals(localPlayerId) || staleCorpses.contains(pv.getId())) continue;
            float dist = Vector2.dst(me.getPosition().x(), me.getPosition().y(), pv.getPosition().x(), pv.getPosition().y());
            if (dist < REPORT_RANGE) return pv;
        }
        return null;
    }

    private void handleVoteInput(GameSnapshot snapshot, float meetingTimer) {
        if (meetingTimer < 15f) return;
        if (engine.getVotedPlayers().contains(localPlayerId)) return;

        List<PlayerView> votable = snapshot.getPlayers().stream()
            .filter(PlayerView::isAlive).collect(Collectors.toList());

        for (int i = 0; i < votable.size(); i++) {
            if (!Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) continue;
            PlayerId targetId = votable.get(i).getId();
            actionSender.send(new VoteAction(localPlayerId, targetId));
        }

        if (Gdx.input.isKeyJustPressed(keySkip)) {
            actionSender.send(new VoteAction(localPlayerId, null));
        }
    }

    // Cambiamos el nombre y añadimos la lógica de prioridad
    public void executeInteraction(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return;

        // --- 1. PRIORIDAD: BOTÓN DE EMERGENCIA ---
        Position emPos = getEmergencyButtonPos();
        if (Vector2.dst(me.getPosition().x(), me.getPosition().y(), emPos.x(), emPos.y()) <= EMERGENCY_RADIUS) {
            reporterId = localPlayerId;
            reportedCorpseId = null;
            // Usamos el ReportAction con el "código secreto" para que viaje por red sin crear paquetes nuevos
            PlayerId emergencyId = new PlayerId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"));
            actionSender.send(new ReportAction(localPlayerId, emergencyId));
            return; // Cortamos para que no abra tareas
        }

        // --- 2. SECUNDARIO: TAREAS ---
        snapshot.getTasks().stream()
            .filter(tv -> Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y()) <= 50f)
            .min(Comparator.comparingDouble(tv -> Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y())))
            .ifPresent(tv -> engine.initiateTask(tv.getId()));
    }

    private void updateTimers(float delta) {
        if (killCooldown > 0) killCooldown -= delta;
    }

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream().filter(p -> p.getId().equals(localPlayerId)).findFirst().orElse(null);
    }

    // Metodo que evalúa si hay una tarea cerca
    public boolean canUse(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return false;

        // 1. Verifica si está cerca de una tarea
        boolean nearTask = snapshot.getTasks().stream()
            .anyMatch(tv -> Vector2.dst(me.getPosition().x(), me.getPosition().y(), tv.getPosition().x(), tv.getPosition().y()) <= 50f);

        // 2. Verifica si está cerca de la mesa de emergencia
        Position emPos = getEmergencyButtonPos();
        boolean nearEmergency = Vector2.dst(me.getPosition().x(), me.getPosition().y(), emPos.x(), emPos.y()) <= EMERGENCY_RADIUS;

        return nearTask || nearEmergency;
    }

    public int getDireccion() { return direccion; }
    public PlayerId getReporterId() { return reporterId; }
    public PlayerId getReportedCorpseId() { return reportedCorpseId; }
    public float getKillCooldown() { return killCooldown; }
    public boolean isKillReady() { return killCooldown <= 0; }
    public boolean canVent() { return canVent; }
}
