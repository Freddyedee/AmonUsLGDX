package com.amongus.core.impl.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minijuego: Conectar Cables (Wires).
 *
 * Mecánica:
 *  - 3 cables (rojo, azul, amarillo) en el lado izquierdo en orden aleatorio
 *  - 3 conectores en el lado derecho en orden aleatorio
 *  - El jugador hace drag desde el inicio del cable (izquierda) hasta el conector correcto (derecha)
 *  - La conexión es válida solo si los colores coinciden
 *  - Al conectar los 3 correctamente, la tarea se completa
 *
 * Sprites del atlas:
 *  - electricity_wiresBaseBack  → panel de fondo
 *  - electricity_wiresBase*     → conectores de los extremos
 */
public class WiresMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas ─────────────────────────────────────────────────
    private TextureAtlas atlas;
    private TextureRegion panelBg;
    private TextureRegion connectorRegion;

    // ── ShapeRenderer para dibujar cables ─────────────────────
    private ShapeRenderer shapeRenderer;

    // ── Colores de los cables ─────────────────────────────────
    private static final Color[] WIRE_COLORS = {
        new Color(0.9f, 0.15f, 0.15f, 1f),   // Rojo
        new Color(0.2f, 0.4f, 0.9f,  1f),    // Azul
        new Color(0.9f, 0.8f, 0.1f,  1f)     // Amarillo
    };
    private static final String[] COLOR_NAMES = {"ROJO", "AZUL", "AMARILLO"};
    private static final int NUM_WIRES = 3;

    // ── Layout ────────────────────────────────────────────────
    private float panelX, panelY, panelW, panelH;

    // Puntos de inicio (izquierda) y fin (derecha)
    private final Vector2[] leftPoints  = new Vector2[NUM_WIRES];
    private final Vector2[] rightPoints = new Vector2[NUM_WIRES];

    // leftOrder[i] = índice de color en la posición i del lado izquierdo
    private final int[] leftOrder  = {0, 1, 2};
    // rightOrder[i] = índice de color en la posición i del lado derecho
    private final int[] rightOrder = {0, 1, 2};

    // Radio del conector clickeable
    private static final float CONNECTOR_RADIUS = 22f;
    private static final float WIRE_THICKNESS   = 6f;

    // ── Estado drag ───────────────────────────────────────────
    private int  draggingFrom = -1;   // índice en leftOrder que se está arrastrando (-1 = ninguno)
    private float mouseX, mouseY;

    // ── Conexiones realizadas ─────────────────────────────────
    // connections[i] = j significa que el cable i (leftOrder[i]) está conectado al conector j (rightOrder[j])
    // -1 = sin conectar
    private final int[] connections = {-1, -1, -1};

    public WiresMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();

        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/wires/wires.atlas"));
        panelBg         = atlas.findRegion("electricity_wiresBaseBack");
        connectorRegion = atlas.findRegion("electricity_wiresBase1");

        shapeRenderer = new ShapeRenderer();

        // ── Calcular layout centrado ──────────────────────────
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        panelH = sh * 0.70f;
        panelW = panelH;                          // el panel es cuadrado
        panelX = sw / 2f - panelW / 2f;
        panelY = sh / 2f - panelH / 2f;

        // Aleatorizar orden de ambos lados
        List<Integer> leftList  = new ArrayList<>(Arrays.asList(0, 1, 2));
        List<Integer> rightList = new ArrayList<>(Arrays.asList(0, 1, 2));
        Collections.shuffle(leftList);
        Collections.shuffle(rightList);
        for (int i = 0; i < NUM_WIRES; i++) {
            leftOrder[i]  = leftList.get(i);
            rightOrder[i] = rightList.get(i);
        }

        // Posiciones de los conectores
        float margin   = panelH * 0.15f;
        float spacing  = (panelH - margin * 2f) / (NUM_WIRES - 1);

        for (int i = 0; i < NUM_WIRES; i++) {
            float y = panelY + panelH - margin - i * spacing;
            leftPoints[i]  = new Vector2(panelX + panelW * 0.08f, y);
            rightPoints[i] = new Vector2(panelX + panelW * 0.92f, y);
        }
    }

    @Override
    protected void renderContent(float delta) {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        updateInput();

        // ── 1. Dibujar panel de fondo ─────────────────────────
        batch.draw(panelBg, panelX, panelY, panelW, panelH);

        // ── 2. Cables ya conectados ───────────────────────────
        // (dibujar antes de cerrar batch para que ShapeRenderer vaya después)
        batch.end();

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // Cables conectados
        for (int i = 0; i < NUM_WIRES; i++) {
            if (connections[i] != -1) {
                int j = connections[i];
                Color c = WIRE_COLORS[leftOrder[i]];
                drawWire(leftPoints[i].x, leftPoints[i].y,
                    rightPoints[j].x, rightPoints[j].y, c, WIRE_THICKNESS);
            }
        }

        // Cable en arrastre actual
        if (draggingFrom != -1) {
            Color c = WIRE_COLORS[leftOrder[draggingFrom]];
            drawWire(leftPoints[draggingFrom].x, leftPoints[draggingFrom].y,
                mouseX, mouseY, c, WIRE_THICKNESS);
        }

        // ── 3. Conectores izquierda ───────────────────────────
        for (int i = 0; i < NUM_WIRES; i++) {
            Color c = WIRE_COLORS[leftOrder[i]];
            drawConnector(leftPoints[i].x, leftPoints[i].y, c,
                draggingFrom == i);   // highlight si se está arrastrando
        }

        // ── 4. Conectores derecha ─────────────────────────────
        for (int i = 0; i < NUM_WIRES; i++) {
            Color c = WIRE_COLORS[rightOrder[i]];
            boolean connected = isRightConnected(i);
            drawConnector(rightPoints[i].x, rightPoints[i].y, c, connected);
        }

        batch.begin();

        // ── 5. Instrucción ────────────────────────────────────
        // (opcional: añadir BitmapFont si quieres texto)
    }

    // ── Input ─────────────────────────────────────────────────

    private void updateInput() {
        mouseX = Gdx.input.getX();
        mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // invertir Y de LibGDX

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Intentar iniciar drag desde un conector izquierdo
            for (int i = 0; i < NUM_WIRES; i++) {
                if (dst(mouseX, mouseY, leftPoints[i].x, leftPoints[i].y) <= CONNECTOR_RADIUS) {
                    draggingFrom = i;
                    connections[i] = -1;   // desconectar si ya tenía conexión
                    break;
                }
            }
        }

        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (draggingFrom != -1) {
                // Soltar — comprobar si cayó sobre un conector derecho
                for (int j = 0; j < NUM_WIRES; j++) {
                    if (dst(mouseX, mouseY, rightPoints[j].x, rightPoints[j].y) <= CONNECTOR_RADIUS) {
                        // Validar color
                        if (leftOrder[draggingFrom] == rightOrder[j]) {
                            // Desconectar cualquier cable previo en ese conector derecho
                            for (int k = 0; k < NUM_WIRES; k++) {
                                if (connections[k] == j) connections[k] = -1;
                            }
                            connections[draggingFrom] = j;
                            checkCompletion();
                        }
                        break;
                    }
                }
                draggingFrom = -1;
            }
        }
    }

    // ── Helpers de dibujo ─────────────────────────────────────

    /**
     * Dibuja un cable como rectángulo rotado entre dos puntos.
     */
    private void drawWire(float x1, float y1, float x2, float y2,
                          Color color, float thickness) {
        float dx     = x2 - x1;
        float dy     = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle  = (float) Math.toDegrees(Math.atan2(dy, dx));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        // Dibujamos un rectángulo centrado en (x1,y1) con longitud y rotación
        shapeRenderer.rectLine(x1, y1, x2, y2, thickness);
        shapeRenderer.end();
    }

    /**
     * Dibuja un conector circular con borde.
     */
    private void drawConnector(float x, float y, Color color, boolean highlight) {
        // Relleno
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(x, y, CONNECTOR_RADIUS);
        shapeRenderer.end();

        // Borde
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(highlight ? Color.WHITE : Color.DARK_GRAY);
        shapeRenderer.circle(x, y, CONNECTOR_RADIUS);
        shapeRenderer.end();
    }

    private boolean isRightConnected(int rightIndex) {
        for (int c : connections) if (c == rightIndex) return true;
        return false;
    }

    private float dst(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void checkCompletion() {
        for (int c : connections) {
            if (c == -1) return;  // aún hay cables sin conectar
        }
        complete();   // ¡todos conectados correctamente!
    }

    @Override
    public void dispose() {
        atlas.dispose();
        shapeRenderer.dispose();
        super.dispose();
    }
}
