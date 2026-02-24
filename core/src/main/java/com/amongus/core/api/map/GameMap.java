package com.amongus.core.api.map;

import com.amongus.core.model.Position;

import java.util.List;

public interface GameMap {

    //List<Vent> getVents();
    boolean canMove(Position from, Position to);
    //boolean isVent(Position position);
    //Position exitVent(Position entryVent);
}
