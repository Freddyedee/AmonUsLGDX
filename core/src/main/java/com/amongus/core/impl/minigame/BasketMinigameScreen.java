package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Minijuego: Basket.
 *
 * Mecánica:
 *  - La canasta (basketBskt) sigue el eje X del ratón, fija en Y (suelo)
 *  - Balones caen desde arriba en posiciones X aleatorias
 *  - Atrapar 5 balones → victoria
 *  - Si un balón toca el suelo sin ser atrapado → desaparece
 *  - La canasta no puede salirse de los límites del minijuego
 */
public class BasketMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas ─────────────────────────────────────────────────
    private TextureAtlas atlas;
    private TextureRegion bgRegion;
    private TextureRegion bsktRegion;
    private TextureRegion ballRegion;

    // ── Layout ────────────────────────────────────────────────
    // El minijuego ocupa un panel centrado en pantalla
    private float panelX, panelY, panelW, panelH;
    private float groundY;   // Y del suelo dentro del panel

    // ── Canasta ───────────────────────────────────────────────
    private static final float BSKT_H    = 90f;
    private float bsktW;
    private float bsktX;     // posición X del centro de la canasta

    // ── Balones que caen ──────────────────────────────────────
    private static final float BALL_SIZE    = 55f;
    private static final float BALL_SPEED   = 320f;   // píxeles por segundo
    private static final float SPAWN_INTERVAL = 1.1f; // segundos entre spawns

    private float spawnTimer = 0f;

    private static class FallingBall {
        float x, y;
        FallingBall(float x, float y) { this.x = x; this.y = y; }
    }
    private final List<FallingBall> balls = new ArrayList<>();

    // ── Puntuación ────────────────────────────────────────────
    private static final int BALLS_TO_WIN = 5;
    private int caught = 0;

    // ── Font para marcador ────────────────────────────────────
    private BitmapFont font;

    public BasketMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/basket/basketBall.atlas"));

        bgRegion   = atlas.findRegion("basketBg-removebg-preview");
        bsktRegion = atlas.findRegion("basketBskt-removebg-preview");
        ballRegion = atlas.findRegion("basketBall-removebg-preview");

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(2.5f);

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Panel centrado, proporción del fondo (481x330)
        panelH = sh * 0.80f;
        panelW = panelH * (481f / 330f);
        panelX = sw / 2f - panelW / 2f;
        panelY = sh / 2f - panelH / 2f;

        // Suelo: parte baja del panel, dejando un pequeño margen
        groundY = panelY + panelH * 0.08f;

        // Canasta: ancho proporcional al sprite (152x161)
        bsktW = BSKT_H * (152f / 161f);
        bsktX = panelX + panelW / 2f;  // empieza en el centro
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        // mouseY no se usa para la canasta (eje Y fijo)

        updateBasket(mouseX);
        updateBalls(delta);
        checkCatches();

        // ── Dibujar fondo ─────────────────────────────────────
        batch.draw(bgRegion, panelX, panelY, panelW, panelH);

        // ── Dibujar balones ───────────────────────────────────
        for (FallingBall ball : balls) {
            batch.draw(ballRegion,
                ball.x - BALL_SIZE / 2f,
                ball.y - BALL_SIZE / 2f,
                BALL_SIZE, BALL_SIZE);
        }

        // ── Dibujar canasta ───────────────────────────────────
        batch.draw(bsktRegion,
            bsktX - bsktW / 2f,
            groundY,
            bsktW, BSKT_H);

        // ── Marcador ──────────────────────────────────────────
        font.draw(batch,
            caught + " / " + BALLS_TO_WIN,
            panelX + 20f,
            panelY + panelH - 15f);
    }

    // ── Lógica ────────────────────────────────────────────────

    private void updateBasket(float mouseX) {
        // Seguir el X del ratón, clampear dentro del panel
        bsktX = MathUtils.clamp(
            mouseX,
            panelX + bsktW / 2f,
            panelX + panelW - bsktW / 2f
        );
    }

    private void updateBalls(float delta) {
        // Spawn de nuevos balones
        spawnTimer += delta;
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f;
            float spawnX = MathUtils.random(
                panelX + BALL_SIZE / 2f,
                panelX + panelW - BALL_SIZE / 2f
            );
            float spawnY = panelY + panelH - BALL_SIZE;
            balls.add(new FallingBall(spawnX, spawnY));
        }

        // Mover balones hacia abajo y eliminar los que tocan el suelo
        Iterator<FallingBall> it = balls.iterator();
        while (it.hasNext()) {
            FallingBall ball = it.next();
            ball.y -= BALL_SPEED * delta;
            if (ball.y - BALL_SIZE / 2f <= groundY) {
                it.remove(); // tocó el suelo sin ser atrapado
            }
        }
    }

    private void checkCatches() {
        // Hitbox de la canasta (parte superior, donde entra el balón)
        float bsktTop    = groundY + BSKT_H;
        float bsktLeft   = bsktX - bsktW / 2f;
        float bsktRight  = bsktX + bsktW / 2f;
        // Solo los 30% superiores de la canasta atrapan balones
        float catchZoneY = bsktTop - BSKT_H * 0.30f;

        Iterator<FallingBall> it = balls.iterator();
        while (it.hasNext()) {
            FallingBall ball = it.next();
            boolean inX = ball.x + BALL_SIZE / 2f >= bsktLeft
                && ball.x - BALL_SIZE / 2f <= bsktRight;
            boolean inY = ball.y <= bsktTop && ball.y >= catchZoneY;

            if (inX && inY) {
                it.remove();
                caught++;
                System.out.println("[Basket] Atrapado! " + caught + "/" + BALLS_TO_WIN);

                if (caught >= BALLS_TO_WIN && !isCompleted) {
                    complete();
                }
            }
        }
    }

    @Override
    public void dispose() {
        atlas.dispose();
        font.dispose();
        super.dispose();
    }
}
