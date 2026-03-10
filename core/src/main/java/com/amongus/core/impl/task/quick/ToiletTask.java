// ══════════════════════════════════════════════════════
// ToiletTask.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.quick;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class ToiletTask extends BasicTask {

    private static final String MAP_SPRITE =
        "minijuegos/toilet/plunger-removebg-preview.png";

    public ToiletTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.QUICK, "Destapar Toilet", provider);
    }

    @Override public String getMapSpritePath()  { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 0.7f; }
}
