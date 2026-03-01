package com.amongus.core.api.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Screen;

public interface MinigameScreen extends Screen {
    public void complete();   // llama engine.completeTask(playerId, task.getId())
    public void cancel();     // vuelve a FirstScreen
}
