package com.amongus.core.api.task;

import java.util.UUID;

public record TaskId(UUID value) {
    public static TaskId random(){
        return new TaskId(UUID.randomUUID());
    }
}
