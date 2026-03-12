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

    // Helper para generar el ID determinista
    private TaskId createId(String name, Position loc) {
        // Usamos solo el nombre para asegurar 100% de coincidencia en red
        return TaskId.generate(name);
    }

    public Task createNumberTask(Position location) {
        return new SimpleNumberTask(
            createId("NumberTask", location), // ← Ya no es random
            location,
            new NumberCodeMinigameProvider(engine)
        );
    }

    public Task createBotellonTask(Position location) {
        return new BotellonTask(
            createId("BotellonTask", location),
            location,
            new BotellonMinigameProvider(engine)
        );
    }

    public Task createWiresTask(Position location) {
        return new WiresTask(
            createId("WiresTask", location),
            location,
            new WiresMinigameProvider(engine)
        );
    }

    public List<Task> createGasolineTask(Position locationPart1, Position locationPart2) {
        GasolineTaskGroup group = new GasolineTaskGroup();
        Task part1 = new GasolineTaskPart1(createId("Gasoline1", locationPart1), locationPart1, new GasolinePart1MinigameProvider(engine, group), group);
        Task part2 = new GasolineTaskPart2(createId("Gasoline2", locationPart2), locationPart2, new GasolinePart2MinigameProvider(engine), group);
        return List.of(part1, part2);
    }

    public Task createWhiteBoardTask(Position location) {
        return new WhiteBoardTask(
            createId("WhiteBoard", location),
            location,
            new WhiteBoardMinigameProvider(engine)
        );
    }

    public List<Task> createTrashTask(Position locationPart1, Position locationPart2) {
        TrashTaskGroup group = new TrashTaskGroup();
        Task part1 = new TrashTaskPart1(createId("Trash1", locationPart1), locationPart1, new TrashPart1MinigameProvider(engine, group), group);
        Task part2 = new TrashTaskPart2(createId("Trash2", locationPart2), locationPart2, new TrashPart2MinigameProvider(engine), group);
        return List.of(part1, part2);
    }

    public Task createBasketTask(Position location) {
        return new BasketTask(
            createId("BasketTask", location),
            location,
            new BasketMinigameProvider(engine)
        );
    }

    public Task createToiletTask(Position location) {
        return new ToiletTask(
            createId("ToiletTask", location),
            location,
            new ToiletMinigameProvider(engine)
        );
    }

    public Task createLibraryTask(Position location) {
        return new LibraryTask(
            createId("LibraryTask", location),
            location,
            new LibraryMinigameProvider(engine)
        );
    }
}
