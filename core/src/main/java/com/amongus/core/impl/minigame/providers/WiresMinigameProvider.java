package com.amongus.core.impl.minigame.providers;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.WiresMinigameScreen;

public class WiresMinigameProvider implements MinigameProvider {

    private final GameEngine engine;

    public WiresMinigameProvider(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public MinigameScreen createScreen(PlayerId playerId, Task task) {
        return new WiresMinigameScreen(engine, playerId, task);
    }

    @Override
    public String getDisplayName() {
        return "Conectar Cables";
    }
}
