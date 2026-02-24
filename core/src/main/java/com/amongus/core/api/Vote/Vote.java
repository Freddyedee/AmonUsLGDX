package com.amongus.core.api.Vote;

import com.amongus.core.api.player.PlayerId;

import java.util.UUID;

public interface Vote {

    PlayerId getVoterId();

    PlayerId getTargetId();

    boolean isSkip();
}
