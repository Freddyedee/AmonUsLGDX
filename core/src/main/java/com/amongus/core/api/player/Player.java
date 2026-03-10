package com.amongus.core.api.player;

import com.amongus.core.model.Position;

public interface Player {
    PlayerId getId();
    String getName();
    Role getRole();
    boolean alive();
    boolean connected();
    boolean isVenting();
    void setVenting(boolean venting);

    // Cambiamos int por float
    void move(float deltaX, float deltaY);

    void kill();
    void disconnect();
    Position getPosition();
    void updatePosition(Position targetPos);
}
