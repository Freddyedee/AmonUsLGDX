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

/**
 * Minijuego: Destapar el Toilet.
 *
 * Mecánica:
 *  - Mash SPACE para llenar la barra
 *  - Cada press alterna entre toiletact1 y toiletact2
 *  - Al llegar al 100%: toiletFinished1 (0.5s) → toiletFinished2 (0.5s) → completa
 *  - La barra drena lentamente si no presionas
 *  - spaceButton se muestra debajo de la barra
 */
public class ToiletMinigameScreen extends AbstractMinigameScreen {

    // ── Atlas ─────────────────────────────────────────────────
    private TextureAtlas atlas;
    private TextureRegion act1Region;
    private TextureRegion act2Region;
    private TextureRegion finished1Region;
    private TextureRegion finished2Region;
    private TextureRegion spaceButtonRegion;

    // ── Estado de animación ───────────────────────────────────
    private boolean showAct2    = false;   // alterna con cada press
    private boolean finished    = false;   // barra llena
    private boolean showFinish2 = false;   // segunda fase del final
    private float   finishTimer = 0f;

    private static final float FINISH1_DURATION = 0.5f;
    private static final float FINISH2_DURATION = 0.5f;

    // ── Barra de progreso ─────────────────────────────────────
    private float fillProgress = 0f;
    private static final float FILL_PER_PRESS = 0.12f;  // cuánto sube por cada mash
    private static final float DRAIN_SPEED    = 0.08f;  // cuánto baja por segundo

    // ── Input: detectar solo el "just pressed" del space ─────
    private boolean spaceWasPressed = false;

    // ── ShapeRenderer para la barra ───────────────────────────
    private ShapeRenderer shapeRenderer;

    // ── Layout ────────────────────────────────────────────────
    // Toilet centrado; todos los sprites al mismo tamaño forzado
    private static final float TOILET_W = 280f;
    private static final float TOILET_H = 320f;
    private float toiletX, toiletY;

    // Barra a la derecha del toilet
    private static final float BAR_W    = 40f;
    private static final float BAR_H    = 260f;
    private static final float BAR_GAP  = 30f;   // espacio entre toilet y barra
    private float barX, barY;

    // Botón espacio debajo de la barra
    private static final float BTN_W = 150f;
    private static final float BTN_H = 86f * (150f / 150f); // proporcional
    private float btnX, btnY;

    public ToiletMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();
        atlas = new TextureAtlas(Gdx.files.internal("minijuegos/toilet/toilet.atlas"));

        act1Region      = atlas.findRegion("toiletact1-removebg-preview");
        act2Region      = atlas.findRegion("toiletact2-removebg-preview");
        finished1Region = atlas.findRegion("toiletFinished1-removebg-preview");
        finished2Region = atlas.findRegion("toiletFinished2-removebg-preview");
        spaceButtonRegion = atlas.findRegion("spaceButton");

        shapeRenderer = new ShapeRenderer();

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Toilet centrado ligeramente a la izquierda para dejar espacio a la barra
        float totalW = TOILET_W + BAR_GAP + BAR_W;
        toiletX = sw / 2f - totalW / 2f;
        toiletY = sh / 2f - TOILET_H / 2f;

        // Barra a la derecha del toilet, alineada verticalmente al centro
        barX = toiletX + TOILET_W + BAR_GAP;
        barY = toiletY + TOILET_H / 2f - BAR_H / 2f;

        // Botón espacio centrado debajo de la barra
        float btnScale = BAR_W / BTN_W * 3.5f;   // escalar para que sea visible
        btnX = barX + BAR_W / 2f - (BTN_W * btnScale) / 2f;
        btnY = barY - 20f - BTN_H * btnScale;
    }

    @Override
    protected void renderContent(float delta) {
        // ── Actualizar lógica ──────────────────────────────────
        if (finished) {
            updateFinishSequence(delta);
        } else {
            updateMash(delta);
        }

        // ── Dibujar toilet ─────────────────────────────────────
        TextureRegion toiletToDraw;
        if (finished) {
            toiletToDraw = showFinish2 ? finished2Region : finished1Region;
        } else {
            toiletToDraw = showAct2 ? act2Region : act1Region;
        }
        // Forzar mismo tamaño para todos los sprites
        batch.draw(toiletToDraw, toiletX, toiletY, TOILET_W, TOILET_H);

        // ── Dibujar barra (ShapeRenderer) ──────────────────────
        batch.end();
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // Fondo de la barra (gris oscuro)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 1f);
        shapeRenderer.rect(barX, barY, BAR_W, BAR_H);
        shapeRenderer.end();

        // Relleno (gradiente simple: verde→amarillo→rojo según progreso)
        float r = Math.min(1f, 2f * fillProgress);
        float g = Math.min(1f, 2f * (1f - fillProgress));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(r, g, 0.1f, 1f);
        shapeRenderer.rect(barX, barY, BAR_W, BAR_H * fillProgress);
        shapeRenderer.end();

        // Borde de la barra
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, BAR_W, BAR_H);
        shapeRenderer.end();

        batch.begin();

        // ── Dibujar botón espacio ──────────────────────────────
        float btnScale = 1.0f;
        float bW = 150f * btnScale * (BAR_W / 150f);
        float bH = 86f  * btnScale * (BAR_W / 150f);
        float bX = barX + BAR_W / 2f - bW / 2f;
        float bY = barY - 15f - bH;
        batch.draw(spaceButtonRegion, bX, bY, bW, bH);
    }

    // ── Lógica de mash ────────────────────────────────────────

    private void updateMash(float delta) {
        boolean spaceNow = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        // Detectar "just pressed" manualmente
        if (spaceNow && !spaceWasPressed) {
            fillProgress = Math.min(1f, fillProgress + FILL_PER_PRESS);
            showAct2     = !showAct2;   // alternar sprite

            if (fillProgress >= 1f) {
                finished    = true;
                finishTimer = 0f;
                showFinish2 = false;
                System.out.println("[Toilet] ¡Barra llena! Iniciando secuencia final.");
            }
        }
        spaceWasPressed = spaceNow;

        // Drenar si no se presiona
        if (!spaceNow) {
            fillProgress = Math.max(0f, fillProgress - DRAIN_SPEED * delta);
        }
    }

    private void updateFinishSequence(float delta) {
        finishTimer += delta;

        if (!showFinish2 && finishTimer >= FINISH1_DURATION) {
            showFinish2 = true;
            finishTimer = 0f;
            System.out.println("[Toilet] Mostrando toiletFinished2.");
        }

        if (showFinish2 && finishTimer >= FINISH2_DURATION && !isCompleted) {
            System.out.println("[Toilet] Completado.");
            complete();
        }
    }

    @Override
    public void dispose() {
        atlas.dispose();
        shapeRenderer.dispose();
        super.dispose();
    }
}
