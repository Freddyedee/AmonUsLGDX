package com.amongus.core.impl.player;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.amongus.core.view.TaskView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

public class PlayerController {
    private final GameEngine engine;
    private final PlayerId localPlayerId;
    private int direccion; // 1 derecha, -1 izquierda

    public PlayerController(GameEngine engine, PlayerId localPlayerId) {
        this.engine = engine;
        this.localPlayerId = localPlayerId;
        this.direccion = 1;
    }

    public int getDireccion() {
        return direccion;
    }

    /**
     * Procesa la entrada del teclado y solicita el movimiento al engine.
     *
     * @return true si el jugador intentó moverse.
     */
    public boolean handleInput() {
        int speed = 5;
        float dx = 0;
        float dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) { dx -= speed; direccion = -1; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { dx += speed; direccion = 1; }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) { dy += speed; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { dy -= speed; }

        boolean moving = dx != 0 || dy != 0;
        engine.setPlayerMoving(localPlayerId, moving, direccion);

        if (moving) {
            GameSnapshot snapshot = engine.getSnapshot();
            PlayerView me = snapshot.getPlayers().stream()
                .filter(p -> p.getId().equals(localPlayerId))
                .findFirst().orElse(null);

            if (me != null) {
                Position currentPos = me.getPosition();
                Position nextPos = new Position(
                    (int) (currentPos.x() + dx),
                    (int) (currentPos.y() + dy)
                );
                engine.movePlayer(localPlayerId, nextPos);
            }
        }

        // === NUEVA LÓGICA: ABRIR MINIJUEGO CON E ===
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            System.out.println("[PlayerController] E presionado");
            GameSnapshot snapshot = engine.getSnapshot();
            TaskView nearest = findNearestTask(snapshot);
            System.out.println("[PlayerController] tarea más cercana: " + nearest);
            if (nearest != null) {
                engine.initiateTask(nearest.getId());
                return true;
            }
        }

        return moving;
    }

    private TaskView findNearestTask(GameSnapshot snapshot) {
        PlayerView me = snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(localPlayerId))
            .findFirst()
            .orElse(null);
        if (me == null) return null;

        TaskView nearest = null;
        float minDist = Float.MAX_VALUE;

        // Iteramos con TaskView (tipo correcto)
        for (TaskView tv : snapshot.getTasks()) {
            float dist = Vector2.dst(
                me.getPosition().x(), me.getPosition().y(),
                tv.getPosition().x(), tv.getPosition().y()
            );

            if (dist < minDist && dist <= 150f) {
                minDist = dist;

                // Buscamos la Task real por ID (desde el engine o session)
                nearest = engine.getSnapshot().getTasks().stream()
                    .filter(t -> t.getId().equals(tv.getId()))
                    .findFirst()
                    .orElse(null);
            }
        }

        return nearest;
    }
}
