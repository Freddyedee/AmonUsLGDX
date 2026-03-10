package com.amongus.core.impl.task.unique;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

/**
 * Parte 1 de la tarea de gasolina.
 * Llenar el bidón (gas can).
 * Siempre es interactuable mientras no esté completada.
 */
public class GasolineTaskPart1 extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/gasoline/objInteraccionGas-removebg-preview.png";

    private final GasolineTaskGroup group;

    public GasolineTaskPart1(TaskId id, Position location,
                             MinigameProvider provider, GasolineTaskGroup group) {
        super(id, location, TaskType.UNIQUE, "Llenar Bidón", provider);
        this.group = group;
    }

    public GasolineTaskGroup getGroup() {
        return group;
    }

    @Override
    public String getMapSpritePath() {
        return MAP_SPRITE;
    }

    @Override
    public float getMapSpriteScale() {
        return 0.5f;
    }
}
