package com.amongus.core.impl.task.sabotageTask;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.model.Position;

public class FixLightsSabotageTask extends SabotageTask {

    private static final String MAP_SPRITE =
        "minijuegos/fixLights/fixLightsObjt-removebg-preview.png";

    public FixLightsSabotageTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, "Arreglar Luces", provider);
    }

    @Override public String getMapSpritePath()  { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 1.3f; }
}
