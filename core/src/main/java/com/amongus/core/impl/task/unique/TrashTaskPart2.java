// ══════════════════════════════════════════════════════
// TrashTaskPart2.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.unique;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class TrashTaskPart2 extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/trash/trashCanInteractionObj.png";
    private final TrashTaskGroup group;

    public TrashTaskPart2(TaskId id, Position location,
                          MinigameProvider provider, TrashTaskGroup group) {
        super(id, location, TaskType.UNIQUE, "Tirar Basura", provider);
        this.group = group;
    }

    @Override
    public boolean canInteract(PlayerId playerId, Position playerPos) {
        if (!group.isPart1Completed()) {
            System.out.println("[TrashPart2] Bloqueada: completa primero la Parte 1.");
            return false;
        }
        return super.canInteract(playerId, playerPos);
    }

    @Override public String getMapSpritePath() { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 0.7f; }
}
