package com.amongus.core.impl.task.unique;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.impl.task.unique.GasolineTaskGroup;
import com.amongus.core.model.Position;

/**
 * Parte 2 de la tarea de gasolina.
 * Vaciar el bidón en la estación de recarga.
 * Solo es interactuable si la Parte 1 está completada.
 */
public class GasolineTaskPart2 extends BasicTask {

    private static final String MAP_SPRITE = "minijuegos/gasoline/objInteraccionGas-removebg-preview.png";

    private final GasolineTaskGroup group;

    public GasolineTaskPart2(TaskId id, Position location,
                             MinigameProvider provider, GasolineTaskGroup group) {
        super(id, location, TaskType.UNIQUE, "Vaciar Bidón en Estación", provider);
        this.group = group;
    }

    /**
     * Solo se puede interactuar si la parte 1 ya fue completada
     * Y el jugador está en rango.
     */
    @Override
    public boolean canInteract(PlayerId playerId, Position playerPos) {
        if (!group.isPart1Completed()) {
            System.out.println("[GasolinePart2] Bloqueada: completa primero la Parte 1.");
            return false;
        }
        return super.canInteract(playerId, playerPos);
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
