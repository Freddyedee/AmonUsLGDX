package com.amongus.core.impl.minigame;

import com.amongus.core.api.events.TaskCompletedEvent;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class AbstractMinigameScreen implements MinigameScreen {

    protected final GameEngine engine;
    protected final PlayerId   playerId;
    protected final Task       task;
    protected final SpriteBatch batch;
    protected boolean isCompleted = false;

    // ── Panel de fondo compartido por todos los minijuegos ────
    private Texture taskBackgroundTexture;

    /** Coordenadas y tamaño del panel centrado (calculados en show) */
    protected float panelX, panelY, panelW, panelH;

    public AbstractMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        this.engine   = engine;
        this.playerId = playerId;
        this.task     = task;
        this.batch    = new SpriteBatch();
    }

    @Override
    public void show() {
        taskBackgroundTexture = new Texture(Gdx.files.internal("minijuegos/taskBackground.png"));

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        // Panel ocupa el 60% del ancho y 65% del alto, centrado
        panelW = sw * 0.60f;
        panelH = sh * 0.65f;
        panelX = (sw - panelW) / 2f;
        panelY = (sh - panelH) / 2f;
    }

    @Override
    public void render(float delta) {
        // NO llamar ScreenUtils.clear() — el fondo lo gestiona GameScreen
        batch.begin();

        // 1. Dibujar el panel de fondo centrado
        batch.draw(taskBackgroundTexture, panelX, panelY, panelW, panelH);

        // 2. Las subclases dibujan su contenido DENTRO del panel
        renderContent(delta);

        batch.end();
    }

    /** Hook para que las subclases dibujen dentro del panel */
    protected abstract void renderContent(float delta);

    @Override public void resize(int width, int height) { }
    @Override public void pause()  { }
    @Override public void resume() { }
    @Override public void hide()   { }

    @Override
    public void dispose() {
        batch.dispose();
        if (taskBackgroundTexture != null) taskBackgroundTexture.dispose();
    }

    @Override
    public void complete() {
        if (isCompleted) return;
        isCompleted = true;
        engine.getEventBus().publish(new TaskCompletedEvent(playerId, task.getId()));
        cancel();
    }

    @Override
    public void cancel() {
        engine.clearActiveMinigame();
    }
}
