package com.amongus.core.view;

import com.amongus.core.model.GameState;
import java.util.List;

public final class GameSnapshot {

    private final GameState state;
    private final List<PlayerView> players;

    public GameSnapshot(GameState state, List<PlayerView> players){
        this.state = state;
        this.players = List.copyOf(players);
    }

    public GameState getState() {
        return state;
    }

    public List<PlayerView> getPlayers() {
        return players;
    }
}
