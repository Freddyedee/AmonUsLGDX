package com.amongus.core.impl.task.unique;

/**
 * Estado compartido entre GasolineTaskPart1 y GasolineTaskPart2.
 * Ambas partes referencian la misma instancia de este objeto.
 */
public class GasolineTaskGroup {

    private boolean part1Completed = false;

    public boolean isPart1Completed() {
        return part1Completed;
    }

    public void completePart1() {
        this.part1Completed = true;
        System.out.println("[GasolineGroup] Parte 1 completada, Parte 2 desbloqueada.");
    }
}
