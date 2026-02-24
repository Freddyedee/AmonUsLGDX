package com.amongus.core.impl.task;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;

public class BasicTask implements Task {

    private boolean completed = false;

    public void complete(){
        completed = true;
    }

    @Override
    public boolean isCompleted(){
         return completed;
    }

}
