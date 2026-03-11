package com.amongus.core.impl.map;

import com.amongus.core.api.map.GameMap;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MaskCollisionMap implements GameMap {
    private final com.badlogic.gdx.graphics.Pixmap collisionMask;
    private final com.badlogic.gdx.graphics.Color tempColor;
    private static final float WORLD_WIDTH  = 3840f;
    private static final float WORLD_HEIGHT = 2160f;

    private final List<List<Position>> ventNetworks = new ArrayList<>();

    public MaskCollisionMap(String maskPath) {
        this.collisionMask = new com.badlogic.gdx.graphics.Pixmap(Gdx.files.internal(maskPath));
        this.tempColor = new com.badlogic.gdx.graphics.Color();

        String pathName = maskPath.toLowerCase();

        // ── MAPA 1: Tus coordenadas exactas ──
        if (pathName.endsWith("mapacolisiones.png")) {
            ventNetworks.add(Arrays.asList(fromPaint(565, 57), fromPaint(1583, 1007)));
            ventNetworks.add(Arrays.asList(fromPaint(2300, 394), fromPaint(2132, 1016), fromPaint(3052, 1006)));
            ventNetworks.add(Arrays.asList(fromPaint(588, 1960), fromPaint(1850, 2066), fromPaint(2514, 2127)));

            System.out.println("[MAPA] Ventilaciones del Mapa 1 cargadas.");

            // ── MAPA 2: Tus coordenadas exactas ──
        } else if (pathName.endsWith("mapacolisiones2.png")) {
            ventNetworks.add(Arrays.asList(fromPaint(725, 21), fromPaint(1794, 232)));
            ventNetworks.add(Arrays.asList(fromPaint(46, 1043), fromPaint(1614, 1333), fromPaint(766, 1852)));
            ventNetworks.add(Arrays.asList(fromPaint(3544, 728), fromPaint(3240, 2099), fromPaint(2395, 2117)));

            System.out.println("[MAPA] Ventilaciones del Mapa 2 cargadas.");
        }
    }

    private Position fromPaint(float paintX, float paintY) {
        return new Position(paintX, WORLD_HEIGHT - paintY);
    }

    @Override
    public boolean canMove(Position from, Position to) {
        // ── BYPASS DE COLISIÓN PARA ALCANTARILLAS ──
        for (List<Position> network : ventNetworks) {
            for (Position vent : network) {
                if (Math.abs(vent.x() - to.x()) < 5.0f && Math.abs(vent.y() - to.y()) < 5.0f) {
                    return true;
                }
            }
        }

        // ── SISTEMA ANTI-TÚNEL DE ALTA PRECISIÓN ──
        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        float distance = (float) Math.hypot(dx, dy);

        if (distance == 0) return true;

        // Paso de 1 píxel. Precisión milimétrica o pixel a pixel
        float stepSize = 1f;
        int steps = (int) Math.ceil(distance / stepSize);

        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;

            float currentX = from.x() + dx * t;
            float currentY = from.y() + dy * t;

            int cX = (int) currentX;
            int cY = (int) currentY;

            // Hitbox de los pies dividida en 9 sensores (3x3)
            int margenIzquierdo = cX - 12;
            int centroX         = cX;
            int margenDerecho   = cX + 12;

            int basePies  = cY;
            int medioPies = cY + 6;
            int topePies  = cY + 12;

            // Comprobamos los 9 puntos. Si ALGUNO toca negro, bloqueamos.
            if (!(
                // Fila inferior
                isWalkable(margenIzquierdo, basePies) &&
                    isWalkable(centroX, basePies) &&
                    isWalkable(margenDerecho, basePies) &&

                    // Fila central (¡El punto ciego que te estaba fallando!)
                    isWalkable(margenIzquierdo, medioPies) &&
                    isWalkable(centroX, medioPies) &&
                    isWalkable(margenDerecho, medioPies) &&

                    // Fila superior
                    isWalkable(margenIzquierdo, topePies) &&
                    isWalkable(centroX, topePies) &&
                    isWalkable(margenDerecho, topePies)
            )) {
                return false;
            }
        }

        return true;
    }

    private boolean isWalkable(int worldX, int worldY) {
        int pixelX = (int) ((worldX / WORLD_WIDTH) * collisionMask.getWidth());
        int pixelY = (int) ((worldY / WORLD_HEIGHT) * collisionMask.getHeight());

        if (pixelX < 0 || pixelX >= collisionMask.getWidth() || pixelY < 0 || pixelY >= collisionMask.getHeight()) {
            return false;
        }

        int invertedY = collisionMask.getHeight() - 1 - pixelY;
        int pixelValue = collisionMask.getPixel(pixelX, invertedY);
        Color.rgba8888ToColor(tempColor, pixelValue);

        if (tempColor.r < 0.1f && tempColor.g < 0.1f && tempColor.b < 0.1f) {
            return false;
        }

        return true;
    }

    @Override
    public Position getNearestVent(Position pos, float maxDistance) {
        Position nearest = null;
        double minD = maxDistance;
        for (List<Position> network : ventNetworks) {
            for (Position vent : network) {
                double d = Math.hypot(pos.x() - vent.x(), pos.y() - vent.y());
                if (d < minD) { minD = d; nearest = vent; }
            }
        }
        return nearest;
    }

    @Override
    public Position getNextVentInNetwork(Position currentVent, int direction) {
        for (List<Position> network : ventNetworks) {
            for (int i = 0; i < network.size(); i++) {
                Position vent = network.get(i);
                if (Math.hypot(currentVent.x() - vent.x(), currentVent.y() - vent.y()) < 50.0) {
                    int nextIndex = (i + direction) % network.size();
                    if (nextIndex < 0) nextIndex += network.size();
                    return network.get(nextIndex);
                }
            }
        }
        return null;
    }

    public void dispose() {
        if (collisionMask != null) collisionMask.dispose();
    }
}
