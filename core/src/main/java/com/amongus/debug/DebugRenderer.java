package com.amongus.debug;

import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class DebugRenderer {
    private final Texture whitePixel;
    private final ShapeDrawer shapeDrawer;

    public DebugRenderer(SpriteBatch batch) {
        // Generamos un píxel blanco puro por código para que ShapeDrawer funcione
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        TextureRegion region = new TextureRegion(whitePixel);
        shapeDrawer = new ShapeDrawer(batch, region);
    }

    public void drawHitboxes(GameSnapshot snapshot) {
        // Usamos un color rojo con 50% de transparencia (Alpha = 0.5f)
        shapeDrawer.setColor(1f, 0f, 0f, 0.5f);

        for (PlayerView pv : snapshot.getPlayers()) {
            if (!pv.isAlive() || pv.isVenting()) continue;

            float x = pv.getPosition().x();
            float y = pv.getPosition().y();

            // ── MATEMÁTICAS DE TU MASKCOLLISIONMAP ──
            // Izquierda: x - 14 | Derecha: x + 14  --> Ancho total = 28
            // Base: y + 2       | Tope: y + 12     --> Alto total = 10

            float boxX = x - 12f;
            float boxY = y;
            float boxWidth = 24f;
            float boxHeight = 12f;

            // Dibujamos el rectángulo de colisión real
            shapeDrawer.filledRectangle(boxX, boxY, boxWidth, boxHeight);

            // Extra: Dibujamos un puntito verde en el centro exacto (x, y)
            // Esto te ayudará a ver desde dónde está calculando todo
            shapeDrawer.setColor(Color.GREEN);
            shapeDrawer.filledRectangle(x - 1, y - 1, 2, 2);

            // Restauramos el color rojo para el siguiente jugador
            shapeDrawer.setColor(1f, 0f, 0f, 0.5f);
        }
    }

    public void dispose() {
        if (whitePixel != null) {
            whitePixel.dispose();
        }
    }
}
