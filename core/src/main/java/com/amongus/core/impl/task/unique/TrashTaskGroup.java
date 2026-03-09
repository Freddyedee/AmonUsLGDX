// ══════════════════════════════════════════════════════
// TrashTaskGroup.java
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.unique;

public class TrashTaskGroup {
    private boolean part1Completed = false;

    public boolean isPart1Completed() { return part1Completed; }

    public void completePart1() {
        this.part1Completed = true;
        System.out.println("[TrashGroup] Parte 1 completada, Parte 2 desbloqueada.");
    }
}
