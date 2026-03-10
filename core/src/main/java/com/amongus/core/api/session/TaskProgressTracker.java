package com.amongus.core.api.session;

public class TaskProgressTracker {

    private final int total;
    private int pending;

    public TaskProgressTracker(int total) {
        this.total   = total;
        this.pending = total;
    }

    public void taskCompleted() {
        if (pending > 0) pending--;
    }

    public int getTotal()   { return total;   }
    public int getPending() { return pending; }
    public int getCompleted() { return total - pending; }
    public boolean allCompleted() { return pending == 0; }
    public float getProgress() { return (float) getCompleted() / total; }
}
