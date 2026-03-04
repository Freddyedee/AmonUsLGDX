package com.amongus.core.view;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/**
 * Dibuja flechas en los bordes de la pantalla apuntando hacia las tareas pendientes.
 *
 * Comportamiento:
 *  - Si la tarea está dentro del rango de interacción (150f) → flecha oculta
 *  - Lejos de la tarea  → flecha pequeña  (ARROW_SIZE_FAR  = 25px)
 *  - Cerca de la tarea  → flecha grande   (ARROW_SIZE_NEAR = 50px, igual al sprite)
 *  - La flecha se desplaza por el borde de pantalla apuntando siempre a la tarea
 *  - Las tareas completadas no muestran flecha
 */
public class TaskArrowRenderer implements Disposable {

    // ── Constantes ────────────────────────────────────────────
    private static final float HIDE_RANGE    = 150f;   // igual que interacción
    private static final float ARROW_SIZE_NEAR = 50f;  // igual al sprite del jugador
    private static final float ARROW_SIZE_FAR  = 25f;  // mitad cuando está lejos
    private static final float SCALE_DIST_MAX  = 800f; // distancia a partir de la que es mínimo
    private static final float BORDER_PADDING  = 40f;  // separación del borde de pantalla

    // ── La textura de la flecha apunta hacia la DERECHA en la imagen ──
    // Si tu PNG apunta a otra dirección, ajusta TEXTURE_OFFSET_ANGLE
    private static final float TEXTURE_OFFSET_ANGLE = 0f;

    private final Texture arrowTexture;

    public TaskArrowRenderer() {
        arrowTexture = new Texture("sprites/flechaTaskGuia.png");
    }

    /**
     * Dibuja todas las flechas para las tareas del snapshot.
     *
     * Debe llamarse con el batch en coordenadas de PANTALLA (setToOrtho2D),
     * ya que las flechas siempre se dibujan en la HUD, no en el mundo.
     *
     * @param batch       SpriteBatch ya comenzado con proyección de pantalla
     * @param snapshot    snapshot actual del juego
     * @param myPlayerId  id del jugador local
     * @param screenW     ancho de pantalla en píxeles
     * @param screenH     alto  de pantalla en píxeles
     * @param camera      cámara para convertir coordenadas mundo → pantalla
     */
    public void draw(SpriteBatch batch,
                     GameSnapshot snapshot,
                     com.amongus.core.api.player.PlayerId myPlayerId,
                     float screenW, float screenH,
                     com.badlogic.gdx.graphics.OrthographicCamera camera) {

        // Posición del jugador local en el mundo
        PlayerView me = snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(myPlayerId))
            .findFirst().orElse(null);
        if (me == null) return;

        float px = me.getPosition().x();
        float py = me.getPosition().y();

        for (TaskView tv : snapshot.getTasks()) {
            if (tv.isCompleted()) continue;   // tarea completada → sin flecha

            float tx = tv.getPosition().x();
            float ty = tv.getPosition().y();

            float dist = Vector2.dst(px, py, tx, ty);

            if (dist <= HIDE_RANGE) continue;  // tarea visible → sin flecha

            // ── Tamaño según distancia (interpolación lineal) ──────────
            float t    = MathUtils.clamp((dist - HIDE_RANGE) / (SCALE_DIST_MAX - HIDE_RANGE), 0f, 1f);
            float size = MathUtils.lerp(ARROW_SIZE_NEAR, ARROW_SIZE_FAR, t);

            // ── Ángulo de la flecha (mundo: jugador → tarea) ───────────
            float angleRad = MathUtils.atan2(ty - py, tx - px);
            float angleDeg = angleRad * MathUtils.radiansToDegrees;

            // ── Posición en el borde de pantalla ──────────────────────
            // Convertimos la posición de la tarea a coordenadas de pantalla
            // para saber en qué dirección está desde el centro
            com.badlogic.gdx.math.Vector3 taskScreen = new com.badlogic.gdx.math.Vector3(tx, ty, 0);
            camera.project(taskScreen);   // → coordenadas de pantalla

            float cx = screenW / 2f;
            float cy = screenH / 2f;

            // Dirección desde el centro de la pantalla hacia la tarea
            float dx = taskScreen.x - cx;
            float dy = taskScreen.y - cy;

            // Clampeamos esa dirección al borde de la pantalla con padding
            float innerW = screenW / 2f - BORDER_PADDING - size / 2f;
            float innerH = screenH / 2f - BORDER_PADDING - size / 2f;

            float arrowX, arrowY;

            if (Math.abs(dx) * innerH > Math.abs(dy) * innerW) {
                // Clamp en borde izquierdo/derecho
                float scale = innerW / Math.abs(dx);
                arrowX = cx + dx * scale;
                arrowY = cy + dy * scale;
            } else {
                // Clamp en borde superior/inferior
                float scale = innerH / Math.abs(dy);
                arrowX = cx + dx * scale;
                arrowY = cy + dy * scale;
            }

            // ── Dibujar ───────────────────────────────────────────────
            // SpriteBatch.draw(tex, x, y, originX, originY, w, h, scaleX, scaleY, rotation, ...)
            // x,y = esquina inferior izquierda → ajustamos con -size/2
            batch.draw(
                arrowTexture,
                arrowX - size / 2f,   // x esquina
                arrowY - size / 2f,   // y esquina
                size / 2f,            // originX (centro)
                size / 2f,            // originY (centro)
                size,                 // width
                size,                 // height
                1f, 1f,               // scaleX, scaleY
                angleDeg + TEXTURE_OFFSET_ANGLE,
                0, 0,                 // srcX, srcY
                arrowTexture.getWidth(), arrowTexture.getHeight(),
                false, false          // flipX, flipY
            );
        }
    }

    @Override
    public void dispose() {
        arrowTexture.dispose();
    }
}
