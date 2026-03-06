package com.amongus.core.api.map;

public enum MapType {
    // Definimos los mapas y sus rutas en la carpeta assets
    MAPA_1("mapas/mapa1.png", "mapas/mapaColisiones.png"),
    MAPA_2("mapas/mapa2.png", "mapas/mapaColisiones2.png");

    private final String visualPath;
    private final String collisionPath;

    MapType(String visualPath, String collisionPath) {
        this.visualPath = visualPath;
        this.collisionPath = collisionPath;
    }

    public String getVisualPath() { return visualPath; }
    public String getCollisionPath() { return collisionPath; }
}
