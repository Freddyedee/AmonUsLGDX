// ══════════════════════════════════════════════════════
// InternetSabotageMinigameProvider.java
// package com.amongus.core.impl.minigame.providers
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.minigame.providers;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.InternetSabotageMinigameScreen;
import com.amongus.core.impl.sabotage.SabotageManager;

public class InternetSabotageMinigameProvider implements MinigameProvider {

    private final GameEngine      engine;
    private final SabotageManager sabotageManager;

    public InternetSabotageMinigameProvider(GameEngine engine, SabotageManager sabotageManager) {
        this.engine          = engine;
        this.sabotageManager = sabotageManager;
    }

    @Override
    public MinigameScreen createScreen(PlayerId playerId, Task task) {
        return new InternetSabotageMinigameScreen(engine, playerId, task, sabotageManager);
    }

    @Override
    public String getDisplayName() { return "Arreglar Internet"; }
}
