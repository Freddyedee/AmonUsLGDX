package com.amongus.core;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Game;

/**
 * Punto de entrada del juego para LibGDX.
 *
 * <p>Responsabilidad única: inicializar el {@link GameEngine}, registrar
 * jugadores y delegar el ciclo de vida a {@link GameScreen}.
 *
 * <p>No contiene lógica de juego. Toda la lógica vive en {@code core/impl}.
 *
 * <p>TODO: reemplazar la configuración manual por una pantalla de lobby.
 * El enunciado exige mínimo 5 jugadores para iniciar partida y asignación
 * aleatoria de roles.
 */
public class AmongUsGame extends Game {

    private GameEngine engine;

    @Override
    public void create() {
        engine = new GameEngine();

        // ── Registro de jugadores (temporal — reemplazar con pantalla de lobby) ──
        PlayerId localPlayer = engine.spawnPlayer("Local Player");
        PlayerId jugador2    = engine.spawnPlayer("Jugador 2");
        PlayerId jugador3    = engine.spawnPlayer("Jugador 3");
        PlayerId jugador4    = engine.spawnPlayer("Jugador 4");
        PlayerId jugador5    = engine.spawnPlayer("Jugador 5");
        PlayerId jugador6    = engine.spawnPlayer("Jugador 6");

        // ── Asignación de roles (temporal — debe ser aleatoria en producción) ──
        engine.assignRolesRandomly();

        engine.startGame();

        // ── Posiciones iniciales en el mapa ───────────────────────────────────
        engine.movePlayer(localPlayer, new Position(500, 500));
        engine.movePlayer(jugador2,    new Position(500, 520));
        engine.movePlayer(jugador3,    new Position(800, 800));
        engine.movePlayer(jugador4,    new Position(850, 700));
        engine.movePlayer(jugador5,    new Position(860, 750));
        engine.movePlayer(jugador6,    new Position(870, 780));

        setScreen(new GameScreen(engine));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
