package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

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
        // FitViewport escala automáticamente toda la UI sin importar si es 800x600 o 1080p
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        // Cargamos las preferencias guardadas (si no existe, usa "Jugador" por defecto)
        prefs = Gdx.app.getPreferences("AmongUsPrefs");

        Table table = new Table();
        table.setFillParent(true);

        // ── Elementos de la UI ──
        Label titleLabel = new Label("CONFIGURACION", skin);
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.center);

        // --- Área del Nombre ---
        Label nameLabel = new Label("Nombre del Jugador:", skin);
        TextField nameField = new TextField(prefs.getString("playerName", "Jugador"), skin);
        nameField.setAlignment(Align.center);

        Label resLabel = new Label("Resolucion de Pantalla:", skin);

        TextButton btnRes1 = new TextButton("800 x 600 (Ventana)", skin);
        TextButton btnRes2 = new TextButton("1280 x 720 (HD)", skin);
        TextButton btnRes3 = new TextButton("1920 x 1080 (Full HD)", skin);

        TextButton btnVolver = new TextButton("Guardar y Volver", skin);

        // ── Listeners de Botones ──
        btnRes1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.graphics.setWindowedMode(800, 600);
            }
        });

        btnRes2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.graphics.setWindowedMode(1280, 720);
            }
        });

        btnRes3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.graphics.setWindowedMode(1920, 1080);
            }
        });

        btnVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Guardamos el nombre ingresado al darle a volver
                String newName = nameField.getText().trim();
                if(newName.isEmpty()) newName = "Jugador"; // Validación para que no quede vacío

                prefs.putString("playerName", newName);
                prefs.flush(); // Guarda los cambios en disco inmediatamente

                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // ── Organización visual en la tabla ──
        table.add(titleLabel).padBottom(30).colspan(2).row();

        // Agregamos el área de nombre a la tabla
        table.add(nameLabel).padBottom(5).row();
        table.add(nameField).width(300).height(50).padBottom(20).row();

        table.add(resLabel).padBottom(10).row();
        table.add(btnRes1).width(300).height(50).padBottom(10).row();
        table.add(btnRes2).width(300).height(50).padBottom(10).row();
        table.add(btnRes3).width(300).height(50).padBottom(30).row();

        table.add(btnVolver).width(250).height(50).padTop(10);

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
