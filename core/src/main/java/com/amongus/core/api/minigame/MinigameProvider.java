package com.amongus.core.api.minigame;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;

public interface MinigameProvider {
    MinigameScreen createScreen(PlayerId playerId, Task task);  // fábrica
    String getDisplayName();
}
