package com.amongus.core.impl.minigame.providers;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.GasolinePart1MinigameScreen;
import com.amongus.core.impl.task.unique.GasolineTaskGroup;
import com.amongus.core.impl.task.unique.GasolineTaskPart1;

public class GasolinePart1MinigameProvider implements MinigameProvider {

    private final GameEngine engine;
    private final GasolineTaskGroup group;

    public GasolinePart1MinigameProvider(GameEngine engine, GasolineTaskGroup group) {
        this.engine = engine;
        this.group  = group;
    }

    @Override
    public MinigameScreen createScreen(PlayerId playerId, Task task) {
        return new GasolinePart1MinigameScreen(engine, playerId, task, group);
    }

    @Override
    public String getDisplayName() { return "Llenar Bidón"; }
}
