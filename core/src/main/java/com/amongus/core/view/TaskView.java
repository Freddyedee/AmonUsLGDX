package com.amongus.core.view;

import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.model.Position;

public final class TaskView {

    private final TaskId id;
    private final Position position;
    private final boolean completed;
    private final String mapSpritePath;

    public TaskView(Task task, boolean completed) {
        this.id = task.getId();
        this.position = task.getPosition();
        this.completed = completed;
        this.mapSpritePath = task.getMapSpritePath();
    }

    public TaskId getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isCompleted() { return completed; }

    public String getMapSpritePath() { return mapSpritePath; }
}
