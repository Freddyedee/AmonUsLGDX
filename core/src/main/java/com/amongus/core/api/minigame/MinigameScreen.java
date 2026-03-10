package com.amongus.core.api.minigame;

import com.badlogic.gdx.Screen;

public interface MinigameScreen extends Screen {
    public void complete();   // llama engine.completeTask(playerId, task.getId())
    public void cancel();     // vuelve a FirstScreen
}
