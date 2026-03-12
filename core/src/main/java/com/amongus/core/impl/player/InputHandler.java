package com.amongus.core.impl.player;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.actions.*;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.HudRenderer;
import com.amongus.core.view.PlayerView;
import com.amongus.core.api.player.Role;
import com.amongus.core.view.VotingRenderer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InputHandler {
    private final ActionSender actionSender;
    private final GameEngine engine;
    private final PlayerId localPlayerId;

    private static final float KILL_RANGE = 100f;
    private static final float KILL_COOLDOWN = 25.0f;
    private static final float REPORT_RANGE = 100f;

    // Posiciones de los botones de emergencia
    private static final Position EMERGENCY_MAP1 = new Position(339f, 1240f);
    private static final Position EMERGENCY_MAP2 = new Position(2331f, 1275f);
    private static final float EMERGENCY_RADIUS = 100f;
    private PlayerId selectedVoteTarget = null;

    private float emergencyCooldown = 30f;
    private float freezeTimer = 0f;
    private GameState previousState = GameState.LOBBY;

    private int keyKill = Input.Keys.Q;
    private int keyReport = Input.Keys.R;
    private int keySkip = Input.Keys.S;
    private int keyVent = Input.Keys.V;
    private final java.util.Set<PlayerId> staleCorpses = new java.util.HashSet<>();

    private int direccion = 1;
    private float killCooldown = 0;
    private PlayerId reporterId = null;
    private PlayerId reportedCorpseId = null;

    private PlayerId spectatedPlayerId = null;
    private boolean canVent = false;

    public InputHandler(ActionSender actionSender, GameEngine engine, PlayerId localPlayerId) {
        this.actionSender = actionSender;
        this.engine = engine;
        this.localPlayerId = localPlayerId;
    }

    private Position getEmergencyButtonPos() {
        return engine.getMapType() == MapType.MAPA_1 ? EMERGENCY_MAP1 : EMERGENCY_MAP2;
    }

    public float getEmergencyCooldown() {
        return Math.max(0, emergencyCooldown);
    }

    public boolean isNearEmergencyTable(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return false;
        Position emPos = getEmergencyButtonPos();
        return Vector2.dst(me.getPosition().x(), me.getPosition().y(), emPos.x(), emPos.y()) <= EMERGENCY_RADIUS;
    }

    public void handleGameInput(GameSnapshot snapshot, float delta) {
        // Detectar el inicio de la partida o fin de la reunión
        if (snapshot.getState() == GameState.IN_GAME && previousState != GameState.IN_GAME) {
            emergencyCooldown = 30f;
            // Diferenciamos de dónde venimos
            if (previousState == GameState.LOBBY) {
                freezeTimer = 5f; // 5 segundos para la intro gigante
            } else {
                freezeTimer = 0f;
            }

            for (PlayerView pv : snapshot.getPlayers()) {
                if (!pv.isAlive()) {
                    staleCorpses.add(pv.getId());
                }
            }
        }
        previousState = snapshot.getState();

        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return;

        if (!me.isAlive()) {
            handleSpectatorInput(snapshot);
            return;
        }

        spectatedPlayerId = localPlayerId;

        // ── LÓGICA DE CAMBIO DE COLOR EN EL LOBBY  ──
        if (snapshot.getState() == GameState.LOBBY) {
            staleCorpses.clear();
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
            Position nearest = engine.getNearestVent(me.getPosition(), 60f);
            canVent = me.isVenting() || nearest != null;

            if (me.isVenting()) {
                if (Gdx.input.isKeyJustPressed(keyVent)) {
                    executeVent(snapshot);
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                    Position next = engine.getNextVent(me.getPosition(), 1);
                    if (next != null) actionSender.send(new VentAction(localPlayerId, next, false));
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                    Position prev = engine.getNextVent(me.getPosition(), -1);
                    if (prev != null) actionSender.send(new VentAction(localPlayerId, prev, false));
                }

                updateTimers(delta);
                return;
            } else {
                if (Gdx.input.isKeyJustPressed(keyVent)) {
                    executeVent(snapshot);
                    return;
                }
            }
        }

        // --- LÓGICA GENERAL ---
        handleMovement(delta);
        updateTimers(delta);
        handleKillInput(snapshot);
        handleReportInput(snapshot);

        // --- INTERACCIÓN CON TAREAS/EMERGENCIA (Teclado) ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            executeInteraction(snapshot);
        }
    }

    public void handleMeetingInput(GameSnapshot snapshot, float meetingTimer, VotingRenderer votingRenderer) {
        previousState = snapshot.getState();
        for (PlayerView pv : snapshot.getPlayers()) {
            if (!pv.isAlive()) {
                staleCorpses.add(pv.getId());
            }
        }
        handleVoteInput(snapshot, meetingTimer, votingRenderer);
    }

    private void handleMovement(float delta) {
        if (freezeTimer > 0) {
            // Le decimos al motor que detenga la animación por si se quedó pegada
            engine.setPlayerMoving(localPlayerId, false, direccion);
            return;
        }
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
        if (!me.isAlive() || me.getRole() != Role.IMPOSTOR) return;

        if (me.isVenting()) {
            actionSender.send(new VentAction(localPlayerId, null, true));
        } else {
            Position nearest = engine.getNearestVent(me.getPosition(), 60f);
            if (nearest != null) {
                actionSender.send(new VentAction(localPlayerId, nearest, false));
            }
        }
    }

    public void handleHudClick(HudRenderer hud, GameSnapshot snapshot) {
        if (hud.isKillClicked()) executeKill(snapshot);
        if (hud.isReportClicked()) executeReport(snapshot);
        if (hud.isVentClicked()) executeVent(snapshot);
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

    public void handleVoteInput(GameSnapshot snapshot, float meetingTimer, VotingRenderer votingRenderer) {
        if (meetingTimer < 15f) return;
        if (engine.getVotedPlayers().contains(localPlayerId)) return;

        // 1. Mapear el clic del ratón a la resolución 4K interna de la tablet
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Convertimos las coordenadas de la ventana (ej. 1024x768) a las del mundo virtual de la UI (3840x2160)
            float screenX = Gdx.input.getX();
            float screenY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Invertir Y

            float scaleX = 3840f / Gdx.graphics.getWidth();
            float scaleY = 2160f / Gdx.graphics.getHeight();

            float worldX = screenX * scaleX;
            float worldY = screenY * scaleY;

            // 2. Comprobar si hicimos clic en los botones Confirmar/Rechazar (si hay alguien seleccionado)
            if (selectedVoteTarget != null) {
                if (votingRenderer.btnConfirmarHitbox != null && votingRenderer.btnConfirmarHitbox.contains(worldX, worldY)) {
                    // Si el ID es el "falso", es que le dimos a Skip
                    if (selectedVoteTarget.value().toString().equals("00000000-0000-0000-0000-000000000000")) {
                        actionSender.send(new VoteAction(localPlayerId, null));
                    } else {
                        actionSender.send(new VoteAction(localPlayerId, selectedVoteTarget));
                    }
                    selectedVoteTarget = null; // Limpiar tras votar
                    return;
                }

                if (votingRenderer.btnRechazarHitbox != null && votingRenderer.btnRechazarHitbox.contains(worldX, worldY)) {
                    selectedVoteTarget = null; // Cancelar selección
                    return;
                }
            }

            // 3. Comprobar si hicimos clic en el botón de SKIP VOTE
            if (votingRenderer.skipHitbox != null && votingRenderer.skipHitbox.contains(worldX, worldY)) {
                // Usamos un UUID de ceros como "bandera" para identificar el Skip
                selectedVoteTarget = new PlayerId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"));
                return;
            }

            // 4. Comprobar si hicimos clic en el recuadro de algún jugador
            for (Map.Entry<PlayerId, Rectangle> entry : votingRenderer.playerHitboxes.entrySet()) {
                if (entry.getValue().contains(worldX, worldY)) {
                    // Si hacemos clic en el mismo, lo deseleccionamos. Si es otro, cambiamos la selección.
                    if (selectedVoteTarget != null && selectedVoteTarget.equals(entry.getKey())) {
                        selectedVoteTarget = null;
                    } else {
                        selectedVoteTarget = entry.getKey();
                    }
                    return;
                }
            }
        }

        // Mantener también el soporte por teclado por si acaso
        List<com.amongus.core.view.PlayerView> votable = snapshot.getPlayers().stream()
            .filter(com.amongus.core.view.PlayerView::isAlive).toList();

        for (int i = 0; i < votable.size(); i++) {
            if (!Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) continue;
            PlayerId targetId = votable.get(i).getId();
            actionSender.send(new VoteAction(localPlayerId, targetId));
        }

        if (Gdx.input.isKeyJustPressed(keySkip)) {
            actionSender.send(new VoteAction(localPlayerId, null));
        }
    }

    // 👇 Añade este método para que la vista pueda saber quién está seleccionado
    public PlayerId getSelectedVoteTarget() {
        return selectedVoteTarget;
    }

    public void executeInteraction(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return;

        // Verificamos si hay un sabotaje activo
        boolean isSabotageActive = snapshot.getTasks().stream()
            .anyMatch(tv -> tv.getTaskType() == TaskType.SABOTAGE && !tv.isCompleted());

        // --- 1. PRIORIDAD: BOTÓN DE EMERGENCIA ---
        if (isNearEmergencyTable(snapshot) && emergencyCooldown <= 0 && !isSabotageActive) {
            reporterId = localPlayerId;
            reportedCorpseId = null;
            PlayerId emergencyId = new PlayerId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"));
            actionSender.send(new ReportAction(localPlayerId, emergencyId));
            return;
        }

        // --- 2. SECUNDARIO: TAREAS ---
        snapshot.getTasks().stream()
            .filter(tv -> Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y()) <= 150f)
            .min(Comparator.comparingDouble(tv -> Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y())))
            .ifPresent(tv -> engine.initiateTask(tv.getId()));
    }

    private void updateTimers(float delta) {
        if (killCooldown > 0) killCooldown -= delta;
        if (emergencyCooldown > 0) emergencyCooldown -= delta;
        if (freezeTimer > 0) freezeTimer -= delta;
    }

    public float getFreezeTimer() {
        return Math.max(0, freezeTimer);
    }

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream().filter(p -> p.getId().equals(localPlayerId)).findFirst().orElse(null);
    }

    // Metodo que evalúa si hay una tarea o emergencia cerca para encender el botón
    public boolean canUse(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return false;

        boolean nearTask = snapshot.getTasks().stream()
            .anyMatch(tv -> Vector2.dst(me.getPosition().x(), me.getPosition().y(), tv.getPosition().x(), tv.getPosition().y()) <= 80f);

        boolean nearEmergency = isNearEmergencyTable(snapshot);

        // Verificamos si hay algún sabotaje activo en la partida
        boolean isSabotageActive = snapshot.getTasks().stream()
            .anyMatch(tv -> tv.getTaskType() == TaskType.SABOTAGE && !tv.isCompleted());

        // Si hay cooldown O hay un sabotaje, el botón NO se ilumina por la mesa
        if (nearEmergency && (emergencyCooldown > 0 || isSabotageActive)) {
            nearEmergency = false;
        }

        // Si hay cooldown, el botón de "usar" NO se ilumina por la mesa
        if (nearEmergency && emergencyCooldown > 0) {
            nearEmergency = false;
        }

        return nearTask || nearEmergency;
    }

    public int getDireccion() { return direccion; }
    public PlayerId getReporterId() { return reporterId; }
    public PlayerId getReportedCorpseId() { return reportedCorpseId; }
    public float getKillCooldown() { return killCooldown; }
    public boolean isKillReady() { return killCooldown <= 0; }
    public boolean canVent() { return canVent; }
}
