// ══════════════════════════════════════════════════════
// TrashTaskPart1.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.unique;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.impl.task.unique.TrashTaskGroup;
import com.amongus.core.model.Position;

public class TrashTaskPart1 extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/trash/trashBag-removebg-preview.png";
    private final TrashTaskGroup group;

    public TrashTaskPart1(TaskId id, Position location,
                          MinigameProvider provider,TrashTaskGroup group) {
        super(id, location, TaskType.UNIQUE, "Recoger Basura", provider);
        this.group = group;
    }

    public TrashTaskGroup getGroup() { return group; }

    @Override public String getMapSpritePath() { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 0.8f; }
}
