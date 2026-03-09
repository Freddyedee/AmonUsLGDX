package com.amongus.core.api.task;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.model.Position;

public interface Task {
    TaskId getId();

    String getName();

    Position getPosition();

    TaskType getTaskType();

    boolean canInteract(PlayerId playerId, Position playerPos);

    void interact(PlayerId playerId);

    MinigameProvider getMinigameProvider();

    /**
     * Ruta del sprite que se muestra en el mapa para esta tarea.
     * Si devuelve null, se usa el círculo amarillo por defecto.
     */
    default String getMapSpritePath() { return null; }

    default float getMapSpriteScale() { return 1.0f; }
}
