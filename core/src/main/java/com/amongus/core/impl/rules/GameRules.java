package com.amongus.core.impl.rules;

import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.Role;

import java.util.Collection;

public class GameRules {

    public GameRules(){

    }

    public static boolean canStartGame(Collection<Player> players){
        return players.size() >= 3;
    }

    public static boolean gameOver(Collection<Player> players){
        long impostorsAlive = players.stream()
                .filter(player-> player.alive() && player.getRole() == Role.IMPOSTOR)
                .count();

        long crewmatesAlive = players.stream()
                .filter(player -> player.alive() && player.getRole() == Role.CREWMATE)
                .count();

        return impostorsAlive == 0 || impostorsAlive >= crewmatesAlive;
    }
}
