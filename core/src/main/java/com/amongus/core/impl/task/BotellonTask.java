package com.amongus.core.impl.task;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.model.Position;

public class BotellonTask extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/botellon/objInteraccionBotellon.png";

    public BotellonTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.QUICK, "Rellenar Botellón", provider);
    }

    @Override
    public String getMapSpritePath() {
        return MAP_SPRITE;
    }
}
