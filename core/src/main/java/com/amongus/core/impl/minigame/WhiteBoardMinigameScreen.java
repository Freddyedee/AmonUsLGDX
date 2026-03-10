package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/**
 * Minijuego: Limpiar la Pizarra.
 *
 * Mecánica:
 *  - Se muestran 3 objetos sobre la pizarra
 *  - El cursor del ratón se reemplaza por el borrador
 *  - Click/mantener borra píxeles de los objetos en el área del borrador
 *  - Cada objeto tiene un % de píxeles borrados; al superar el umbral se considera limpio
 *  - Cuando los 3 están limpios, la tarea se completa
 *
 * Implementación:
 *  - Cada objeto se carga en un Pixmap (editable en CPU)
 *  - Al borrar, se ponen píxeles a transparente en el Pixmap
 *  - Se recrea la Texture desde el Pixmap actualizado cada frame
 */
public class WhiteBoardMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas y sprites estáticos ─────────────────────────────
    private TextureAtlas atlas;
    private Texture boardTexture;    // pizarra de fondo (no se borra)
    private Texture eraserTexture;   // cursor borrador

    // ── Objetos borrables ─────────────────────────────────────
    private static final int NUM_OBJECTS = 3;
    private static final String[] REGION_NAMES = {
        "whiteBoardobj1-removebg-preview",
        "whiteBoardobj2-removebg-preview",
        "whiteBoardobj3-removebg-preview"
    };

    // Pixmaps editables (donde borramos píxeles)
    private Pixmap[]  objPixmaps  = new Pixmap[NUM_OBJECTS];
    // Texturas que se renderizan (se recrean cuando el pixmap cambia)
    private Texture[] objTextures = new Texture[NUM_OBJECTS];
    // Píxeles originales visibles (no transparentes) de cada objeto
    private int[]     totalPixels = new int[NUM_OBJECTS];
    // Marca si el pixmap cambió y hay que subir la textura
    private boolean[] dirty       = new boolean[NUM_OBJECTS];
    // Marca si el objeto ya está suficientemente limpio
    private boolean[] cleaned     = new boolean[NUM_OBJECTS];

    // Umbral para considerar un objeto limpio (85% borrado)
    private static final float CLEAN_THRESHOLD = 0.95f;

    // ── Layout ────────────────────────────────────────────────
    private float boardX, boardY, boardW, boardH;

    // Posiciones de cada objeto SOBRE la pizarra (en coordenadas de pantalla)
    private Rectangle[] objRects = new Rectangle[NUM_OBJECTS];

    // ── Borrador ──────────────────────────────────────────────
    private static final float ERASER_SIZE    = 60f;   // tamaño visual del cursor
    private static final float ERASER_RADIUS  = 10f;   // radio de borrado en pantalla

    public WhiteBoardMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();

        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/whiteBoard/whiteBoard.atlas"));

        // ── Pizarra de fondo ──────────────────────────────────
        TextureRegion boardRegion = atlas.findRegion("whiteBoardbg");
        boardTexture = copyRegionToTexture(boardRegion);

        // ── Borrador (cursor) ─────────────────────────────────
        // Nombre del atlas con UUID — buscamos por prefijo
        TextureAtlas.AtlasRegion eraserRegion = null;
        for (TextureAtlas.AtlasRegion r : atlas.getRegions()) {
            if (r.name.startsWith("156b4863")) { eraserRegion = r; break; }
        }
        eraserTexture = copyRegionToTexture(eraserRegion);

        // ── Layout ────────────────────────────────────────────
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        boardH = sh * 0.75f;
        boardW = boardH * (408f / 612f);
        boardX = sw / 2f - boardW / 2f;
        boardY = sh / 2f - boardH / 2f;

        // ── Cargar objetos en Pixmap ──────────────────────────
        // Posiciones de los objetos dentro de la pizarra
        // Posiciones de los 3 posibles objetos
        float[] relX = {0.30f, 0.30f, 0.30f};  // todos centrados horizontalmente
        float[] relY = {0.35f, 0.40f, 0.35f};  // todos centrados verticalmente
        float[] relW = {0.40f, 0.38f, 0.38f};  // más grandes

        // Elegir uno al azar
                int chosen = (int)(Math.random() * NUM_OBJECTS);
                System.out.println("[WhiteBoard] objeto elegido: " + chosen + " → " + REGION_NAMES[chosen]);

        // Marcar los no elegidos como ya limpios
                for (int i = 0; i < NUM_OBJECTS; i++) {
                    cleaned[i]    = (i != chosen);
                    objPixmaps[i]  = null;
                    objTextures[i] = null;
                    objRects[i]    = null;
                }

        // Cargar solo el objeto elegido
                TextureRegion region = atlas.findRegion(REGION_NAMES[chosen]);
                System.out.println("[WhiteBoard] región encontrada: " + (region != null));

                objPixmaps[chosen]  = regionToPixmap(region);
                objTextures[chosen] = new Texture(objPixmaps[chosen]);
                totalPixels[chosen] = countVisiblePixels(objPixmaps[chosen]);
                dirty[chosen]       = false;

        // Asignar rect del objeto elegido
                float w = boardW * relW[chosen];
                float h = w * ((float) region.getRegionHeight() / region.getRegionWidth());
                float x = boardX + boardW * relX[chosen];
                float y = boardY + boardH * relY[chosen];
                objRects[chosen] = new Rectangle(x, y, w, h);


        // No usamos setCursor() para máxima compatibilidad;
        // simplemente dibujamos el borrador sobre el cursor nativo
        System.out.println("[WhiteBoard] objeto elegido: " + chosen + " → " + REGION_NAMES[chosen]);
        System.out.println("[WhiteBoard] región encontrada: " + (atlas.findRegion(REGION_NAMES[chosen]) != null));
        System.out.println("[WhiteBoard] totalPixels: " + totalPixels[chosen]);
        System.out.println("[WhiteBoard] rect: " + objRects[chosen]);
        System.out.println("[WhiteBoard] textura: " + objTextures[chosen]);
        System.out.println("[WhiteBoard] cleaned[" + chosen + "] = " + cleaned[chosen]);
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // invertir Y

        // ── 1. Procesar borrado ───────────────────────────────
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            eraseAtPosition(mouseX, mouseY);
        }

        // ── 2. Actualizar texturas sucias ─────────────────────
        for (int i = 0; i < NUM_OBJECTS; i++) {
            if (dirty[i]) {
                System.out.println("[WhiteBoard] recreando textura " + i);
                objTextures[i].dispose();
                objTextures[i] = new Texture(objPixmaps[i]);
                dirty[i] = false;

                // Comprobar si ya está suficientemente limpio
                if (!cleaned[i]) {
                    int remaining = countVisiblePixels(objPixmaps[i]);
                    float erased  = 1f - (float) remaining / totalPixels[i];
                    if (erased >= CLEAN_THRESHOLD) {
                        cleaned[i] = true;
                        System.out.println("[WhiteBoard] Objeto " + (i+1) + " limpio!");
                    }
                }
            }
        }


        // ── 3. Dibujar pizarra ────────────────────────────────
        batch.draw(boardTexture, boardX, boardY, boardW, boardH);

        // ── 4. Dibujar objetos (con sus píxeles borrados) ─────
        for (int i = 0; i < NUM_OBJECTS; i++) {
            if (!cleaned[i] && objRects[i] != null && objTextures[i] != null){
                Rectangle r = objRects[i];
                batch.draw(objTextures[i], r.x, r.y, r.width, r.height);
            }
            // Si está limpio, no se dibuja → ya desapareció
        }

        // ── 5. Dibujar cursor borrador ────────────────────────
        batch.draw(eraserTexture,
            mouseX - ERASER_SIZE / 2f,
            mouseY - ERASER_SIZE / 2f,
            ERASER_SIZE, ERASER_SIZE);

        // ── 6. Comprobar victoria ─────────────────────────────
        boolean allClean = true;
        for (boolean c : cleaned) if (!c) { allClean = false; break; }
        if (allClean && !isCompleted) {
            complete();
        }
    }

    // ── Borrado ───────────────────────────────────────────────

    /**
     * Borra píxeles en todos los objetos que estén bajo el cursor.
     */
    private void eraseAtPosition(float screenX, float screenY) {
        for (int i = 0; i < NUM_OBJECTS; i++) {
            if (cleaned[i] || objRects[i] == null || objPixmaps[i] == null) continue;

            objPixmaps[i].setBlending(Pixmap.Blending.None);

            Rectangle r = objRects[i];
            System.out.println("[Erase] mouse=(" + screenX + "," + screenY +
                ") rect=(" + r.x + "," + r.y + "," + r.width + "," + r.height + ")");

            // Convertir posición de pantalla a coordenadas del Pixmap
            float relX = (screenX - r.x) / r.width;
            float relY = (screenY - r.y) / r.height;
            System.out.println("[Erase] relX=" + relX + " relY=" + relY);

            // Relativo fuera del objeto → ignorar
            if (relX < 0 || relX > 1 || relY < 0 || relY > 1) continue;

            // Coordenadas en el Pixmap (Y invertida porque LibGDX vs Pixmap)
            int pmW = objPixmaps[i].getWidth();
            int pmH = objPixmaps[i].getHeight();

            int centerPx = (int)(relX * pmW);
            int centerPy = (int)((1f - relY) * pmH); // invertir Y

            // Radio en píxeles del pixmap (proporcional al tamaño en pantalla)
            int radiusPx = Math.max(5, (int)(ERASER_RADIUS * ((float) pmW / r.width)));

            System.out.println("[Erase] pmW=" + pmW + " pmH=" + pmH +
                " centerPx=" + centerPx + " centerPy=" + centerPy +
                " radiusPx=" + radiusPx);

            // Borrar píxeles en el radio del borrador
            for (int dy = -radiusPx; dy <= radiusPx; dy++) {
                for (int dx = -radiusPx; dx <= radiusPx; dx++) {
                    if (dx*dx + dy*dy > radiusPx * radiusPx) continue; // círculo

                    int px = centerPx + dx;
                    int py = centerPy + dy;

                    if (px < 0 || px >= pmW || py < 0 || py >= pmH) continue;

                    // Solo borrar si el píxel tiene alpha > 0 (es visible)
                    int pixel = objPixmaps[i].getPixel(px, py);
                    int alpha  = pixel & 0xFF;
                    if (alpha > 0) {
                        objPixmaps[i].drawPixel(px, py, 0); // 0 = RGBA 0x00000000 = transparente
                        dirty[i] = true;
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Convierte una TextureRegion a un Pixmap editable (en CPU).
     */
    private Pixmap regionToPixmap(TextureRegion region) {
        int rw = region.getRegionWidth();
        int rh = region.getRegionHeight();

        // 1. CORRECCIÓN: Se añade un 'false' extra al final para el parámetro hasStencil
        com.badlogic.gdx.graphics.glutils.FrameBuffer fb =
            new com.badlogic.gdx.graphics.glutils.FrameBuffer(
                Pixmap.Format.RGBA8888, rw, rh, false, false);

        fb.begin();
        com.badlogic.gdx.graphics.g2d.SpriteBatch tmpBatch =
            new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        tmpBatch.getProjectionMatrix().setToOrtho2D(0, 0, rw, rh);
        tmpBatch.begin();

        tmpBatch.draw(region.getTexture(),
            0, 0, rw, rh,
            region.getRegionX(), region.getRegionY(),
            rw, rh, false, true);
        tmpBatch.end();

        // 2. CORRECCIÓN: Usamos el método moderno en lugar de ScreenUtils
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, rw, rh);
        fb.end();

        tmpBatch.dispose();
        fb.dispose();
        return pm;
    }

    /**
     * Copia una TextureRegion a una Texture independiente.
     */
    private Texture copyRegionToTexture(TextureRegion region) {
        Pixmap pm = regionToPixmap(region);
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    /**
     * Cuenta píxeles con alpha > 0 en un Pixmap.
     */
    private int countVisiblePixels(Pixmap pm) {
        int count = 0;
        for (int y = 0; y < pm.getHeight(); y++) {
            for (int x = 0; x < pm.getWidth(); x++) {
                if ((pm.getPixel(x, y) & 0xFF) > 10) count++;
            }
        }
        return count;
    }

    @Override
    public void dispose() {
        atlas.dispose();
        boardTexture.dispose();
        eraserTexture.dispose();
        for (int i = 0; i < NUM_OBJECTS; i++) {
            if (objPixmaps[i]  != null) objPixmaps[i].dispose();
            if (objTextures[i] != null) objTextures[i].dispose();
        }
        super.dispose();
    }
}
