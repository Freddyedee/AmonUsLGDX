package com.amongus.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;

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

public class AmongUsGame extends ApplicationAdapter {

        @Override
        public void create() {
            // En el siguiente paso:
            // - Se instancia GameEngine
            // - Se configura GameSession
        }

        @Override
        public void render() {
            // Por ahora vacío
            // Luego: input + render
        }

        @Override
        public void dispose() {
            // Liberación de recursos gráficos
        }
    }


