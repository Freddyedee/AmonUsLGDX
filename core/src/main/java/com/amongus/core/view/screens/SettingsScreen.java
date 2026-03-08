package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class SettingsScreen implements Screen {

    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;
    private Preferences prefs;

    public SettingsScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Asegúrate de tener tu archivo JSON de la UI en la ruta correcta
        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        Table table = new Table();
        table.setFillParent(true);

        // ── Elementos de la UI ──
        Label titleLabel = new Label("CONFIGURACION", skin);
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.center);

        Label resLabel = new Label("Resolucion de Pantalla:", skin);

        // Botones de Resolución
        TextButton btnRes1 = new TextButton("800 x 600 (Ventana)", skin);
        TextButton btnRes2 = new TextButton("1280 x 720 (HD)", skin);
        TextButton btnRes3 = new TextButton("1920 x 1080 (Full HD)", skin);

        TextButton btnVolver = new TextButton("Volver al Menu", skin);

        // ── Listeners para cambiar la resolución ──
        btnRes1.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.graphics.setWindowedMode(800, 600);
            }
        });

        btnRes2.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.graphics.setWindowedMode(1280, 720);
            }
        });

        btnRes3.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.graphics.setWindowedMode(1920, 1080);
            }
        });

        btnVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // ── Organización visual en la tabla ──
        table.add(titleLabel).padBottom(40).colspan(2).row();

        table.add(resLabel).padBottom(20).colspan(2).row();
        table.add(btnRes1).width(300).height(50).padBottom(10).row();
        table.add(btnRes2).width(300).height(50).padBottom(10).row();
        table.add(btnRes3).width(300).height(50).padBottom(40).row();

        table.add(btnVolver).width(200).height(50).padTop(20);

        stage.addActor(table);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
