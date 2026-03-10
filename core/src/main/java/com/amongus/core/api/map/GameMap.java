package com.amongus.core.api.map;
import com.amongus.core.model.Position;

public interface GameMap {
    boolean canMove(Position from, Position to);
    Position getNearestVent(Position pos, float maxDistance);
    Position getNextVentInNetwork(Position currentVent, int direction); // direction: 1 (siguiente) o -1 (anterior)
}
