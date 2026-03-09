// ══════════════════════════════════════════════════════
// BasketTask.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.quick;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class BasketTask extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/basket/basketBall-removebg-preview.png";

    public BasketTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.QUICK, "Basket", provider);
    }

    @Override public String getMapSpritePath()  { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 0.5f; }
}
