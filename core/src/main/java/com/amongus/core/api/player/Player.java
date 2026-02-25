package com.amongus.core.api.player;

public interface Player {

    PlayerId getId();
    String getName();
    Role getRole();

    boolean alive();
    boolean connected();

    void move(int deltaX, int deltaY);

    // Mutaciones controladas del estado
    void kill();
    void disconnect();
}
