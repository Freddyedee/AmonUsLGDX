package com.amongus.core;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Game;
import com.amongus.core.impl.engine.GameEngine;

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

    private GameEngine engine;

    @Override
    public void create() {
        this.engine = new GameEngine();                  // 1. primero el engine

        PlayerId myPlayerId = engine.spawnPlayer("Local Player");
        PlayerId testPlayer = engine.spawnPlayer("test Player");

        engine.assignRole(testPlayer, Role.IMPOSTOR);
        engine.assignRole(myPlayerId, Role.CREWMATE);

        engine.startGame();

        engine.movePlayer(myPlayerId, new Position(500, 500));
        engine.movePlayer(testPlayer, new Position(350, 350));

        FirstScreen firstScreen = new FirstScreen(engine); // 2. luego la pantalla
        setScreen(firstScreen);
        engine.setMainScreen(firstScreen);
    }
    @Override
    public void dispose(){
        super.dispose();
    }

}


