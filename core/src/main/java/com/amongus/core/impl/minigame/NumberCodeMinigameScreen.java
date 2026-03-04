package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Minijuego: Pulsar números del 1 al 10 en orden ascendente.
 *
 * Sprites del atlas basicNumber:
 *  - reactorPanel        → pantalla azul de fondo (izquierda)
 *  - reactorButton01-10  → botones individuales (82x82 px)
 *
 * Estados de cada botón:
 *  - Normal   → tinte blanco (color original del sprite)
 *  - Correcto → tinte verde
 *  - Error    → tinte rojo por un instante, luego reset a blanco
 */
public class NumberCodeMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas ─────────────────────────────────────────────────
    private TextureAtlas atlas;
    private TextureRegion reactorPanel;
    private TextureRegion[] buttonRegions; // índice 0 = botón "1", índice 9 = botón "10"

    // ── Estado ────────────────────────────────────────────────
    private int   currentExpected = 1;          // próximo número a pulsar
    private Color[] buttonColors;               // color actual de cada botón
    private float errorTimer = 0f;              // tiempo mostrando error (rojo)
    private static final float ERROR_DURATION = 0.4f;
    private boolean showingError = false;

    // ── Layout (calculado en show relativo al panel base) ─────
    private float[] btnX, btnY;
    private float btnSize;

    // ── Orden visual de los botones en la cuadrícula ──────────
    // El atlas tiene: 04,01,03,05,02 / 07,09,06,08,10
    // Queremos mostrarlos mezclados visualmente (como Among Us)
    // pero el número lógico es el índice+1
    private static final int[] VISUAL_ORDER = {4, 1, 3, 5, 2, 7, 9, 6, 8, 10};

    public NumberCodeMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show(); // carga taskBackground, calcula panelX/Y/W/H

        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/basicNumber/basicNumber.atlas"));
        reactorPanel = atlas.findRegion("reactorPanel");

        // Cargar botones en el orden visual definido
        buttonRegions = new TextureRegion[10];
        buttonColors  = new Color[10];
        for (int i = 0; i < 10; i++) {
            String name = String.format("reactorButton%02d", VISUAL_ORDER[i]);
            buttonRegions[i] = atlas.findRegion(name);
            buttonColors[i]  = new Color(Color.WHITE);
        }

        calculateLayout();
    }

    private void calculateLayout() {
        // ── El panel interior se divide en dos zonas ──────────
        // Izquierda: reactorPanel (pantalla azul) ~40% del ancho
        // Derecha:   cuadrícula 5x2 de botones    ~55% del ancho

        float innerPad = panelW * 0.04f;  // padding interior del taskBackground

        // Zona de botones: 5 columnas, 2 filas, sin apenas separación
        float gridW = panelW * 0.52f;
        float gridH = panelH * 0.72f;

        float gridX = panelX + panelW * 0.24f;  // empieza después del panel azul
        float gridY = panelY + (panelH - gridH) / 0.75f;

        // Tamaño de cada botón (casi sin padding entre ellos)
        float gap  = 2f;
        btnSize = Math.min((gridW - gap * 4) / 5f, (gridH - gap) / 2f);

        btnX = new float[10];
        btnY = new float[10];

        for (int i = 0; i < 10; i++) {
            int col = i % 5;
            int row = 1 - (i / 5);  // row 1 = arriba, row 0 = abajo
            btnX[i] = gridX + col * (btnSize + gap);
            btnY[i] = gridY + row * (btnSize + gap);
        }
    }

    @Override
    protected void renderContent(float delta) {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // ── Actualizar timer de error ─────────────────────────
        if (showingError) {
            errorTimer -= delta;
            if (errorTimer <= 0) {
                resetButtons();
                showingError = false;
            }
        }

        // ── Dibujar panel azul (reactorPanel) ─────────────────
        float rPanelW = panelW * 0.80f;
        float rPanelH = panelH * 0.38f;
        float rPanelX = panelX + panelW * 0.10f;
        float rPanelY = panelY + (panelH - rPanelH) / 2f;
        batch.draw(reactorPanel, rPanelX, rPanelY, rPanelW, rPanelH);

        // ── Dibujar botones ───────────────────────────────────
        for (int i = 0; i < 10; i++) {
            batch.setColor(buttonColors[i]);
            batch.draw(buttonRegions[i], btnX[i], btnY[i], btnSize, btnSize);
        }
        batch.setColor(Color.WHITE);

        // ── Detectar clic en botones ──────────────────────────
        if (!showingError && Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = sh - Gdx.input.getY();  // LibGDX Y invertido

            for (int i = 0; i < 10; i++) {
                if (mx >= btnX[i] && mx <= btnX[i] + btnSize
                    && my >= btnY[i] && my <= btnY[i] + btnSize) {
                    onButtonClicked(i);
                    break;
                }
            }
        }

        // ── Tecla numérica como alternativa al clic ───────────
        if (!showingError) {
            for (int n = 1; n <= 10; n++) {
                boolean pressed = (n < 10)
                    ? Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + n)
                    : Gdx.input.isKeyJustPressed(Input.Keys.NUM_0);
                if (pressed) {
                    // Buscar el botón que corresponde al número n
                    for (int i = 0; i < 10; i++) {
                        if (VISUAL_ORDER[i] == n) {
                            onButtonClicked(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void onButtonClicked(int buttonIndex) {
        int clickedNumber = VISUAL_ORDER[buttonIndex];

        if (clickedNumber == currentExpected) {
            // ── Acierto: poner verde ──────────────────────────
            buttonColors[buttonIndex] = new Color(0.3f, 1f, 0.3f, 1f);
            currentExpected++;

            if (currentExpected > 10) {
                complete();
            }
        } else {
            // ── Error: poner todos en rojo y activar timer ────
            for (int i = 0; i < 10; i++) {
                buttonColors[i] = new Color(1f, 0.25f, 0.25f, 1f);
            }
            showingError = true;
            errorTimer   = ERROR_DURATION;
        }
    }

    private void resetButtons() {
        currentExpected = 1;
        for (int i = 0; i < 10; i++) {
            buttonColors[i] = new Color(Color.WHITE);
        }
    }

    @Override
    public void dispose() {
        atlas.dispose();
        super.dispose();
    }
}
