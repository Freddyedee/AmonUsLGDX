package com.amongus.core.view;

import com.amongus.core.GameScreen;
import com.amongus.core.api.state.GameState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class HudRenderer {
    private final Texture spriteSheet;
    private final TextureRegion btnReport;
    private final TextureRegion btnKill;
    private final Texture btnConfiguration;
    private final Texture btnVent;
    private final Texture btnComenzar;

    private static final float BUTTON_SIZE   = 120f;
    private static final float BUTTON_MARGIN = 20f;
    private static final float BUTTON_Y      = 20f;

    // ── Animación de aparición ────────────────────────────────
    private float reportAlpha = 0f;
    private float ventAlpha   = 0f; // NUEVO: Fade del botón Vent
    private static final float FADE_SPEED = 4f;

    // ── Posiciones cacheadas para detección de click
    private float killX;
    private float reportX;
    private float ventX;
    private float configurationX;
    private float configurationY;

    private final BitmapFont font;

    public HudRenderer() {
        spriteSheet      = new Texture(Gdx.files.internal("hud/AmongUsSprites.png"));
        btnReport        = new TextureRegion(spriteSheet, 116, 764, 116, 115);
        btnKill          = new TextureRegion(spriteSheet, 336, 764, 116, 115);
        btnConfiguration = new Texture(Gdx.files.internal("hud/Configuración.png"));
        btnVent          = new Texture(Gdx.files.internal("hud/Ventilacion.png"));
        btnComenzar      = new Texture(Gdx.files.internal("hud/Comenzar.png"));

        font = new BitmapFont();
        font.setColor(Color.WHITE);
    }

    public void draw(SpriteBatch batch, boolean killReady, boolean reportReady, boolean canVent, float delta, float killCooldown) {
        // Actualizar fades
        reportAlpha = reportReady ? Math.min(1f, reportAlpha + FADE_SPEED * delta) : Math.max(0f, reportAlpha - FADE_SPEED * delta);
        ventAlpha   = canVent     ? Math.min(1f, ventAlpha   + FADE_SPEED * delta) : Math.max(0f, ventAlpha   - FADE_SPEED * delta);

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        this.killX          = screenWidth - BUTTON_SIZE - BUTTON_MARGIN;
        this.reportX        = this.killX - BUTTON_SIZE - BUTTON_MARGIN;
        this.ventX          = this.reportX - BUTTON_SIZE - BUTTON_MARGIN;

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.begin();

        // ── Botón KILL ────────────────────────────────────────
        float killBrightness = killReady ? 1f : 0.35f;
        batch.setColor(killBrightness, killBrightness, killBrightness, 1f);
        batch.draw(btnKill, killX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE);

        // ── Botón REPORT ──────────────────────────────────────
        if (reportAlpha > 0f) {
            batch.setColor(1f, 1f, 1f, reportAlpha);
            batch.draw(btnReport, reportX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE);
        }

        // ── Botón VENTILACIÓN ─────────────────────────────────
        if (ventAlpha > 0f) {
            batch.setColor(1f, 1f, 1f, ventAlpha);
            batch.draw(btnVent, ventX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE);
        }


        batch.setColor(1, 1, 1, 1);
        batch.end();

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!killReady) {
            batch.begin();
            int segundos = (int) Math.ceil(killCooldown);
            font.draw(batch, String.valueOf(segundos), killX + BUTTON_SIZE / 2f - 5, BUTTON_Y + BUTTON_SIZE + 20);
            batch.end();
        }
    }

    // Dibujo del botón de Comenzar (Específico para el Lobby)
    public void drawStartButton(SpriteBatch batch) {
        float startX = Gdx.graphics.getWidth() - BUTTON_SIZE - BUTTON_MARGIN;
        float startY = BUTTON_Y; // Lo posicionamos abajo a la derecha

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.draw(btnComenzar, startX, startY, BUTTON_SIZE, BUTTON_SIZE);
        batch.end();
    }


    //Dibujo del botón de configuración independiente
    public void drawConfigButton(SpriteBatch batch) {
        float configX = Gdx.graphics.getWidth() - BUTTON_SIZE - BUTTON_MARGIN;
        float configY = Gdx.graphics.getHeight() - BUTTON_SIZE - BUTTON_MARGIN;

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.draw(btnConfiguration, configX, configY, BUTTON_SIZE, BUTTON_SIZE);
        batch.end();
    }

    // ── Detección de clicks ───────────────────────────────────
    public boolean isStartClicked() {
        if (!Gdx.input.justTouched()) return false;

        float startX = Gdx.graphics.getWidth() - BUTTON_SIZE - BUTTON_MARGIN;
        float startY = BUTTON_Y;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();

        return mx >= startX && mx <= startX + BUTTON_SIZE && my >= startY && my <= startY + BUTTON_SIZE;
    }

    public boolean isKillClicked() {
        if (!Gdx.input.justTouched()) return false;
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= killX && mx <= killX + BUTTON_SIZE && my >= BUTTON_Y && my <= BUTTON_Y + BUTTON_SIZE;
    }

    public boolean isReportClicked() {
        if (!Gdx.input.justTouched()) return false;
        if (reportAlpha < 0.5f) return false;
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= reportX && mx <= reportX + BUTTON_SIZE && my >= BUTTON_Y && my <= BUTTON_Y + BUTTON_SIZE;
    }

    public boolean isVentClicked() {
        if (!Gdx.input.justTouched()) return false;
        if (ventAlpha < 0.5f) return false; // Inactivo si es casi invisible
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= ventX && mx <= ventX + BUTTON_SIZE && my >= BUTTON_Y && my <= BUTTON_Y + BUTTON_SIZE;
    }

    public boolean isConfigurationClicked() {
        if (!Gdx.input.justTouched()) return false;

        // Calculamos independientemente donde está el botón arriba a la derecha
        float configX = Gdx.graphics.getWidth() - BUTTON_SIZE - BUTTON_MARGIN;
        float configY = Gdx.graphics.getHeight() - BUTTON_SIZE - BUTTON_MARGIN;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();

        return mx >= configX && mx <= configX + BUTTON_SIZE && my >= configY && my <= configY + BUTTON_SIZE;
    }

    public void dispose() {
        font.dispose();
        spriteSheet.dispose();
        btnComenzar.dispose();
        btnConfiguration.dispose();
        btnVent.dispose();
    }
}
