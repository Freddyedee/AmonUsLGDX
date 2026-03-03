package com.amongus.core.view;



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

    private static final float BUTTON_SIZE   = 120f;
    private static final float BUTTON_MARGIN = 20f;
    private static final float BUTTON_Y      = 20f;

    // ── Animación de aparición ────────────────────────────────
    private float reportAlpha = 0f; // 0 = invisible, 1 = visible
    private static final float FADE_SPEED = 4f; // velocidad de fade in/out

    // ── Posiciones cacheadas para detección de click
    private float killX;
    private float reportX;

    //Con killcooldown

    private final BitmapFont font;


    public HudRenderer() {
        spriteSheet = new Texture(Gdx.files.internal("hud/AmongUsSprites.png"));
        btnReport = new TextureRegion(spriteSheet, 116, 764, 116, 115);
        btnKill   = new TextureRegion(spriteSheet, 336, 764, 116, 115);
        font = new BitmapFont();
        font.setColor(Color.WHITE);
    }

    public void draw(SpriteBatch batch, boolean killReady, boolean reportReady, float delta, float killCooldown,
                     boolean isImpostor) {

        // Actualizar fade del report
        reportAlpha = reportReady
            ? Math.min(1f, reportAlpha + FADE_SPEED * delta)
            : Math.max(0f, reportAlpha - FADE_SPEED * delta);


        float screenWidth = Gdx.graphics.getWidth();
        this.killX   = screenWidth - BUTTON_SIZE - BUTTON_MARGIN;
        this.reportX = this.killX - BUTTON_SIZE - BUTTON_MARGIN;

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.begin();

        // ── Botón KILL ────────────────────────────────────────
        if(isImpostor) {
            float killBrightness = killReady ? 1f : 0.35f;
            batch.setColor(killBrightness, killBrightness, killBrightness, 1f);
            batch.draw(btnKill, killX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE);
            }

        // ── Botón REPORT — aparece solo si hay cadáver cerca ──
        if (reportAlpha > 0f) {
            batch.setColor(1f, 1f, 1f, reportAlpha);
            batch.draw(btnReport, reportX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE);
        }

        batch.setColor(1, 1, 1, 1);
        batch.end();

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        //Contador de kill cooldown

        if (!killReady) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.begin();
            int segundos = (int) Math.ceil(killCooldown);
            font.draw(batch, String.valueOf(segundos),
                killX + BUTTON_SIZE / 2f - 5,
                BUTTON_Y + BUTTON_SIZE + 20);
            batch.end();
        }
    }

    // ── Detección de clicks ───────────────────────────────────

    public boolean isKillClicked() {
        if (!Gdx.input.justTouched()) return false;
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        System.out.println("[CLICK] mx=" + mx + " my=" + my + " killX=" + killX + " killX+SIZE=" + (killX + BUTTON_SIZE));
        return mx >= killX && mx <= killX + BUTTON_SIZE
            && my >= BUTTON_Y && my <= BUTTON_Y + BUTTON_SIZE;
    }

    public boolean isReportClicked() {
        if (!Gdx.input.justTouched()) return false;
        if (reportAlpha < 0.5f) return false; // no clickeable si casi invisible
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= reportX && mx <= reportX + BUTTON_SIZE
            && my >= BUTTON_Y && my <= BUTTON_Y + BUTTON_SIZE;
    }


    public void dispose() {
        font.dispose();
    }
}
