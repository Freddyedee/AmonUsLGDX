package com.amongus.core.impl.player;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.actions.KillAction;
import com.amongus.core.impl.actions.MoveAction;
import com.amongus.core.impl.actions.ReportAction;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.HudRenderer;
import com.amongus.core.view.PlayerView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

import java.util.HashSet;
import java.util.Set;

/**
 * Procesa el input del jugador local y lo convierte en acciones de dominio.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Movimiento con WASD</li>
 *   <li>Kill con tecla Q o click en botón HUD</li>
 *   <li>Reporte de cadáveres con tecla F o click en botón HUD</li>
 *   <li>Detección de cadáveres cercanos para mostrar el botón report</li>
 * </ul>
 *
 * <p>La votación se maneja exclusivamente por clicks en {@code VotingRenderer}.
 * {@link #handleMeetingInput} existe por simetría de la API pero no procesa nada.
 *
 * <p>Las teclas son configurables vía setters para una futura pantalla de opciones.
 */
public class InputHandler {

    // ── Dependencias ──────────────────────────────────────────────────
    private final ActionSender actionSender;
    private final GameEngine engine;
    private final PlayerId localPlayerId;

    // ── Constantes de juego ───────────────────────────────────────────
    private static final float KILL_RANGE = 150f;
    private static final float KILL_COOLDOWN = 15.0f;
    private static final float REPORT_RANGE = 120f;

    // ── Teclas configurables ──────────────────────────────────────────
    private int keyKill = Input.Keys.Q;
    private int keyReport = Input.Keys.F;
    private int keySkip = Input.Keys.S;
    private int keyVoteConfirm = Input.Keys.ENTER;

    // ── Estado de movimiento ──────────────────────────────────────────
    private int direccion = 1; // 1 = derecha, -1 = izquierda

    // ── Estado de kill ────────────────────────────────────────────────
    private float killCooldown = 0;
    private boolean killHappenedThisFrame = false;
    private Position killPosition = null;

    // ── Efectos visuales ──────────────────────────────────────────────
    private float bloodOverlay = 0;
    private float shakeTimer = 0;

    // ── Cadáveres reportados — no pueden volver a iniciar reunión ─────
    private final Set<PlayerId> reportedCorpses = new HashSet<>();

    // ══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════

    public InputHandler(ActionSender actionSender, GameEngine engine,
                        PlayerId localPlayerId) {
        this.actionSender = actionSender;
        this.engine = engine;
        this.localPlayerId = localPlayerId;
    }

    // ══════════════════════════════════════════════════════════════════
    //  ENTRADA PRINCIPAL POR ESTADO
    // ══════════════════════════════════════════════════════════════════

    /**
     * Procesa todo el input durante el estado IN_GAME.
     * Debe llamarse una vez por frame.
     *
     * @param snapshot estado actual del juego
     * @param delta    segundos transcurridos desde el frame anterior
     */
    public void handleGameInput(GameSnapshot snapshot, float delta) {
        handleMovement();
        updateTimers(delta);
        handleKillInput(engine.getSnapshot());
        handleReportInput(engine.getSnapshot());
    }

    /**
     * Punto de entrada durante MEETING.
     * La votación se procesa en {@code VotingRenderer} + {@code GameScreen}.
     */
    public void handleMeetingInput(GameSnapshot snapshot) {
        // Vacío intencionalmente — ver VotingRenderer.handleClick()
    }

    // ══════════════════════════════════════════════════════════════════
    //  MOVIMIENTO
    // ══════════════════════════════════════════════════════════════════

    /**
     * Lee WASD y envía {@link MoveAction} al engine.
     * No procesa input si el jugador local está muerto.
     */
    private void handleMovement() {
        PlayerView me = findLocalPlayer(engine.getSnapshot());
        if (me == null || !me.isAlive()) return;

        int speed = 3;
        float dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= speed;
            direccion = -1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += speed;
            direccion = 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= speed;
        }

        boolean moving = dx != 0 || dy != 0;
        engine.setPlayerMoving(localPlayerId, moving, direccion);

