// ══════════════════════════════════════════════════════
// TrashPart2MinigameProvider.java
// package com.amongus.core.impl.minigame.providers
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.minigame.providers;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.TrashPart2MinigameScreen;

public class TrashPart2MinigameProvider implements MinigameProvider {

    private final GameEngine engine;

    public TrashPart2MinigameProvider(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public MinigameScreen createScreen(PlayerId playerId, Task task) {
        return new TrashPart2MinigameScreen(engine, playerId, task);
    }

    @Override
    public String getDisplayName() { return "Tirar Basura"; }
}
