package com.amongus.infrastructure;

public class Prueba {
}

    /*Qué hace:

    Renderizado
    Animaciones
    Pixel art
    Mapas

    Consume GameSnapshot

    No conoce reglas
    No toca core

    Se encargaria de carpetas como UI, que contiene el libgdx con la screen rende inputs assets.

        sprites
        animaciones
        mapas
        cámara
        escalado

    Solo consume (deberia) GameSnapshot y no deberia definir logica del juego, puede tener carpetas como libgdx
    la cual se podria ecargar de screen/ ; render/ ; input/ ; assets/

    */
