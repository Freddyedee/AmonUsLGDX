package com.amongus.core.impl.task.quick;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

public class SimpleNumberTask extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/basicNumber/objInteraccionBasicNmbr.png";

    public SimpleNumberTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, TaskType.QUICK, "Enter Ascending Code", provider);
    }

    // Opcional: puedes sobreescribir métodos si la tarea tiene comportamiento único
    @Override
    public boolean canInteract(PlayerId playerId, Position playerPos) {
        // Ejemplo: agregar lógica extra, como "solo si no completada"
        return super.canInteract(playerId, playerPos);
    }

    public String getMapSpritePath() {
        return MAP_SPRITE;
    }
}
