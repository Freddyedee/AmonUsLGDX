package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Minijuego: Biblioteca.
 *
 * Mecánica:
 *  - 5 libros fuera de la estantería
 *  - 5 slots vacíos en la estantería
 *  - Drag & drop de cada libro a un slot
 *  - Cada slot acepta solo 1 libro
 *  - Al colocar los 5, se completa la tarea
 */
public class LibraryMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas ─────────────────────────────────────────────────
    private TextureAtlas atlas;
    private TextureRegion shelfRegion;

    private static final String[] BOOK_NAMES = {
        "book1-removebg-preview",
        "book2-removebg-preview",
        "book3-removebg-preview",
        "book4-removebg-preview",
        "book5-removebg-preview"
    };
    private static final int NUM_BOOKS = 5;
    private TextureRegion[] bookRegions = new TextureRegion[NUM_BOOKS];

    // ── Layout estantería ─────────────────────────────────────
    private float shelfX, shelfY, shelfW, shelfH;

    // ── Slots vacíos (posiciones relativas al sprite de la estantería) ──
    // Cada par {relX, relY, relW, relH} define el slot como fracción del sprite
    // Ajustar según posición real de los huecos grises en bookShelf
    private static final float[][] SLOT_REL = {
        // fila superior: dos huecos
        {0.410f, 0.700f, 0.185f, 0.230f},   // slot 0 – superior izquierdo
        {0.729f, 0.700f, 0.185f, 0.230f},   // slot 1 – superior derecho
        // fila media: un hueco
        {0.410f, 0.370f, 0.185f, 0.230f},   // slot 2 – medio
        // fila inferior: dos huecos
        {0.410f, 0.050f, 0.185f, 0.230f},   // slot 3 – inferior izquierdo
        {0.729f, 0.050f, 0.185f, 0.230f},   // slot 4 – inferior derecho
    };
    private Rectangle[] slotRects  = new Rectangle[NUM_BOOKS];
    private int[]        slotFilled = new int[NUM_BOOKS]; // -1 = vacío, else = bookIdx

    // ── Libros ────────────────────────────────────────────────
    // Tamaño visual forzado igual para todos (los libros son verticales)
    private static final float BOOK_W = 27f;
    private static final float BOOK_H = 65f;

    private Rectangle[] bookRects   = new Rectangle[NUM_BOOKS];
    private boolean[]   bookPlaced  = new boolean[NUM_BOOKS]; // ya en un slot

    // ── Drag state ────────────────────────────────────────────
    private int draggingIdx = -1;
    private float dragOffX, dragOffY;

    // ── ShapeRenderer para highlight de slots ─────────────────
    private ShapeRenderer shapeRenderer;

    // ── Panel exterior para los libros ────────────────────────
    private float bookAreaX, bookAreaY, bookAreaW, bookAreaH;

    public LibraryMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/library/library.atlas"));

        shelfRegion = atlas.findRegion("bookShelf");
        for (int i = 0; i < NUM_BOOKS; i++) {
            bookRegions[i] = atlas.findRegion(BOOK_NAMES[i]);
        }

        shapeRenderer = new ShapeRenderer();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Estantería: ocupa la parte derecha/central de la pantalla
        // Sprite es 784x1168 → ratio 0.671
        shelfH = sh * 0.53f;
        shelfW = shelfH * (784f / 1164f);
        shelfX = sw / 2f - shelfW / 2f + sw * 0.09f; // ligeramente a la derecha
        shelfY = sh / 2.2f - shelfH / 2.2f;

        // Calcular rectángulos de slots en pantalla
        for (int i = 0; i < NUM_BOOKS; i++) {
            float[] r  = SLOT_REL[i];
            float   sx = shelfX + shelfW * r[0];
            float   sy = shelfY + shelfH * r[1];
            float   sw2 = shelfW * r[2];
            float   sh2 = shelfH * r[3];
            slotRects[i]  = new Rectangle(sx, sy, sw2, sh2);
            slotFilled[i] = -1;
        }

        // Área de libros: columna izquierda de la pantalla
        bookAreaX = sw * 0.20f;
        bookAreaY = sh * 0.25f;
        bookAreaW = shelfX - sw * 0.05f - bookAreaX;
        bookAreaH = sh * 0.50f;

        // Posicionar los 5 libros en la columna izquierda
        float spacing = bookAreaH / (NUM_BOOKS + 1);
        for (int i = 0; i < NUM_BOOKS; i++) {
            float bx = bookAreaX + bookAreaW / 2f - BOOK_W / 2f;
            float by = bookAreaY + spacing * (i + 1) - BOOK_H / 2f;
            bookRects[i]  = new Rectangle(bx, by, BOOK_W, BOOK_H);
            bookPlaced[i] = false;
        }
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        handleInput(mouseX, mouseY);

        // ── Dibujar estantería ────────────────────────────────
        batch.draw(shelfRegion, shelfX, shelfY, shelfW, shelfH);

        // ── Highlight de slots (ShapeRenderer) ────────────────
        batch.end();
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        for (int i = 0; i < NUM_BOOKS; i++) {
            Rectangle r = slotRects[i];
            boolean isFilled = slotFilled[i] != -1;

            // Hover: el libro arrastrado está cerca del slot
            boolean hover = false;
            if (draggingIdx != -1) {
                float cx = mouseX, cy = mouseY;
                hover = r.contains(cx, cy);
            }

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            if (isFilled) {
                shapeRenderer.setColor(Color.GREEN);
            } else if (hover) {
                shapeRenderer.setColor(Color.YELLOW);
            } else {
                shapeRenderer.setColor(new Color(0.7f, 0.7f, 0.7f, 0.5f));
            }
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
            shapeRenderer.end();
        }

        batch.begin();



        // ── Dibujar libros en slots (ya colocados) ────────────
        for (int s = 0; s < NUM_BOOKS; s++) {
            int bi = slotFilled[s];
            if (bi == -1) continue;
            Rectangle sr = slotRects[s];
            // Centrar el libro dentro del slot
            float bx = sr.x + sr.width / 2f  - BOOK_W / 2f;
            float by = sr.y + sr.height / 2f - BOOK_H / 2f;
            batch.draw(bookRegions[bi], bx, by, BOOK_W, BOOK_H);
        }

        // ── Dibujar libros sueltos (no colocados, no arrastrando) ──
        for (int i = 0; i < NUM_BOOKS; i++) {
            if (bookPlaced[i] || draggingIdx == i) continue;
            Rectangle r = bookRects[i];
            batch.draw(bookRegions[i], r.x, r.y, r.width, r.height);
        }

        // ── Dibujar libro siendo arrastrado (encima de todo) ──
        if (draggingIdx != -1) {
            batch.draw(bookRegions[draggingIdx],
                mouseX - BOOK_W / 2f + dragOffX,
                mouseY - BOOK_H / 2f + dragOffY,
                BOOK_W, BOOK_H);
        }
    }

    // ── Input ─────────────────────────────────────────────────

    private void handleInput(float mouseX, float mouseY) {
        // Iniciar drag
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && draggingIdx == -1) {
            // Primero revisar libros sueltos
            for (int i = NUM_BOOKS - 1; i >= 0; i--) {
                if (bookPlaced[i]) continue;
                if (bookRects[i].contains(mouseX, mouseY)) {
                    draggingIdx = i;
                    dragOffX = 0;
                    dragOffY = 0;
                    return;
                }
            }
            // También permitir arrastrar libros ya colocados (para recolocarlos)
            for (int s = 0; s < NUM_BOOKS; s++) {
                int bi = slotFilled[s];
                if (bi == -1) continue;
                Rectangle sr = slotRects[s];
                float bx = sr.x + sr.width / 2f  - BOOK_W / 2f;
                float by = sr.y + sr.height / 2f - BOOK_H / 2f;
                Rectangle placed = new Rectangle(bx, by, BOOK_W, BOOK_H);
                if (placed.contains(mouseX, mouseY)) {
                    // Sacar el libro del slot
                    draggingIdx   = bi;
                    bookPlaced[bi] = false;
                    slotFilled[s] = -1;
                    dragOffX = 0;
                    dragOffY = 0;
                    return;
                }
            }
        }

        // Soltar
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && draggingIdx != -1) {
            boolean dropped = false;

            // Buscar slot más cercano al cursor
            for (int s = 0; s < NUM_BOOKS; s++) {
                if (slotFilled[s] != -1) continue; // ocupado
                Rectangle sr = slotRects[s];
                if (sr.contains(mouseX, mouseY)) {
                    // Colocar en el slot
                    slotFilled[s]        = draggingIdx;
                    bookPlaced[draggingIdx] = true;
                    dropped = true;
                    System.out.println("[Library] Libro " + draggingIdx + " → slot " + s);
                    checkCompletion();
                    break;
                }
            }

            if (!dropped) {
                // Clampear de vuelta al área de libros (no puede salirse del panel)
                Rectangle r = bookRects[draggingIdx];
                r.x = MathUtils.clamp(mouseX - BOOK_W / 2f,
                    bookAreaX, bookAreaX + bookAreaW - BOOK_W);
                r.y = MathUtils.clamp(mouseY - BOOK_H / 2f,
                    bookAreaY, bookAreaY + bookAreaH - BOOK_H);
            }

            draggingIdx = -1;
        }
    }

    private void checkCompletion() {
        for (int s = 0; s < NUM_BOOKS; s++) {
            if (slotFilled[s] == -1) return; // aún hay slots vacíos
        }
        System.out.println("[Library] Todos los libros colocados. ¡Completado!");
        complete();
    }

    @Override
    public void dispose() {
        atlas.dispose();
        shapeRenderer.dispose();
        super.dispose();
    }
}
