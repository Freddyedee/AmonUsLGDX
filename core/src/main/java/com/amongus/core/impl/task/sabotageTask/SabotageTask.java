// ══════════════════════════════════════════════════════
// SabotageTask.java  — base para todas las tareas de sabotaje
// package com.amongus.core.impl.task
// ══════════════════════════════════════════════════════
package com.amongus.core.impl.task.sabotageTask;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.task.BasicTask;
import com.amongus.core.model.Position;

/**
 * Tarea de sabotaje:
 *  - No cuenta para la progress bar
 *  - Disponible para TODOS los jugadores (crewmates la pueden completar)
 *  - Solo puede haber 1 activa a la vez
 *  - Al completarla cualquier jugador, se resuelve para todos
 */
public abstract class SabotageTask extends BasicTask {

    public SabotageTask(TaskId id, Position location, String name, MinigameProvider provider) {
        super(id, location, TaskType.SABOTAGE, name, provider);
    }

    /** Las tareas de sabotaje nunca cuentan para la barra de progreso */
    @Override
    public boolean countsForProgress() { return false; }
}
