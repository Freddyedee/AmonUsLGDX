package com.amongus.core.api.player;

public enum Role {
    CREWMATE,
    IMPOSTOR;

    public boolean isImpostor() {
        return this == IMPOSTOR;
    }
}
