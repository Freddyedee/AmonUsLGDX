package com.amongus.core.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Overlay del minimapa.
 *
 * - Muestra el mapa actual semitransparente centrado en pantalla
 * - Dibuja marcadores de tareas (pendientes = amarillo, completadas = gris)
 * - Dibuja la posición del jugador local en tiempo real (punto verde)
 * - Se cierra con ESC o con el botón X
 * - El jugador puede seguir moviéndose con el mapa abierto
 *
 * Uso en GameScreen:
 *   minimap.render(batch, shapeRenderer, snapshot, worldW, worldH)
 */
public class MinimapOverlay {

    // ── Mapa actual ───────────────────────────────────────────
    private Texture mapTexture;
    private String  currentMapPath = "";

    // ── Layout del panel ──────────────────────────────────────
    // El panel ocupa el 55% de la pantalla, centrado
    private static final float PANEL_SCALE = 0.55f;
    private float panelX, panelY, panelW, panelH;

    // Botón X de cierre
    private Rectangle closeBtn;
    private static final float CLOSE_SIZE = 36f;

    // ── Estado ────────────────────────────────────────────────
    private boolean visible = false;

    // ── Input: evitar toggle múltiple en un frame ─────────────
    private boolean mWasPressed = false;

    // ── Marcadores ───────────────────────────────────────────
    private static final float TASK_DOT_RADIUS   = 7f;
    private static final float PLAYER_DOT_RADIUS = 7f;
    private Texture playerIconTexture;

    // ── Font ─────────────────────────────────────────────────
    private BitmapFont font;

    public MinimapOverlay() {
        font = new BitmapFont();
        playerIconTexture = new Texture(Gdx.files.internal("sprites/playerVoteResults.png"));
        font.setColor(Color.WHITE);
        font.getData().setScale(1.4f);
        recalculateLayout();
    }

    private void recalculateLayout() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        panelW = sw * PANEL_SCALE;
        panelH = sh * PANEL_SCALE;
        panelX = sw / 2f - panelW / 2f;
        panelY = sh / 2f - panelH / 2f;

        // Botón X en esquina superior derecha del panel
        closeBtn = new Rectangle(
            panelX + panelW - CLOSE_SIZE - 8f,
            panelY + panelH - CLOSE_SIZE - 8f,
            CLOSE_SIZE, CLOSE_SIZE
        );
    }

    // ── API pública ───────────────────────────────────────────

    public boolean isVisible() { return visible; }

    public void show() { visible = true; }
    public void hide() { visible = false; }
    public void toggle() { visible = !visible; }

    /**
     * Carga (o reutiliza) la textura del mapa indicado.
     * @param mapPath ruta relativa desde assets, ej: "mapas/mapa1.png"
     */
    public void setMap(String mapPath) {
        if (mapPath.equals(currentMapPath)) return;
        if (mapTexture != null) mapTexture.dispose();
        mapTexture      = new Texture(Gdx.files.internal(mapPath));
        currentMapPath  = mapPath;
    }

    /**
     * Procesar input (M para toggle, ESC y click en X para cerrar).
     * Llamar ANTES de render, cada frame.
     */
    public void handleInput() {

        // Tecla M – toggle
        boolean mNow = Gdx.input.isKeyPressed(Input.Keys.M);
        if (mNow && !mWasPressed) toggle();
        mWasPressed = mNow;

        if (!visible) return;

        // ESC cierra
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            hide();
            return;
        }

        // Click en botón X
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (closeBtn.contains(mx, my)) hide();
        }
    }

    /**
     * Renderizar el minimapa completo.
     *
     * @param batch          SpriteBatch ya con proyección de pantalla (screenMatrix)
     * @param shapeRenderer  ShapeRenderer compartido
     * @param snapshot       snapshot actual del juego
     * @param worldW         anchura total del mundo en unidades de juego
     * @param worldH         altura total del mundo en unidades de juego
     */
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer,
                       GameSnapshot snapshot, float worldW, float worldH) {
        if (!visible || mapTexture == null) return;

        recalculateLayout(); // por si cambia resolución

        // ── 1. Fondo oscuro semitransparente detrás del panel ─
        batch.begin();
        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(getWhitePixel(), 0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // ── 2. Mapa semitransparente ──────────────────────────
        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.80f);
        batch.draw(mapTexture, panelX, panelY, panelW, panelH);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // ── 3. Marcadores (ShapeRenderer) ─────────────────────
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // Tareas
        if (snapshot.getTasks() != null) {
            for (TaskView tv : snapshot.getTasks()) {
                float mx = worldToMapX(tv.getPosition().x(), worldW);
                float my = worldToMapY(tv.getPosition().y(), worldH);

                // Relleno
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                if (tv.isCompleted()) {
                    shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f); // gris = completada
                } else {
                    shapeRenderer.setColor(1f, 0.85f, 0f, 1f);    // amarillo = pendiente
                }
                shapeRenderer.circle(mx, my, TASK_DOT_RADIUS);
                shapeRenderer.end();

                // Borde
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.BLACK);
                shapeRenderer.circle(mx, my, TASK_DOT_RADIUS);
                shapeRenderer.end();
            }
        }

        // Jugador local
        if (snapshot.getLocalPlayer() != null) {
            float px = worldToMapX(snapshot.getLocalPlayer().getPosition().x(), worldW);
            float py = worldToMapY(snapshot.getLocalPlayer().getPosition().y(), worldH);

            float iconSize = PLAYER_DOT_RADIUS * 2.8f;
            batch.begin();
            batch.draw(playerIconTexture,
                px - iconSize / 2f,
                py - iconSize / 2f,
                iconSize, iconSize);
            batch.end();
        }

        // ── 4. Borde del panel ────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // ── 5. Botón X ────────────────────────────────────────
        batch.begin();
        font.draw(batch, "X",
            closeBtn.x + closeBtn.width / 2f - 6f,
            closeBtn.y + closeBtn.height / 2f + 8f);
        batch.end();

        // Borde del botón X
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height);
        shapeRenderer.end();

        // ── 6. Leyenda ────────────────────────────────────────
        batch.begin();
        font.draw(batch, "MAPA", panelX + 10f, panelY + panelH - 10f);
        batch.end();
    }

    // ── Conversión coordenadas mundo → pantalla ───────────────

    private float worldToMapX(float worldX, float worldW) {
        return panelX + (worldX / worldW) * panelW;
    }

    private float worldToMapY(float worldY, float worldH) {
        return panelY + (worldY / worldH) * panelH;
    }

    // ── Pixel blanco reutilizable ─────────────────────────────
    private static Texture whitePixel;
    private static Texture getWhitePixel() {
        if (whitePixel == null || whitePixel.getTextureObjectHandle() == 0) {
            com.badlogic.gdx.graphics.Pixmap pm =
                new com.badlogic.gdx.graphics.Pixmap(1, 1,
                    com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whitePixel = new Texture(pm);
            pm.dispose();
        }
        return whitePixel;
    }

    public void dispose() {
        if (mapTexture != null) mapTexture.dispose();
        if (whitePixel != null) { whitePixel.dispose(); whitePixel = null; }
        font.dispose();
        if (playerIconTexture != null) playerIconTexture.dispose();
    }
}
