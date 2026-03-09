package com.amongus.core.impl.minigame.providers;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.GasolinePart2MinigameScreen;

public class GasolinePart2MinigameProvider implements MinigameProvider {

    private final GameEngine engine;

    public GasolinePart2MinigameProvider(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public MinigameScreen createScreen(PlayerId playerId, Task task) {
        return new GasolinePart2MinigameScreen(engine, playerId, task);
    }

    @Override
    public String getDisplayName() { return "Vaciar Bidón en Estación"; }
}
