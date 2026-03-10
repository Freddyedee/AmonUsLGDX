package com.amongus.core.view;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.SkinColor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public final class PlayerRenderer implements Disposable {

    // Guardamos un arreglo de 5 texturas por cada color disponible
    private final Map<SkinColor, Texture[]> colorAnimations = new HashMap<>();

    private final Map<PlayerId, Float> timers = new HashMap<>();
    private final Map<PlayerId, Integer> frames = new HashMap<>();

    public PlayerRenderer() {
        loadTextures();
    }

    private void loadTextures() {
        for (SkinColor color : SkinColor.values()) {
            Texture[] anim = new Texture[5];
            String prefix = color.getPrefix();

            // Asegúrate de que la ruta coincida con tu carpeta de assets
            anim[0] = new Texture("sprites/player/body/" + prefix + "_idle.png");
            anim[1] = new Texture("sprites/player/body/" + prefix + "_01.png");
            anim[2] = new Texture("sprites/player/body/" + prefix + "_02.png");
            anim[3] = new Texture("sprites/player/body/" + prefix + "_03.png");
            anim[4] = new Texture("sprites/player/body/" + prefix + "_04.png");

            colorAnimations.put(color, anim);
        }
    }

    public void draw(SpriteBatch batch, float x, float y, PlayerId id, int dir, boolean moving, boolean isAlive, SkinColor skinColor) {
        Texture[] anim = colorAnimations.get(skinColor);
        if (anim == null) return; // Por seguridad

        Texture currentFrame;

        // --- LÓGICA DE ANIMACIÓN ---
        if (moving && isAlive) {
            float timer = timers.getOrDefault(id, 0f) + 0.15f;
            int frame = frames.getOrDefault(id, 1);

            if (timer > 1f) {
                frame++;
                timer = 0;
            }
            if (frame < 1 || frame > 4) frame = 1;

            timers.put(id, timer);
            frames.put(id, frame);
            currentFrame = anim[frame];
        } else {
            timers.put(id, 0f);
            frames.put(id, 0);
            currentFrame = anim[0]; // Frame estático (idle)
        }

        // --- LÓGICA DE DIBUJADO Y ALINEACIÓN ---
        if (currentFrame != null) {
            // Usamos el tamaño real de tu imagen recortada
            float frameWidth = currentFrame.getWidth();
            float frameHeight = currentFrame.getHeight();

            // Mantenemos los offsets para que las colisiones sigan perfectas
            float offsetY = -6f;
            float offsetX = 4f;

            float baseDrawX = x - (frameWidth / 2f);
            float finalDrawX = baseDrawX + (dir == 1 ? -offsetX : offsetX);
            float finalDrawY = y + offsetY;

            int renderX = (int) finalDrawX;
            int renderY = (int) finalDrawY;

            // Determinar si hay que voltear horizontalmente
            boolean flipX = (dir == -1);

            if (!isAlive) {
                batch.setColor(Color.LIGHT_GRAY);
                float originX = frameWidth / 2f;
                float originY = frameHeight / 2f;

                // Rotamos -90 grados (muerto) usando el método extendido de batch.draw
                batch.draw(currentFrame, renderX, renderY, originX, originY, frameWidth, frameHeight, 1, 1, -90, 0, 0, (int)frameWidth, (int)frameHeight, flipX, false);
                batch.setColor(Color.WHITE);
            } else {
                // Dibujado normal (vivo), el booleano 'flipX' hace la magia de voltear
                batch.draw(currentFrame, renderX, renderY, frameWidth / 2f, frameHeight / 2f, frameWidth, frameHeight, 1, 1, 0, 0, 0, (int)frameWidth, (int)frameHeight, flipX, false);
            }
        }
    }

    @Override
    public void dispose() {
        // Limpiamos todas las texturas de la memoria
        for (Texture[] anim : colorAnimations.values()) {
            for (Texture tex : anim) {
                if (tex != null) tex.dispose();
            }
        }
    }
}
