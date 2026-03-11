// ══════════════════════════════════════════════════════
// InternetSabotageTask.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.sabotageTask;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.model.Position;

public class InternetSabotageTask extends SabotageTask {

    private static final String MAP_SPRITE = "minijuegos/sabotageInternet/internetObj-removebg-preview.png";

    public InternetSabotageTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, "Arreglar Internet", provider);
    }

    @Override public String getMapSpritePath()  { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 1.0f; }
}
