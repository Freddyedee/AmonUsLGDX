package com.amongus.core.impl.map;

import com.amongus.core.api.map.GameMap;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;



public class MaskCollisionMap implements GameMap {
    private final Pixmap collisionMask;
    private final Color tempColor;
    // Constantes con el tamaño que le diste al mapa en tu GameScreen
    private static final float WORLD_WIDTH = 5000f;
    private static final float WORLD_HEIGHT = 4600f;

    public MaskCollisionMap(String maskPath) {
        // Cargamos la máscara en la RAM, no en la tarjeta de video
        this.collisionMask = new Pixmap(Gdx.files.internal(maskPath));
        this.tempColor = new Color();
    }

    @Override
    public boolean canMove(Position from, Position to) {
        // El personaje mide 50x50. Crear una caja de colisión para los pies.
        // Asumiendo que to.x() y to.y() es la esquina inferior izquierda del sprite:

        int margenIzquierdo = to.x() + 15;
        int margenDerecho = to.x() + 30;
        int basePies = to.y() + 5;
        int topePies = to.y() + 20;

        // Verificamos si las 4 esquinas de la caja de los pies están en zona blanca
        return isWalkable(margenIzquierdo, basePies) &&
            isWalkable(margenDerecho, basePies) &&
            isWalkable(margenIzquierdo, topePies) &&
            isWalkable(margenDerecho, topePies);
    }

    private boolean isWalkable(int worldX, int worldY) {
        // 1. Traducimos la coordenada del mundo (0-5000) al píxel real de la imagen
        int pixelX = (int) ((worldX / WORLD_WIDTH) * collisionMask.getWidth());
        int pixelY = (int) ((worldY / WORLD_HEIGHT) * collisionMask.getHeight());

        // 2. Evitar que el jugador se salga de los límites de la imagen
        if (pixelX < 0 || pixelX >= collisionMask.getWidth() || pixelY < 0 || pixelY >= collisionMask.getHeight()) {
            return false; // Fuera del mapa es colisión
        }

        // 3. Invertimos la Y porque LibGDX dibuja hacia arriba y Pixmap lee hacia abajo.
        // Restamos 1 extra para evitar un error de "IndexOutOfBounds" en el borde superior.
        int invertedY = collisionMask.getHeight() - 1 - pixelY;

        // 4. Leemos el color del píxel ya escalado
        int pixelValue = collisionMask.getPixel(pixelX, invertedY);
        Color.rgba8888ToColor(tempColor, pixelValue);

        // 5. Verificamos si es pared (negro puro)
        if (tempColor.r < 0.1f && tempColor.g < 0.1f && tempColor.b < 0.1f) {
            return false;
        }

        return true;
    }

    public void dispose() {
        if (collisionMask != null) {
            collisionMask.dispose();
        }
    }
}
