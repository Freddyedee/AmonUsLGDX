package com.amongus.core.impl.engine;


import com.amongus.core.api.Vote.Vote;
import com.amongus.core.api.events.EventBus;
import com.amongus.core.api.map.GameMap;
import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.session.GameSession;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.event.EventBusImpl;
import com.amongus.core.impl.map.SimpleMap;
import com.amongus.core.impl.session.GameSesionImpl;

import java.util.UUID;

/**
 * GameEngine es la FACHADA del Core.
 *
 * Es el punto de entrada único para interactuar con el dominio del juego.
 * Los módulos externos (Application, Desktop, Multiplayer) SOLO deben
 * comunicarse con el Core a través de esta clase.
 *
 * Responsabilidades:
 *  - Crear y mantener una sesión de juego
 *  - Exponer operaciones de alto nivel (casos de uso)
 *  - Delegar la lógica real a GameSession
 *  - Gestionar el EventBus
 *
 * Importante:
 *  - NO contiene reglas complejas
 *  - NO conoce detalles de UI, red o persistencia
 */

public class GameEngine {

    /*
    * Identificador único de la sesión actual
    * utilizable para el multiplayer.
    * */
    private final UUID sessionId;

    /*
    * Bus de eventos del dominio
    * permite desacoplar el core de los observadores externos
    * */
    private final EventBus eventBus;

    /*
    * Sesión de juego activa.
    * Contiene toda la lógic del dominio.
    */
    private final GameSession session;

    private final GameMap gameMap;

    /**
     * GameEngine es la FACHADA del Core.
     *
     * Es el punto de entrada único para interactuar con el dominio del juego.
     * Los módulos externos (Application, Desktop, Multiplayer) SOLO deben
     * comunicarse con el Core a través de esta clase.
     *
     * Responsabilidades:
     *  - Crear y mantener una sesión de juego
     *  - Exponer operaciones de alto nivel (casos de uso)
     *  - Delegar la lógica real a GameSession
     *  - Gestionar el EventBus
     *
     * Importante:
     *  - NO contiene reglas complejas
     *  - NO conoce detalles de UI, red o persistencia
     */

    public GameEngine(){
        this.sessionId = UUID.randomUUID();
        this.eventBus = new EventBusImpl();
        this.gameMap = new SimpleMap();

        this.session = new GameSesionImpl(sessionId, eventBus, gameMap);
    }

    /* ===================== CONSULTAS ===================== */

    public UUID getSessionId() {
        return sessionId;
    }

    public GameState getGameState() {
        return session.getCurrentState();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /* ===================== CASOS DE USO ===================== */

        public void joinPlayer(Player player) {
        session.addPlayer(player);
    }

    public void startGame() {
        session.startGame();
    }

    public void movePlayer(PlayerId playerId, Object destination) {
        session.movePlayer(playerId, destination);
    }

    public void castVote(Vote vote) {
        session.castVote(vote);
    }


}