        if (moving) {
            me = findLocalPlayer(engine.getSnapshot());
            if (me != null) {
                actionSender.send(new MoveAction(localPlayerId, new Position(
                    (int) (me.getPosition().x() + dx),
                    (int) (me.getPosition().y() + dy))));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  KILL
    // ══════════════════════════════════════════════════════════════════

    /**
     * Procesa la tecla de kill (Q por defecto).
     */
    private void handleKillInput(GameSnapshot snapshot) {
        if (!Gdx.input.isKeyJustPressed(keyKill) || killCooldown > 0) return;
        executeKill(snapshot);
    }

    /**
     * Ejecuta un kill sobre el jugador vivo más cercano dentro del rango.
     * Puede invocarse desde tecla o desde click en el botón HUD.
     *
     * @param snapshot estado actual del juego
     */
    public void executeKill(GameSnapshot snapshot) {
        if (killCooldown > 0) return;

        PlayerView me = findLocalPlayer(snapshot);
        if (me == null || !me.isAlive()) return;

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.getId().equals(localPlayerId) || !pv.isAlive()) continue;

            float dist = Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                pv.getPosition().x(), pv.getPosition().y());

            if (dist < KILL_RANGE) {
                actionSender.send(new KillAction(localPlayerId, pv.getId()));
                bloodOverlay = 0.6f;
                shakeTimer = 0.2f;
                killCooldown = KILL_COOLDOWN;
                killPosition = new Position(
                    (int) pv.getPosition().x(), (int) pv.getPosition().y());
                killHappenedThisFrame = true;
                break;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  REPORTE DE CADÁVERES
    // ══════════════════════════════════════════════════════════════════

    /**
     * Detecta si hay un cadáver cercano no reportado y, si se presiona F, lo reporta.
     *
     * @param snapshot estado actual del juego
     * @return el cadáver cercano no reportado, o {@code null} si no hay ninguno
     */
    public PlayerView handleReportInput(GameSnapshot snapshot) {
        if (snapshot.getState() != GameState.IN_GAME) return null;
        PlayerView nearbyCorpse = detectNearbyCorpse(snapshot);
        if (nearbyCorpse != null && Gdx.input.isKeyJustPressed(keyReport)) {
            executeReport(snapshot);
        }
        return nearbyCorpse;
    }

    /**
     * Ejecuta el reporte del cadáver más cercano.
     * Un cadáver ya reportado no puede volver a iniciar una reunión.
     * Puede invocarse desde tecla o desde click en el botón HUD.
     *
     * @param snapshot estado actual del juego
     */
    public void executeReport(GameSnapshot snapshot) {
        if (snapshot.getState() != GameState.IN_GAME) return;
        PlayerView nearbyCorpse = detectNearbyCorpse(snapshot);
        if (nearbyCorpse == null) return;
        if (reportedCorpses.contains(nearbyCorpse.getId())) return;

        reportedCorpses.add(nearbyCorpse.getId());
        actionSender.send(new ReportAction(localPlayerId, nearbyCorpse.getId()));
    }

    /**
     * Marca un cadáver como ya reportado sin iniciar reunión.
     * Usado para jugadores expulsados por votación — no deben poder reportarse.
     *
     * @param id ID del jugador expulsado
     */
    public void markCorpseReported(PlayerId id) {
        reportedCorpses.add(id);
    }

    /**
     * Busca el primer cadáver no reportado dentro del rango de reporte.
     *
     * @param snapshot estado actual del juego
     * @return {@link PlayerView} del cadáver, o {@code null} si no hay ninguno
     */
    private PlayerView detectNearbyCorpse(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return null;

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive() || pv.getId().equals(localPlayerId)) continue;
            if (reportedCorpses.contains(pv.getId())) continue;

            float dist = Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                pv.getPosition().x(), pv.getPosition().y());
            if (dist < REPORT_RANGE) return pv;
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════
    //  CLICKS EN BOTONES HUD
    // ══════════════════════════════════════════════════════════════════

    /**
     * Procesa los clicks detectados por {@link HudRenderer} este frame.
     * Patrón idéntico al de {@code GameScreen.handleVotingClicks()}.
     *
     * @param hud      renderer del HUD que expone los flags de click
     * @param snapshot estado actual del juego
     */
    public void handleHudClick(HudRenderer hud, GameSnapshot snapshot) {
        if (hud.isKillClicked()) executeKill(snapshot);
        if (hud.isReportClicked()) executeReport(snapshot);
    }

    // ══════════════════════════════════════════════════════════════════
    //  TIMERS
    // ══════════════════════════════════════════════════════════════════

    private void updateTimers(float delta) {
        if (killCooldown > 0) killCooldown -= delta;
        if (bloodOverlay > 0) bloodOverlay -= delta;
        if (shakeTimer > 0) shakeTimer -= delta;
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ══════════════════════════════════════════════════════════════════

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(localPlayerId))
            .findFirst().orElse(null);
    }

    // ══════════════════════════════════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════════════════════════════════

    /**
     * @return dirección actual del jugador (1 = derecha, -1 = izquierda)
     */
    public int getDireccion() {
        return direccion;
    }

    /**
     * @return intensidad del overlay de sangre (0 = invisible)
     */
    public float getBloodOverlay() {
        return bloodOverlay;
    }

    /**
     * @return segundos restantes del cooldown de kill (0 = listo)
     */
    public float getKillCooldown() {
        return killCooldown;
    }

    /**
     * @return true si el cooldown de kill expiró
     */
    public boolean isKillReady() {
        return killCooldown <= 0;
    }

    /**
     * @return posición del último cadáver creado (para la animación de kill)
     */
    public Position getKillPosition() {
        return killPosition;
    }

    /**
     * @return true si ocurrió un kill este frame.
     * Se resetea llamando a {@link #resetKillFlag()}.
     */
    public boolean didKillThisFrame() {
        return killHappenedThisFrame;
    }

    /**
     * Resetea el flag de kill — llamar después de procesar la animación.
     */
    public void resetKillFlag() {
        killHappenedThisFrame = false;
    }

    // ── Setters de teclas — para futura pantalla de opciones ─────────

    /**
     * @param key tecla de kill (por defecto: {@link Input.Keys#Q})
     */
    public void setKeyKill(int key) {
        this.keyKill = key;
    }

    /**
     * @param key tecla de reporte (por defecto: {@link Input.Keys#F})
     */
    public void setKeyReport(int key) {
        this.keyReport = key;
    }

    /**
     * @param key tecla de skip vote (por defecto: {@link Input.Keys#S})
     */
    public void setKeySkip(int key) {
        this.keySkip = key;
    }

    /**
     * @param key tecla de confirmar voto (por defecto: {@link Input.Keys#ENTER})
     */
    public void setKeyVoteConfirm(int key) {
        this.keyVoteConfirm = key;
    }
}
