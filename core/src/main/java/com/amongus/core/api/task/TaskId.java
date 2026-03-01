package com.amongus.core.api.task;

import com.amongus.core.api.player.PlayerId;

import java.util.UUID;

public record TaskId(UUID value) {
    public static TaskId random(){
        return new TaskId(UUID.randomUUID());
    }
}
