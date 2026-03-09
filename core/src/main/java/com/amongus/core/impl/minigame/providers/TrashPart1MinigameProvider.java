// ══════════════════════════════════════════════════════
// TrashPart1MinigameProvider.java
// package com.amongus.core.impl.minigame.providers
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.minigame.providers;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.TrashPart1MinigameScreen;
import com.amongus.core.impl.task.unique.TrashTaskGroup;

public class TrashPart1MinigameProvider implements MinigameProvider {

    private final GameEngine engine;
    private final TrashTaskGroup group;

    public TrashPart1MinigameProvider(GameEngine engine, TrashTaskGroup group) {
        this.engine = engine;
        this.group  = group;
    }

    @Override
    public MinigameScreen createScreen(PlayerId playerId, Task task) {
        return new TrashPart1MinigameScreen(engine, playerId, task, group);
    }

    @Override
    public String getDisplayName() { return "Recoger Basura"; }
}
