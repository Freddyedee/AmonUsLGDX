// ══════════════════════════════════════════════
// WhiteBoardTask.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════
package com.amongus.core.impl.task.quick;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class WhiteBoardTask extends BasicTask {

    private static final String MAP_SPRITE =
        "minijuegos/whiteBoard/whiteBoardInteractionObj.png";

    public WhiteBoardTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.QUICK, "Limpiar Pizarra", provider);
    }

    @Override
    public String getMapSpritePath() { return MAP_SPRITE; }

    @Override
    public float getMapSpriteScale() { return 1.5f; }
}
