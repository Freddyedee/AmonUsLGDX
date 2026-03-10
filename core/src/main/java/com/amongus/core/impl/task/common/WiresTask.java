// ══════════════════════════════════════════════════════
// WiresTask.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.common;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class WiresTask extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/wires/objInteraccionWires.png";

    public WiresTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.COMMON, "Conectar Cables", provider);
    }

    @Override
    public String getMapSpritePath() {
        return MAP_SPRITE;
    }
}
