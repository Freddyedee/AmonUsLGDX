package com.amongus.core.impl.voting;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.Vote.Vote;
import java.util.*;

public class VotingSystemImpl {
    private final Map<PlayerId, Vote> votes = new HashMap<>();

    public void castVote(Vote vote){
        votes.put(vote.getVoterId(), vote);
    }

    // Nuevo: Exponer los jugadores que ya votaron para la UI
    public Set<PlayerId> getVotedPlayers() {
        return votes.keySet();
    }

    public Optional<PlayerId> resolve(){
        Map<PlayerId, Integer> count = new HashMap<>();
        int skipVotes = 0; // Contar los skips explícitos

        for (Vote vote : votes.values()){
            if(vote.isSkip()){
                skipVotes++;
                continue;
            }
            PlayerId target = vote.getTargetId();
            count.merge(target, 1, Integer::sum);
        }
        votes.clear();

        if (count.isEmpty()) return Optional.empty(); // Nadie votó o todos skippearon

        int maxVotes = Collections.max(count.values());

        // Si el skip tiene más o iguales votos que el jugador más votado, se skippea
        if (skipVotes >= maxVotes) return Optional.empty();

        // Verificamos si hay empate (dos o más jugadores con la cantidad máxima de votos)
        long empates = count.values().stream().filter(v -> v == maxVotes).count();
        if (empates > 1) return Optional.empty(); // Empate = skip automático

        return count.entrySet().stream()
            .filter(e -> e.getValue() == maxVotes)
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public void clearVotes(){
        votes.clear();
    }
}
