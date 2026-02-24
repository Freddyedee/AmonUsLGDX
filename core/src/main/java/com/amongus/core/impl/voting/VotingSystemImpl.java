package com.amongus.core.impl.voting;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.Vote.Vote;

import java.util.*;

public class VotingSystemImpl {

    private final Map<PlayerId, Vote> votes = new HashMap<>();

    public void castVote(Vote vote){
        votes.put(vote.getVoterId(), vote);
    }

    public Optional<PlayerId> resolve(){
        Map<PlayerId, Integer> count = new HashMap<>();

        for (Vote vote : votes.values()){
            if(vote.isSkip()){
                continue;
            }

            PlayerId target = vote.getTargetId();
            count.merge(target, 1, Integer::sum);
        }

        votes.clear();

        return count.entrySet()
                .stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

    }

    public void clearVotes(){
        votes.clear();
    }


}
