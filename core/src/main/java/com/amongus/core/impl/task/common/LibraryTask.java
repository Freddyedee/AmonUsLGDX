// ══════════════════════════════════════════════════════
// LibraryTask.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.common;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class LibraryTask extends BasicTask {

    private static final String MAP_SPRITE =
        "minijuegos/library/bookObjInteraction-removebg-preview.png";

    public LibraryTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.COMMON, "Ordenar Biblioteca", provider);
    }

    @Override public String getMapSpritePath()  { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 1.3f; }
}
