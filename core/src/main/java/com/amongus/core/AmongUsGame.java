package com.amongus.core;

import com.amongus.core.api.map.MapType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.model.Position;
import com.amongus.core.network.Server;
import com.badlogic.gdx.Game;
import com.amongus.core.view.screens.MainMenuScreen;
import com.amongus.core.utils.SettingsManager; // Importamos el gestor

public class AmongUsGame extends Game {
    private Server serverInstancia; // Atributo para guardar el servidor

    @Override
    public void create() {
        // 1. Cargar el XML y aplicar la resolución guardada al iniciar el juego
        SettingsManager.load();
        SettingsManager.applyResolution();

        // 2. El juego arranca limpio en el menú principal
        this.setScreen(new MainMenuScreen(this));
    }

    public void startNetworkGame(boolean isHost, String playerName, String ipHost, MapType selectedMap) {
        GameEngine engine = new GameEngine(selectedMap);

        // 1. Obtenemos tu color preferido
        SkinColor myColor = SkinColor.valueOf(SettingsManager.playerColor);

        // 2. Creamos NUESTRO jugador con ese color
        PlayerId myPlayerId = engine.spawnPlayer(playerName, myColor);
        engine.assignRole(myPlayerId, Role.CREWMATE);
        engine.movePlayer(myPlayerId, new Position(1920f, 1080f));

        // 3. Conectamos a la red (Añade el parámetro myColor al constructor)
        GameClient clienteRed = new GameClient(engine, isHost, myPlayerId, playerName, myColor);

        if (isHost) {
            // 1. Limpieza preventiva
            if (serverInstancia != null) {
                serverInstancia.detener();
            }

            System.out.println("[SISTEMA] Iniciando servidor local...");
            serverInstancia = new Server();
            serverInstancia.setDaemon(true);
            serverInstancia.start();

            // Como el socket se inicia en el constructor de Server, ya podemos conectar de inmediato
            clienteRed.conectar("localhost", 5000);
        } else {
            System.out.println("[SISTEMA] Conectando al host en IP: " + ipHost);
            clienteRed.conectar(ipHost, 5000);
        }

        // Pasamos el isHost a GameScreen
        this.setScreen(new GameScreen(engine, clienteRed, isHost));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
