package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.sabotage.SabotageManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Minijuego: Arreglar el Internet (Sabotaje).
 *
 * Mecánica:
 *  - Se muestra el router (panel_wifi_bg) con una palanca (panel_wifi-lever)
 *  - El jugador arrastra la palanca hacia arriba para arreglar
 *  - Al llegar al tope se resuelve el sabotaje
 */
public class InternetSabotageMinigameScreen extends AbstractMinigameScreen {

    private TextureAtlas  atlas;
    private TextureRegion bgRegion;
    private TextureRegion leverRegion;
    private TextureRegion nodeOnRegion;
    private TextureRegion nodeOffRegion;

    private ShapeRenderer shapeRenderer;

    // ── Layout ────────────────────────────────────────────────
    private static final float PANEL_H = 360f;
    private float panelW, panelX, panelY;

    // Palanca: deslizable verticalmente dentro de un rango
    private float leverY;      // posición actual (pantalla)
    private float leverMinY;   // posición más baja
    private float leverMaxY;   // posición más alta (objetivo)
    private float leverX;
    private static final float LEVER_W = 50f;
    private static final float LEVER_H = 36f;
    private boolean draggingLever = false;
    private float   dragOffY;

    // Progreso: 0=abajo (saboteado) → 1=arriba (arreglado)
    private float progress = 0f;

    // Nodos (indicadores de estado)
    private static final int   NODE_COUNT = 5;
    private static final float NODE_SIZE  = 14f;

    private final SabotageManager sabotageManager;

    // ── Constantes de layout ───────────
    // Panel principal (router izquierdo)
    private static final float PANEL_SCALE  = 0.55f;  // altura relativa a la pantalla
    private static final float PANEL_OFFSET_X = -80f; // negativo = más a la izquierda
    private static final float PANEL_OFFSET_Y =  0f;  // positivo = más arriba


    // Palanca (slider vertical)
    private static final float LEVER_REL_X    = 0.84f; // posición X relativa al panel (0=izq, 1=der)
    private static final float LEVER_MIN_REL  = 0.08f; // Y mínima relativa al panel
    private static final float LEVER_MAX_REL  = 0.65f; // Y máxima relativa al panel

    // Nodos de estado (círculos indicadores)
    private static final float NODE_START_REL_X = 0.12f; // X relativa al panel
    private static final float NODE_START_REL_Y = 0.45f; // Y relativa al panel (desde abajo)
    private static final float NODE_SPACING_REL = 0.090f; // separación entre nodos

    public InternetSabotageMinigameScreen(GameEngine engine, PlayerId playerId,
                                          Task task, SabotageManager sabotageManager) {
        super(engine, playerId, task);
        this.sabotageManager = sabotageManager;
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/sabotageInternet/sabotageInternet.atlas"));

        bgRegion      = atlas.findRegion("panel_wifi_bg");
        leverRegion   = atlas.findRegion("panel_wifi-lever");
        nodeOnRegion  = atlas.findRegion("panel_wifi_nodeOn");
        nodeOffRegion = atlas.findRegion("panel_wifi_nodeOff");

        shapeRenderer = new ShapeRenderer();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        panelH = Gdx.graphics.getHeight() * PANEL_SCALE;
        panelW = panelH * (366f / 716f);
        panelX = Gdx.graphics.getWidth() / 2f - panelW / 2f + PANEL_OFFSET_X;
        panelY = Gdx.graphics.getHeight() / 2f - panelH / 2f + PANEL_OFFSET_Y;

        float railX = panelX + panelW * LEVER_REL_X;
        leverX    = railX - LEVER_W / 2f;
        leverMinY = panelY + panelH * LEVER_MIN_REL;
        leverMaxY = panelY + panelH * LEVER_MAX_REL;
        leverY    = leverMinY;
    }

    @Override
    protected void renderContent(float delta) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        handleLeverInput(mouseX, mouseY);
        updateProgress();

        // ── Panel de fondo ─────────────────────────────────────
        batch.draw(bgRegion, panelX, panelY, panelW, PANEL_H);



        // ── Rail de la palanca ─────────────────────────────────
        batch.end();
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(leverX + LEVER_W / 2f - 4f, leverMinY,
            8f, leverMaxY - leverMinY);
        shapeRenderer.end();
        batch.begin();

        // ── Palanca ───────────────────────────────────────────
        batch.draw(leverRegion, leverX, leverY, LEVER_W, LEVER_H);

        // ── Nodos de estado ───────────────────────────────────
        int nodesOn = Math.round(progress * NODE_COUNT);
        // Nodos
        float nodeStartX = panelX + panelW * NODE_START_REL_X;
        float nodeStartY = panelY + panelH * NODE_START_REL_Y;
        float nodeSpacingY = panelH * NODE_SPACING_REL;


        for (int i = 0; i < NODE_COUNT; i++) {
            TextureRegion nr = (i < nodesOn) ? nodeOnRegion : nodeOffRegion;
            batch.draw(nr,
                nodeStartX,
                nodeStartY - i * nodeSpacingY,
                NODE_SIZE, NODE_SIZE);
        }

        // ── Barra de progreso ─────────────────────────────────
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 1f);
        float barX = panelX + panelW * 0.35f;
        float barY = panelY + PANEL_H * 0.007f;
        float barW = panelW * 0.55f;
        float barH = PANEL_H * 0.05f;
        shapeRenderer.rect(barX, barY, barW, barH);

        float fillR = 1f - progress;
        float fillG = progress;
        shapeRenderer.setColor(fillR, fillG, 0.1f, 1f);
        shapeRenderer.rect(barX, barY, barW * progress, barH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, barW, barH);
        shapeRenderer.end();
        batch.begin();

        // ── Victoria ──────────────────────────────────────────
        if (progress >= 1f && !isCompleted) {
            sabotageManager.resolveSabotage();
            complete();
        }
    }

    private void handleLeverInput(float mouseX, float mouseY) {
        Rectangle leverRect = new Rectangle(leverX, leverY, LEVER_W, LEVER_H + 10f);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && !draggingLever) {
            if (leverRect.contains(mouseX, mouseY)) {
                draggingLever = true;
                dragOffY = mouseY - leverY;
            }
        }

        if (draggingLever) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                leverY = MathUtils.clamp(mouseY - dragOffY, leverMinY, leverMaxY);
            } else {
                draggingLever = false;
            }
        }
    }

    private void updateProgress() {
        progress = (leverY - leverMinY) / (leverMaxY - leverMinY);
        progress = MathUtils.clamp(progress, 0f, 1f);
    }

    @Override
    public void dispose() {
        atlas.dispose();
        shapeRenderer.dispose();
        super.dispose();
    }
}
