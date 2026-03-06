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

public class HelpScreen implements Screen {

    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;

    public HelpScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Cargamos el skin Comic
        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        Table table = new Table();
        table.setFillParent(true);
        // table.setDebug(true);

        // ── Título ──
        Label titleLabel = new Label("COMO JUGAR", skin);
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.center);

        // ── Reglas del Juego (Extraídas del documento del proyecto) ──
        String reglas =
            "[ TRIPULANTES ]\n" +
                "Tu objetivo es ganar completando todas las tareas asignadas o descubriendo y expulsando al impostor mediante votacion.\n\n" +
                "[ IMPOSTORES ]\n" +
                "Tu objetivo es anular al resto de participantes sin ser descubierto. Al tener contacto con un oponente, lo paralizas (lo inhabilitas en el juego).\n" +
                "Puedes usar las vias de acceso rapido para moverte por el mapa de Villa Asia.\n\n" +
                "[ DINAMICA ]\n" +
                "- Si descubres un companero inhabilitado, reportalo para iniciar una votacion.\n" +
                "- En la votacion, el jugador mas votado es expulsado de la sede.\n" +
                "- Si nadie esta seguro, pueden saltar el voto (Skip) y la partida continua.";

        Label rulesLabel = new Label(reglas, skin);
        rulesLabel.setAlignment(Align.center);
        rulesLabel.setWrap(true); // Permite que el texto baje a la siguiente línea si es muy largo

        // ── Botón de regreso ──
        TextButton btnVolver = new TextButton("Volver", skin);
        btnVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Regresamos al menú principal
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // ── Organización en la Tabla ──
        table.add(titleLabel).padBottom(20).row();

        // Le damos un ancho máximo al texto (ej. 600 píxeles) para que el setWrap(true) funcione correctamente
        table.add(rulesLabel).width(600).padBottom(30).row();

        table.add(btnVolver).width(200).height(50);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
