package com.amongus.core.impl.minigame;

import com.amongus.core.api.events.TaskCompletedEvent;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.task.unique.GasolineTaskGroup;
import com.amongus.core.impl.task.unique.GasolineTaskPart1;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Parte 1: Llenar el bidón de gasolina.
 * Mantén SPACE presionado para llenar el medidor.
 * Al llegar al 100%, se completa la parte 1 y se desbloquea la parte 2.
 */
public class GasolinePart1MinigameScreen extends AbstractMinigameScreen {

    private TextureAtlas atlas;
    private TextureRegion gasCanBase;   // contorno del bidón
    private TextureRegion gasCanBaseFillAnimation;
    private TextureRegion button;
    private TextureRegion buttonBase;

    private float fillProgress = 0f;
    private static final float FILL_SPEED  = 0.26f;
    private static final float DRAIN_SPEED = 0.08f;

    private float canX, canY, canW, canH;
    private float btnX, btnY, btnW, btnH;

    private final GasolineTaskGroup group;

    public GasolinePart1MinigameScreen(GameEngine engine, PlayerId playerId,
                                       Task task, GasolineTaskGroup group) {
        super(engine, playerId, task);
        this.group = group;
    }


    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/gasoline/gasoline.atlas"));

        gasCanBase = atlas.findRegion("engineFuel_gasCanBase");
        gasCanBaseFillAnimation = atlas.findRegion("fillAnimationGasCanNoBg");// usamos como fill del bidón

        button     = atlas.findRegion("engineFuel_Button");
        buttonBase = atlas.findRegion("engineFuel_buttonBase");

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Bidón centrado a la izquierda del centro
        canH = sh * 0.65f;
        canW = canH * (341f / 500f);
        canX = sw / 2f - canW / 2f - 80f;
        canY = sh / 2f - canH / 2f;

        // Botón a la derecha
        btnW = 100f;
        btnH = 100f;
        btnX = canX + canW + 60f;
        btnY = sh / 2f - btnH / 2f;
    }

    @Override
    protected void renderContent(float delta) {
        boolean pressing = Gdx.input.isKeyPressed(Input.Keys.SPACE)
            || Gdx.input.isTouched();

        // Actualizar progreso
        if (pressing) {
            fillProgress = Math.min(1f, fillProgress + FILL_SPEED * delta);
        } else {
            fillProgress = Math.max(0f, fillProgress - DRAIN_SPEED * delta);
        }

        // Dibujar bidón vacío (contorno)
        batch.draw(gasCanBase, canX, canY, canW, canH);

        // Dibujar fill recortado verticalmente desde abajo
        drawFill();

        // Dibujar botón
        batch.draw(buttonBase, btnX - 10, btnY - 10, btnW + 20, btnH + 20);
        batch.draw(button,
            pressing ? btnX + 4 : btnX,
            pressing ? btnY - 4 : btnY,
            btnW, btnH);

        // Comprobar victoria
        if (fillProgress >= 1f && !isCompleted) {
            group.completePart1();
            complete();
        }
    }

    private void drawFill() {
        // Área interior del bidón donde cabe el fill
        float marginX      = canW * 0.01f;
        float marginBottom = canH * 0.04f;
        float marginTop    = canH * 0.001f;

        float areaX    = canX + marginX;
        float areaW    = canW - marginX * 2.3f;
        float areaYBase = canY + marginBottom;
        float areaMaxH  = canH - marginBottom - marginTop;

        float currentH = areaMaxH * fillProgress;
        if (currentH <= 0) return;

        int srcFullH = gasCanBaseFillAnimation.getRegionHeight();
        int srcH     = (int)(srcFullH * fillProgress);
        int srcX     = gasCanBaseFillAnimation.getRegionX();
        int srcY     = gasCanBaseFillAnimation.getRegionY() + (srcFullH - srcH);

        // Tinte amarillo para el fill
        batch.setColor(1f, 0.85f, 0f, 1f);
        batch.draw(
            gasCanBaseFillAnimation.getTexture(),
            areaX, areaYBase,
            areaW, currentH,
            srcX, srcY,
            gasCanBaseFillAnimation.getRegionWidth(), srcH,
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
