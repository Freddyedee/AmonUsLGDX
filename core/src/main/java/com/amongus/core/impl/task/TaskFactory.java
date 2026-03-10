package com.amongus.core.impl.task;

import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.minigame.providers.*;
import com.amongus.core.impl.task.common.LibraryTask;
import com.amongus.core.impl.task.common.WiresTask;
import com.amongus.core.impl.task.quick.*;
import com.amongus.core.impl.task.unique.*;
import com.amongus.core.model.Position;

import java.util.List;

public class TaskFactory {
    private final GameEngine engine;

    public TaskFactory(GameEngine engine) {
        this.engine = engine;
    }

    public Task createNumberTask(Position location) {
        return new SimpleNumberTask(
            TaskId.random(),                        // ← generamos el ID aquí
            location,
            new NumberCodeMinigameProvider(engine)
        );
    }

    public Task createBotellonTask(Position location) {
        return new BotellonTask(
            TaskId.random(),
            location,
            new BotellonMinigameProvider(engine)
        );
    }

    public Task createWiresTask(Position location) {
        return new WiresTask(
            TaskId.random(),
            location,
            new WiresMinigameProvider(engine)
        );
    }

    /**
     * Crea las dos partes de la tarea de gasolina.
     * Ambas comparten el mismo GasolineTaskGroup.
     * @param locationPart1 posición donde se llena el bidón
     * @param locationPart2 posición de la estación de recarga
     * @return lista con [part1, part2] — añadir ambas al pool de tareas
     */
    public List<Task> createGasolineTask(Position locationPart1, Position locationPart2) {
        GasolineTaskGroup group = new GasolineTaskGroup();

        Task part1 = new GasolineTaskPart1(
            TaskId.random(),
            locationPart1,
            new GasolinePart1MinigameProvider(engine, group),
            group
        );

        Task part2 = new GasolineTaskPart2(
            TaskId.random(),
            locationPart2,
            new GasolinePart2MinigameProvider(engine),
            group
        );

        return List.of(part1, part2);
    }

    public Task createWhiteBoardTask(Position location) {
        return new WhiteBoardTask(
            TaskId.random(),
            location,
            new WhiteBoardMinigameProvider(engine)
        );
    }

    public List<Task> createTrashTask(Position locationPart1, Position locationPart2) {
        TrashTaskGroup group = new TrashTaskGroup();

        Task part1 = new TrashTaskPart1(
            TaskId.random(), locationPart1,
            new TrashPart1MinigameProvider(engine, group), group
        );
        Task part2 = new TrashTaskPart2(
            TaskId.random(), locationPart2,
            new TrashPart2MinigameProvider(engine), group
        );
        return List.of(part1, part2);
    }

    public Task createBasketTask(Position location) {
        return new BasketTask(
            TaskId.random(),
            location,
            new BasketMinigameProvider(engine)
        );
    }

    public Task createToiletTask(Position location) {
        return new ToiletTask(
            TaskId.random(),
            location,
            new ToiletMinigameProvider(engine)
        );
    }

    public Task createLibraryTask(Position location) {
        return new LibraryTask(
            TaskId.random(),
            location,
            new LibraryMinigameProvider(engine)
        );
    }
}
