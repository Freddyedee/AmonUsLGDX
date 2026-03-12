package com.amongus.core.api.task;

import java.util.UUID;

public record TaskId(UUID value) {

    public static TaskId random(){
        return new TaskId(UUID.randomUUID());
    }

    // Crea siempre el mismo UUID si el texto es el mismo
    public static TaskId generate(String seed) {
        return new TaskId(UUID.nameUUIDFromBytes(seed.getBytes()));
    }
}
