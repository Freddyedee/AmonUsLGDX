package com.amongus.core;

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

import com.amongus.core.api.map.MapType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.network.Server;
import com.badlogic.gdx.Game;
import com.amongus.core.view.screens.MainMenuScreen;

public class AmongUsGame extends Game {

    @Override
    public void create() {
        // El juego SIEMPRE arranca limpio en el menú principal
        this.setScreen(new MainMenuScreen(this));
    }

    // Este es el metodo que recibe los datos de PlayMenuScreen
    public void startNetworkGame(boolean isHost, String playerName, String ipHost, MapType selectedMap) {
        GameEngine engine = new GameEngine(selectedMap);

        // 1. Creamos NUESTRO jugador con un ID aleatorio y real
        PlayerId myPlayerId = engine.spawnPlayer(playerName);

        if (isHost) {
            engine.assignRole(myPlayerId, com.amongus.core.api.player.Role.IMPOSTOR);
        } else {
            engine.assignRole(myPlayerId, com.amongus.core.api.player.Role.CREWMATE);
        }

        // Iniciamos el juego local
        engine.startGame();

        // Posición inicial base
        engine.movePlayer(myPlayerId, new com.amongus.core.model.Position(500, 500));

        // 2. Conectamos a la red (el cliente se encargará de avisar que llegamos)
        GameClient clienteRed = new GameClient(engine, isHost, myPlayerId, playerName);

        if (isHost) {
            System.out.println("[SISTEMA] Iniciando servidor local...");
            Server server = new Server();
            server.setDaemon(true); // Muere cuando se cierra el juego
            server.start();

            // Le damos 200 milisegundos al servidor para que abra el puerto
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            clienteRed.conectar("localhost", 5000);
        } else {
            System.out.println("[SISTEMA] Conectando al host en IP: " + ipHost);
            clienteRed.conectar(ipHost, 5000);
        }

        this.setScreen(new GameScreen(engine, clienteRed));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}


