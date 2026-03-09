package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.task.unique.GasolineTaskGroup;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Parte 2: Vaciar el bidón en la estación de recarga.
 * Mantén SPACE para vaciar el bidón y llenar la estación.
 * Al llegar al 100% la tarea completa se termina.
 */
public class GasolinePart2MinigameScreen extends AbstractMinigameScreen {

    private TextureAtlas atlas;
    private TextureRegion fillStationBase;  // medidor de la estación
    private TextureRegion fillBaseFillAnimation;
    private TextureRegion button;
    private TextureRegion buttonBase;

    private float fillProgress = 0f;
    private static final float FILL_SPEED  = 0.35f;
    private static final float DRAIN_SPEED = 0.06f;

    private float stationX, stationY, stationW, stationH;
    private float btnX, btnY, btnW, btnH;

    public GasolinePart2MinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/gasoline/gasoline.atlas"));

        fillStationBase = atlas.findRegion("engineFuel_fillBase");
        fillBaseFillAnimation = atlas.findRegion("fillAnimationFinishGas");
        button          = atlas.findRegion("engineFuel_Button");
        buttonBase      = atlas.findRegion("engineFuel_buttonBase");

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Medidor de estación centrado
        stationH = sh * 0.65f;
        stationW = stationH * (341f / 500f);
        stationX = sw / 2f - stationW / 2f - 80f;
        stationY = sh / 2f - stationH / 2f;

        // Botón a la derecha
        btnW = 100f;
        btnH = 100f;
        btnX = stationX + stationW + 60f;
        btnY = sh / 2f - btnH / 2f;
    }

    @Override
    protected void renderContent(float delta) {
        boolean pressing = Gdx.input.isKeyPressed(Input.Keys.SPACE)
            || Gdx.input.isTouched();

        if (pressing) {
            fillProgress = Math.min(1f, fillProgress + FILL_SPEED * delta);
        } else {
            fillProgress = Math.max(0f, fillProgress - DRAIN_SPEED * delta);
        }

        // Fondo del medidor (vacío)
        batch.draw(fillStationBase, stationX, stationY, stationW, stationH);

        // Fill de la estación (tinte rojo-naranja para diferenciar de parte 1)
        drawStationFill();

        // Botón
        batch.draw(buttonBase, btnX - 10, btnY - 10, btnW + 20, btnH + 20);
        batch.draw(button,
            pressing ? btnX + 4 : btnX,
            pressing ? btnY - 4 : btnY,
            btnW, btnH);

        if (fillProgress >= 1f && !isCompleted) {
            complete();
        }
    }

    private void drawStationFill() {
        float marginX      = stationW * 0.15f;
        float marginBottom = stationH * 0.10f;
        float marginTop    = stationH * 0.10f;

        float areaX     = stationX + marginX;
        float areaW     = stationW - marginX * 2.3f;
        float areaYBase = stationY + marginBottom;
        float areaMaxH  = stationH - marginBottom - marginTop;

        float currentH = areaMaxH * fillProgress;
        if (currentH <= 0) return;

        int srcFullH = fillBaseFillAnimation.getRegionHeight();
        int srcH     = (int)(srcFullH * fillProgress);
        int srcX     = fillBaseFillAnimation.getRegionX();
        int srcY     = fillBaseFillAnimation.getRegionY() + (srcFullH - srcH);

        // Tinte naranja/rojo para la estación
        batch.setColor(1f, 0.45f, 0f, 1f);
        batch.draw(
            fillBaseFillAnimation.getTexture(),
            areaX, areaYBase,
            areaW, currentH,
            srcX, srcY,
            fillBaseFillAnimation.getRegionWidth(), srcH,
            false, false
        );
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void dispose() {
        atlas.dispose();
        super.dispose();
    }
}
