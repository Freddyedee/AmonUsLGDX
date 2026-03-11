package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.sabotage.SabotageManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Minijuego: Arreglar Luces (Sabotaje).
 *
 * Mecánica:
 *  - 5 palancas dispuestas horizontalmente
 *  - Cada palanca puede estar arriba (ON) o abajo (OFF)
 *  - Al inicio todas están en posición aleatoria
 *  - Click en una palanca → cambia de estado
 *  - Cuando las 5 están ON → sabotaje resuelto
 *
 * Visión reducida:
 *  - La mecánica de visión reducida se gestiona via LightSabotageVisionEffect
 *  - GameScreen debe consultar sabotageManager.isLightsSabotaged() y aplicar el efecto
 *  - El esqueleto está listo en SabotageManager, sin implementación visual todavía
 *
 * Sprites:
 *  - boardbg       → fondo del panel
 *  - lightSocket_off → ranura de la palanca (fondo)
 *  - stickUp       → palanca hacia arriba (ON)
 *  - stickDown     → palanca hacia abajo (OFF)
 *  - lightOn       → luz verde debajo de la palanca (solo si ON)
 */
public class FixLightsSabotageMinigameScreen extends AbstractMinigameScreen {

    private TextureAtlas  atlas;
    private TextureRegion boardBg;
    private TextureRegion socketRegion;
    private TextureRegion stickUpRegion;
    private TextureRegion stickDownRegion;
    private TextureRegion lightOnRegion;

    private static final int NUM_LEVERS = 5;

    // Estado de cada palanca: true = ON (arriba), false = OFF (abajo)
    private final boolean[] leverOn = new boolean[NUM_LEVERS];

    // Hitboxes de cada palanca en pantalla
    private final Rectangle[] leverRects = new Rectangle[NUM_LEVERS];

    // ── Layout ────────────────────────────────────────────────
    private float boardX, boardY, boardW, boardH;

    // Tamaño de cada slot (palanca + luz), forzado igual para todos
    private static final float SLOT_W = 60f;
    private static final float SLOT_H = 60f;
    private static final float LIGHT_H = 60f;
    private static final float GAP     = 1f;  // espacio entre palancas

    private final SabotageManager sabotageManager;

    // ── Input: evitar toggle en el mismo frame ─────────────────
    private boolean wasPressed = false;

    public FixLightsSabotageMinigameScreen(GameEngine engine, PlayerId playerId,
                                           Task task, SabotageManager sabotageManager) {
        super(engine, playerId, task);
        this.sabotageManager = sabotageManager;
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/fixLights/fixLights.atlas"));

        boardBg        = atlas.findRegion("boardbg");
        socketRegion   = atlas.findRegion("lightSocket_off");
        stickUpRegion  = atlas.findRegion("stickUp");
        stickDownRegion = atlas.findRegion("stickDown");
        lightOnRegion  = atlas.findRegion("lightOn");

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Panel centrado — proporciones del boardbg (480x520)
        boardH = sh * 0.55f;
        boardW = boardH * (480f / 520f);
        boardX = sw / 2f - boardW / 2f;
        boardY = sh / 2f - boardH / 2f;

        // Distribuir las 5 palancas horizontalmente dentro del panel
        float totalSlotsW = NUM_LEVERS * SLOT_W + (NUM_LEVERS - 1) * GAP;
        float startX      = boardX + boardW / 2f - totalSlotsW / 2f;
        float slotY       = boardY + boardH / 10000f - (SLOT_H + LIGHT_H) / 10000f;

        for (int i = 0; i < NUM_LEVERS; i++) {
            float sx = startX + i * (SLOT_W + GAP);
            leverRects[i] = new Rectangle(sx, slotY, SLOT_W, SLOT_H + LIGHT_H);
            // Estado inicial aleatorio
            leverOn[i] = Math.random() > 0.5;
        }
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        handleInput(mouseX, mouseY);

        // ── Panel de fondo ─────────────────────────────────────
        batch.draw(boardBg, boardX, boardY, boardW, boardH);

        // ── Palancas y luces ───────────────────────────────────
        for (int i = 0; i < NUM_LEVERS; i++) {
            Rectangle r  = leverRects[i];
            float     sx = r.x;

            // Ranura (socket) — fondo siempre visible
            TextureRegion stick = leverOn[i] ? stickUpRegion : stickDownRegion;
            batch.draw(stick, sx, r.y + LIGHT_H, SLOT_W, SLOT_H);

            // Socket como fondo de la luz (siempre visible debajo)
            batch.draw(socketRegion, sx, r.y, SLOT_W, LIGHT_H);

            // Luz encima del socket — solo si está ON
            if (leverOn[i]) {
                batch.draw(lightOnRegion, sx, r.y, SLOT_W, LIGHT_H);
            }
        }

        // ── Comprobar victoria ─────────────────────────────────
        if (allLeversOn() && !isCompleted) {
            complete();
        }
    }

    private void handleInput(float mouseX, float mouseY) {
        boolean pressedNow = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (pressedNow && !wasPressed) {
            for (int i = 0; i < NUM_LEVERS; i++) {
                if (leverRects[i].contains(mouseX, mouseY)) {
                    leverOn[i] = !leverOn[i];
                    System.out.println("[FixLights] Palanca " + i + " → " + (leverOn[i] ? "ON" : "OFF"));
                    break;
                }
            }
        }
        wasPressed = pressedNow;
    }

    private boolean allLeversOn() {
        for (boolean on : leverOn) if (!on) return false;
        return true;
    }

    @Override
    public void dispose() {
        atlas.dispose();
        super.dispose();
    }
}
