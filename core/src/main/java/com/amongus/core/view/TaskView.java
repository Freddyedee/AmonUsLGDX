package com.amongus.core.view;

import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.model.Position;

public final class TaskView {

    private final TaskId id;
    private final Position position;

    public TaskView(Task task) {
        this.id = task.getId();
        this.position = task.getPosition();
    }

    public TaskId getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }
}
