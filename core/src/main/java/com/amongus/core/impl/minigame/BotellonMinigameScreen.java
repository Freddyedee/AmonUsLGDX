package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Minijuego: Rellenar el Botellón.
 *
 * Mecánica:
 *  - El jugador mantiene presionado el botón (SPACE o clic)
 *  - Mientras lo mantiene, el agua sube dentro del botellón
 *  - Si suelta antes de llegar al tope, el agua baja lentamente
 *  - Al llegar al 100% la tarea se completa
 *
 * Sprites usados del atlas:
 *  - panel_water_bg       → fondo interior del botellón
 *  - Panel_water_outline  → contorno del botellón (encima del agua)
 *  - water_buttonUp       → botón sin presionar
 *  - water_buttonDown     → botón presionado
 *  - waterfill            → sprite del agua (se recorta verticalmente)
 */
public class BotellonMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas y regiones ──────────────────────────────────────
    private TextureAtlas atlas;
    private TextureRegion bottleOutline;
    private TextureRegion bottleBg;
    private TextureRegion buttonUp;
    private TextureRegion buttonDown;
    private TextureRegion waterFill;

    // ── Estado del minijuego ──────────────────────────────────
    private float fillProgress  = 0f;   // 0.0 → 1.0
    private static final float FILL_SPEED   = 0.25f;  // progreso por segundo al presionar
    private static final float DRAIN_SPEED  = 0.10f;  // progreso por segundo al soltar

    // ── Layout (calculado en show) ────────────────────────────
    private float bottleX, bottleY, bottleW, bottleH;
    private float buttonX, buttonY, buttonW, buttonH;

    public BotellonMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();

        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/botellon/minijuegoBotellon.atlas"));

        bottleOutline = atlas.findRegion("Panel_water_outline");
        bottleBg      = atlas.findRegion("panel_water_bg");
        buttonUp      = atlas.findRegion("water_buttonUp");
        buttonDown    = atlas.findRegion("water_buttonDown");
        waterFill     = atlas.findRegion("waterfill");

        // ── Layout centrado en pantalla ───────────────────────
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Botellón: escalar manteniendo proporción del outline (291x401)
        bottleH = sh * 0.40f;
        bottleW = bottleH * (291f / 401f);
        bottleX = sw / 2f - bottleW / 2f - 60f;   // un poco a la izquierda del centro
        bottleY = sh / 2f - bottleH / 2f;

        // Botón: a la derecha del botellón
        buttonW = sw * 0.10f;
        buttonH = buttonW * (137f / 205f);
        buttonX = bottleX + bottleW + 40f;
        buttonY = sh / 2f - buttonH / 2f;
    }

    @Override
    protected void renderContent(float delta) {

        boolean pressing = Gdx.input.isKeyPressed(Input.Keys.SPACE)
            || Gdx.input.isTouched();

        // ── Actualizar progreso ───────────────────────────────
        if (pressing) {
            fillProgress = Math.min(1f, fillProgress + FILL_SPEED * delta);
        } else {
            fillProgress = Math.max(0f, fillProgress - DRAIN_SPEED * delta);
        }

        // ── Dibujar fondo del botellón ────────────────────────
        batch.draw(bottleBg, bottleX, bottleY, bottleW, bottleH);

        // ── Dibujar agua (recorte vertical desde abajo) ───────
        drawWater();

        // ── Dibujar contorno encima del agua ─────────────────
        batch.draw(bottleOutline, bottleX, bottleY, bottleW, bottleH);

        // ── Dibujar botón ─────────────────────────────────────
        TextureRegion btn = pressing ? buttonDown : buttonUp;
        batch.draw(btn, buttonX, buttonY, buttonW, buttonH);

        // ── Instrucción ───────────────────────────────────────
        // (texto simple; puedes reemplazar por BitmapFont con estilo)

        // ── Comprobar victoria ────────────────────────────────
        if (fillProgress >= 1f) {
            complete();
        }
    }

    /**
     * Dibuja el sprite de agua recortado verticalmente desde abajo
     * según el progreso actual, creando la ilusión de llenado.
     */
    private void drawWater() {
        // Área interior del botellón donde cabe el agua
        // Dejamos un margen para que encaje dentro del contorno
        float marginX = bottleW * 0.12f;
        float marginBottom = bottleH * 0.05f;
        float marginTop    = bottleH * 0.08f;

        float areaX = bottleX + marginX;
        float areaW = bottleW - marginX * 2f;
        float areaYBase = bottleY + marginBottom;
        float areaMaxH  = bottleH - marginBottom - marginTop;

        float currentH = areaMaxH * fillProgress;
        if (currentH <= 0) return;

        // Recortamos el sprite desde abajo: srcY empieza desde abajo del sprite
        int srcFullH = waterFill.getRegionHeight();
        int srcH     = (int)(srcFullH * fillProgress);
        int srcX     = waterFill.getRegionX();
        int srcY     = waterFill.getRegionY() + (srcFullH - srcH); // desde abajo

        batch.draw(
            waterFill.getTexture(),
            areaX,
            areaYBase,
            areaW,
            currentH,
            srcX, srcY,
            waterFill.getRegionWidth(), srcH,
            false, false
        );
    }

    @Override
    public void dispose() {
        atlas.dispose();
        super.dispose();
    }
}
