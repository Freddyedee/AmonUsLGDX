package com.amongus.core;

import com.badlogic.gdx.Game;

/**
 * Punto de entrada del juego para LibGDX.
 *
 * Esta clase NO contiene lógica del juego.
 * Su única responsabilidad es:
 *  - Inicializar el motor del juego (GameEngine)
 *  - Delegar el ciclo de vida a LibGDX
 *
 * La lógica vive exclusivamente en core/impl.
 */

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class AmongUsGame extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}


