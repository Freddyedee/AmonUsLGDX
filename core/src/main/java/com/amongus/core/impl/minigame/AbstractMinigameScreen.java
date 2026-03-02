package com.amongus.core.impl.minigame;

import com.amongus.core.GameScreen;
import com.amongus.core.api.events.TaskCompletedEvent;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public abstract class AbstractMinigameScreen implements MinigameScreen {

    protected final GameEngine engine;
    protected final PlayerId playerId;
    protected final Task task;
    protected final SpriteBatch batch;          // común a casi todos
    protected boolean isCompleted = false;

    public AbstractMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        this.engine = engine;
        this.playerId = playerId;
        this.task = task;
        this.batch = new SpriteBatch();
    }

    @Override
    public void show() {
        // común: reproducir música de minijuego, resetear estado, etc.
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1);  // fondo oscuro típico
        batch.begin();
        // aquí las subclases dibujan su contenido específico
        renderContent(delta);
        batch.end();
    }

    // Método hook para que las subclases dibujen su UI específica
    protected abstract void renderContent(float delta);

    @Override
    public void resize(int width, int height) {
        // común o dejar a las subclases
    }

    @Override
    public void pause() { }
    @Override
    public void resume() { }
    @Override
    public void hide() { }

    @Override
    public void dispose() {
        batch.dispose();
        // dispose de texturas, fonts, etc. comunes si las hay
    }

    @Override
    public void complete() {
        if (isCompleted) return;
        isCompleted = true;
        // Opción A: publicar evento (recomendado)
        engine.getEventBus().publish(new TaskCompletedEvent(playerId, task.getId()));
        // Opción B: llamar directamente (menos desacoplado)
        // engine.markTaskCompleted(playerId, task.getId());
        cancel();
    }

    @Override
    public void cancel() {
        GameScreen main = engine.getMainScreen();
        if (main != null) {
            ((Game) Gdx.app.getApplicationListener()).setScreen(main);
        }
    }
}
