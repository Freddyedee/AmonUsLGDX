package com.amongus.debug;

import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class DebugEndGame {
    private final Texture pixelOscuro;

    public DebugEndGame() {
        // Generamos un fondo gris semitransparente por código
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.1f, 0.1f, 0.1f, 0.8f));
        pixmap.fill();
        pixelOscuro = new Texture(pixmap);
        pixmap.dispose();
    }

    public void drawAndHandleInput(SpriteBatch batch, BitmapFont font, GameEngine engine) {
        float sh = Gdx.graphics.getHeight();

        float btnW = 240f;
        float btnH = 40f;
        float startX = 10f;
        float startY1 = sh - 50f; // Botón 1: Arriba a la izquierda
        float startY2 = sh - 100f; // Botón 2: Justo debajo

        // 1. Dibujar los fondos de los botones
        batch.setColor(1, 1, 1, 1);
        batch.draw(pixelOscuro, startX, startY1, btnW, btnH);
        batch.draw(pixelOscuro, startX, startY2, btnW, btnH);

        // 2. Dibujar los textos
        font.setColor(Color.RED);
        font.draw(batch, "[DEBUG] Gana Impostor", startX + 15, startY1 + 28);
        font.setColor(Color.CYAN);
        font.draw(batch, "[DEBUG] Gana Tripulacion", startX + 10, startY2 + 28);
        font.setColor(Color.WHITE); // Restauramos el color blanco por seguridad

        // 3. Detección de clics
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = sh - Gdx.input.getY(); // Convertimos la Y del mouse a coordenadas de LibGDX

            if (mx >= startX && mx <= startX + btnW) {
                if (my >= startY1 && my <= startY1 + btnH) {
                    engine.forceGameResult("IMPOSTOR");
                } else if (my >= startY2 && my <= startY2 + btnH) {
                    engine.forceGameResult("CREWMATE");
                }
            }
        }
    }

    public void dispose() {
        if (pixelOscuro != null) pixelOscuro.dispose();
    }
}
