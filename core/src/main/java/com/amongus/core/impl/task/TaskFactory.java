package com.amongus.core.impl.task;

import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.providers.BotellonMinigameProvider;
import com.amongus.core.impl.minigame.providers.NumberCodeMinigameProvider;
import com.amongus.core.impl.minigame.providers.WiresMinigameProvider;
import com.amongus.core.model.Position;

public class TaskFactory {
    private final GameEngine engine;

    public TaskFactory(GameEngine engine) {
        this.engine = engine;
    }

    public Task createNumberTask(Position location) {
        return new SimpleNumberTask(
            TaskId.random(),                        // ← generamos el ID aquí
            location,
            new NumberCodeMinigameProvider(engine)
        );
    }

    public Task createBotellonTask(Position location) {
        return new BotellonTask(
            TaskId.random(),
            location,
            new BotellonMinigameProvider(engine)
        );
    }

    public Task createWiresTask(Position location) {
        return new WiresTask(
            TaskId.random(),
            location,
            new WiresMinigameProvider(engine)
        );
    }
}
