package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Parte 2: Arrastrar el saco de basura (trashBag) hasta el contenedor (trashCanPart2).
 * Al soltarlo sobre el contenedor:
 *  1. El saco desaparece
 *  2. El contenedor cambia a trashCanPart2Close
 *  3. Espera 1 segundo y completa la tarea
 */
public class TrashPart2MinigameScreen extends AbstractMinigameScreen {

    private TextureAtlas atlas;

    // Saco de basura (para arrastrar)
    private TextureRegion bagRegion;
    private Rectangle     bagRect;
    private boolean       bagDragging = false;
    private boolean       bagDropped  = false;

    // Contenedor
    private TextureRegion canOpenRegion;
    private TextureRegion canCloseRegion;
    private Rectangle     canRect;
    private boolean       canClosed = false;

    // Temporizador para cerrar y completar
    private float closeTimer = 0f;
    private static final float CLOSE_DURATION = 1.0f;

    private static final float BAG_H = 180f;
    private static final float CAN_H = 280f;
    private static final float DROP_DIST = 100f;

    public TrashPart2MinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/trash/trash.atlas"));

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Saco de basura: lado izquierdo
        bagRegion  = atlas.findRegion("trashBag-removebg-preview");
        float bagW = BAG_H * ((float) bagRegion.getRegionWidth() / bagRegion.getRegionHeight());
        bagRect    = new Rectangle(sw * 0.35f - bagW / 2f, sh * 0.40f, bagW, BAG_H);

        // Contenedor: lado derecho
        canOpenRegion  = atlas.findRegion("trashCanpart2-removebg-preview");
        canCloseRegion = atlas.findRegion("trashCanpart2Close-removebg-preview");
        float canW     = CAN_H * ((float) canOpenRegion.getRegionWidth()
            / canOpenRegion.getRegionHeight());
        canRect        = new Rectangle(sw * 0.60f - canW / 2f, sh * 0.25f, canW, CAN_H);
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // ── Temporizador post-drop ────────────────────────────
        if (canClosed) {
            closeTimer += delta;
            if (closeTimer >= CLOSE_DURATION && !isCompleted) {
                complete();
            }
        }

        handleInput(mouseX, mouseY);

        // ── Dibujar contenedor ────────────────────────────────
        TextureRegion canToDraw = canClosed ? canCloseRegion : canOpenRegion;
        batch.draw(canToDraw, canRect.x, canRect.y, canRect.width, canRect.height);

        // ── Dibujar saco (si no fue droppeado) ───────────────
        if (!bagDropped) {
            float drawX = bagDragging
                ? mouseX - bagRect.width / 2f
                : bagRect.x;
            float drawY = bagDragging
                ? mouseY - bagRect.height / 2f
                : bagRect.y;
            batch.draw(bagRegion, drawX, drawY, bagRect.width, bagRect.height);
        }
    }

    private void handleInput(float mouseX, float mouseY) {
        if (bagDropped || canClosed) return;

        // Iniciar drag
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !bagDragging) {
            if (bagRect.contains(mouseX, mouseY)) {
                bagDragging = true;
            }
        }

        // Soltar
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && bagDragging) {
            bagDragging = false;

            float canCX = canRect.x + canRect.width / 2f;
            float canCY = canRect.y + canRect.height / 2f;
            float dist  = dst(mouseX, mouseY, canCX, canCY);

            if (dist <= DROP_DIST + canRect.width / 2f) {
                System.out.println("[Trash P2] Saco en el contenedor. Cerrando...");
                bagDropped = true;
                canClosed  = true;
                closeTimer = 0f;
            }
        }
    }

    private float dst(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void dispose() {
        atlas.dispose();
        super.dispose();
    }
}
