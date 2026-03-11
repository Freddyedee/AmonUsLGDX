package com.amongus.core.view;

import com.amongus.core.impl.sabotage.SabotageManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Overlay del mapa de sabotaje (solo para impostores).
 *
 * - Muestra el mapa con tinte rojo semitransparente
 * - 2 botones de sabotaje (panel_drill_alert) en posiciones del mapa
 * - Cerrar con ESC o X
 */
public class SabotageMapOverlay {

    // ── Assets ────────────────────────────────────────────────
    private Texture mapTexture;
    private Texture internetButtonTexture;
    private Texture lightsButtonTexture;
    private String  currentMapPath = "";

    // ── Layout ────────────────────────────────────────────────
    private static final float PANEL_SCALE = 0.55f;
    private float panelX, panelY, panelW, panelH;
    private Rectangle closeBtn;
    private static final float CLOSE_SIZE = 36f;

    // ── Botones de sabotaje ───────────────────────────────────
    // Posiciones relativas al panel (fracción del panel)
    // Ajustar según dónde quieras los puntos de sabotaje en el mapa
    private static final float[][] SABOTAGE_POSITIONS = {
        {0.30f, 0.60f},   // Punto sabotaje 1
        {0.65f, 0.35f},   // Punto sabotaje 2
    };
    private static final float ALERT_BTN_SIZE = 48f;
    private Rectangle[] sabotageButtons = new Rectangle[2];

    // Qué tipo activa cada botón
    private static final SabotageManager.SabotageType[] SABOTAGE_TYPES = {
        SabotageManager.SabotageType.INTERNET,
        SabotageManager.SabotageType.LIGHTS, // segundo slot para futuro segundo sabotaje
    };

    // ── Estado ────────────────────────────────────────────────
    private boolean visible = false;
    private boolean nWasPressed = false;

    private BitmapFont font;

    // ── Listener ──────────────────────────────────────────────
    public interface SabotageSelectListener {
        void onSabotageSelected(SabotageManager.SabotageType type, int buttonIndex);
    }
    private SabotageSelectListener selectListener;

    public SabotageMapOverlay() {
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.3f);
    }

    public void setSelectListener(SabotageSelectListener l) { this.selectListener = l; }

    public void setMap(String mapPath) {
        if (mapPath.equals(currentMapPath)) return;
        if (mapTexture != null) mapTexture.dispose();
        mapTexture     = new Texture(Gdx.files.internal(mapPath));
        currentMapPath = mapPath;
    }

    public void setInternetButtonTexture(String path) {
        if (internetButtonTexture != null) internetButtonTexture.dispose();
        internetButtonTexture = new Texture(Gdx.files.internal(path));
    }

    public void setLightsButtonTexture(String path) {
        if (lightsButtonTexture != null) lightsButtonTexture.dispose();
        lightsButtonTexture = new Texture(Gdx.files.internal(path));
    }

    public boolean isVisible() { return visible; }
    public void show()   { visible = true; }
    public void hide()   { visible = false; }
    public void toggle() { visible = !visible; }

    public void handleInput(SabotageManager manager) {
        // Tecla N – toggle (solo si puede sabotear)
        boolean nNow = Gdx.input.isKeyPressed(Input.Keys.N);
        if (nNow && !nWasPressed) {
            if (manager.canSabotage()) toggle();
        }
        nWasPressed = nNow;

        if (!visible) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { hide(); return; }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();

            // Botón cerrar
            if (closeBtn != null && closeBtn.contains(mx, my)) { hide(); return; }

            // Botones de sabotaje
            for (int i = 0; i < sabotageButtons.length; i++) {
                if (sabotageButtons[i] != null && sabotageButtons[i].contains(mx, my)) {
                    if (manager.canSabotage()) {
                        manager.activateSabotage(SABOTAGE_TYPES[i]);
                        if (selectListener != null)
                            selectListener.onSabotageSelected(SABOTAGE_TYPES[i], i);
                    }
                    hide();
                    return;
                }
            }
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer,
                       SabotageManager manager) {
        if (!visible || mapTexture == null) return;

        recalculateLayout();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // ── Fondo oscuro ──────────────────────────────────────
        batch.begin();
        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(getWhitePixel(), 0, 0, sw, sh);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // ── Mapa con tinte rojo ───────────────────────────────
        batch.begin();
        batch.setColor(1f, 0.35f, 0.35f, 0.80f);  // tinte rojo
        batch.draw(mapTexture, panelX, panelY, panelW, panelH);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // ── Borde rojo del panel ──────────────────────────────
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // ── Botones de sabotaje ───────────────────────────────
        recalculateSabotageButtons();
        boolean canSabotage = manager.canSabotage();

        batch.begin();
        for (int i = 0; i < sabotageButtons.length; i++) {
            Rectangle btn = sabotageButtons[i];
            Texture icon = (i == 0) ? internetButtonTexture : lightsButtonTexture;
            if (icon != null) {
                if (!canSabotage) batch.setColor(0.5f, 0.5f, 0.5f, 0.7f);
                else              batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(icon, btn.x, btn.y, btn.width, btn.height);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }

        // Cooldown text si no puede sabotear
        if (!canSabotage && manager.getCooldownRemaining() > 0f) {
            font.draw(batch,
                String.format("CD: %.0fs", manager.getCooldownRemaining()),
                panelX + panelW / 2f - 30f,
                panelY + 30f);
        }
        if (manager.hasSabotageActive()) {
            font.draw(batch, "SABOTAJE ACTIVO",
                panelX + panelW / 2f - 60f,
                panelY + 30f);
        }

        // Botón X
        font.draw(batch, "X",
            closeBtn.x + closeBtn.width / 2f - 5f,
            closeBtn.y + closeBtn.height / 2f + 8f);
        batch.end();

        // Borde X
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height);
        shapeRenderer.end();
    }

    private void recalculateLayout() {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        panelW = sw * PANEL_SCALE;
        panelH = sh * PANEL_SCALE;
        panelX = sw / 2f - panelW / 2f;
        panelY = sh / 2f - panelH / 2f;
        closeBtn = new Rectangle(
            panelX + panelW - CLOSE_SIZE - 8f,
            panelY + panelH - CLOSE_SIZE - 8f,
            CLOSE_SIZE, CLOSE_SIZE);
    }

    private void recalculateSabotageButtons() {
        for (int i = 0; i < SABOTAGE_POSITIONS.length; i++) {
            float bx = panelX + panelW * SABOTAGE_POSITIONS[i][0] - ALERT_BTN_SIZE / 2f;
            float by = panelY + panelH * SABOTAGE_POSITIONS[i][1] - ALERT_BTN_SIZE / 2f;
            sabotageButtons[i] = new Rectangle(bx, by, ALERT_BTN_SIZE, ALERT_BTN_SIZE);
        }
    }

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
        if (internetButtonTexture != null) internetButtonTexture.dispose();
        if (lightsButtonTexture   != null) lightsButtonTexture.dispose();
        if (whitePixel != null) { whitePixel.dispose(); whitePixel = null; }
        font.dispose();
    }
}
