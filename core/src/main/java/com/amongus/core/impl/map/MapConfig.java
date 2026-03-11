package com.amongus.core.impl.map;

/**
 * Registro de los mapas disponibles en el juego.
 * Añadir nuevos mapas aquí sin tocar otra lógica.
 */
public enum MapConfig {

    MAPA_1("mapas/mapa1.png", 5000f, 4600f, "Villa Asia - Sector A"),
    MAPA_2("mapas/mapa2.png", 5000f, 4600f, "Villa Asia - Sector B");

    // Ruta del PNG del mapa (usado tanto en GameScreen como en MinimapOverlay)
    public final String texturePath;

    // Dimensiones del mundo en unidades de juego
    // (debe coincidir con el batch.draw del mapa en GameScreen)
    public final float worldWidth;
    public final float worldHeight;

    // Nombre legible para UI
    public final String displayName;

    MapConfig(String texturePath, float worldWidth, float worldHeight, String displayName) {
        this.texturePath = texturePath;
        this.worldWidth  = worldWidth;
        this.worldHeight = worldHeight;
        this.displayName = displayName;
    }
}
