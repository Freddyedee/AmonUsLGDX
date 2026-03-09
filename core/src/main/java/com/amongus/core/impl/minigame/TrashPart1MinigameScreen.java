package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.task.unique.TrashTaskGroup;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Parte 1: Arrastrar los 4 objetos de basura (botella, bola, manzana, caja)
 * hasta el saco de basura (trashBagInto).
 */
public class TrashPart1MinigameScreen extends AbstractMinigameScreen {

    private TextureAtlas atlas;

    // Sprites de los items de basura
    private static final String[] ITEM_NAMES = {
        "bottleTrash-removebg-preview",
        "ballTrash-removebg-preview",
        "appleTrash-removebg-preview",
        "boxTrash-removebg-preview"
    };
    private static final int NUM_ITEMS = 4;

    private TextureRegion[] itemRegions  = new TextureRegion[NUM_ITEMS];
    private Rectangle[]     itemRects    = new Rectangle[NUM_ITEMS];
    private boolean[]       itemDropped  = new boolean[NUM_ITEMS]; // ya en el saco

    // Saco de basura (destino)
    private TextureRegion bagRegion;
    private Rectangle     bagRect;

    // Drag state
    private int     draggingIdx = -1;
    private float   dragOffX, dragOffY; // offset desde el centro del item al click

    // Tamaño base de los items
    private static final float ITEM_H    = 90f;
    private static final float BAG_H     = 200f;
    private static final float DROP_DIST = 80f; // distancia para considerar "dentro del saco"

    private final TrashTaskGroup group;

    public TrashPart1MinigameScreen(GameEngine engine, PlayerId playerId,
                                    Task task, TrashTaskGroup group) {
        super(engine, playerId, task);
        this.group = group;
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/trash/trash.atlas"));

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Saco de basura: centrado en parte inferior
        bagRegion = atlas.findRegion("trashBagInto-removebg-preview");
        float bagW = BAG_H * ((float) bagRegion.getRegionWidth() / bagRegion.getRegionHeight());
        bagRect = new Rectangle(sw / 2f - bagW / 2f, sh * 0.20f, bagW, BAG_H);

        // Items dispersos en la pantalla (posiciones fijas distribuidas)
        float[][] positions = {
            {sw * 0.40f, sh * 0.55f},  // botella - izquierda
            {sw * 0.35f, sh * 0.65f},  // bola - centro izquierda
            {sw * 0.60f, sh * 0.60f},  // manzana - centro derecha
            {sw * 0.64f, sh * 0.40f},  // caja - derecha
        };

        for (int i = 0; i < NUM_ITEMS; i++) {
            itemRegions[i] = atlas.findRegion(ITEM_NAMES[i]);
            float iW = ITEM_H * ((float) itemRegions[i].getRegionWidth()
                / itemRegions[i].getRegionHeight());
            itemRects[i]   = new Rectangle(positions[i][0] - iW / 2f,
                positions[i][1] - ITEM_H / 2f, iW, ITEM_H);
            itemDropped[i] = false;
        }
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        handleInput(mouseX, mouseY);

        // ── Dibujar saco ──────────────────────────────────────
        batch.draw(bagRegion, bagRect.x, bagRect.y, bagRect.width, bagRect.height);

        // ── Dibujar items (no arrastrados y no droppeados) ────
        for (int i = 0; i < NUM_ITEMS; i++) {
            if (itemDropped[i] || draggingIdx == i) continue;
            Rectangle r = itemRects[i];
            batch.draw(itemRegions[i], r.x, r.y, r.width, r.height);
        }

        // ── Dibujar item arrastrado encima de todo ────────────
        if (draggingIdx != -1) {
            Rectangle r = itemRects[draggingIdx];
            batch.draw(itemRegions[draggingIdx],
                mouseX - r.width / 2f + dragOffX,
                mouseY - r.height / 2f + dragOffY,
                r.width, r.height);
        }

        // ── Instrucción ───────────────────────────────────────
        // (puedes añadir BitmapFont aquí si quieres texto)
    }

    private void handleInput(float mouseX, float mouseY) {
        // Iniciar drag
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && draggingIdx == -1) {
            for (int i = 0; i < NUM_ITEMS; i++) {
                if (itemDropped[i]) continue;
                Rectangle r = itemRects[i];
                if (r.contains(mouseX, mouseY)) {
                    draggingIdx = i;
                    dragOffX = 0;
                    dragOffY = 0;
                    break;
                }
            }
        }

        // Soltar
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && draggingIdx != -1) {
            // Comprobar si cayó sobre el saco
            float bagCX = bagRect.x + bagRect.width / 2f;
            float bagCY = bagRect.y + bagRect.height / 2f;
            float dist  = dst(mouseX, mouseY, bagCX, bagCY);

            if (dist <= DROP_DIST + bagRect.width / 2f) {
                itemDropped[draggingIdx] = true;
                System.out.println("[Trash P1] Item " + draggingIdx + " en el saco.");
                checkAllDropped();
            } else {
                // Volver a posición original (no hacemos snap, simplemente queda donde estaba)
            }
            draggingIdx = -1;
        }
    }

    private void checkAllDropped() {
        for (boolean d : itemDropped) if (!d) return;
        System.out.println("[Trash P1] Todos los items en el saco. Parte 2 desbloqueada.");
        group.completePart1();
        complete();
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
