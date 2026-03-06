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

public class AboutScreen implements Screen {

    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;

    public AboutScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Cargamos el mismo skin que usamos en el menú principal
        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        Table table = new Table();
        table.setFillParent(true);
        // table.setDebug(true); // Útil para ver los márgenes

        // ── Título ──
        Label titleLabel = new Label("ACERCA DE", skin);
        titleLabel.setFontScale(1.5f); // Hacemos el título un poco más grande
        titleLabel.setAlignment(Align.center);

        // ── Información Técnica requerida por el proyecto ──
        Label techLabel = new Label(
            "Lenguaje de Programacion: Java\n" +
                "Librerias Externas: LibGDX\n" +
                "Version Actualizada: 1.0", skin);
        techLabel.setAlignment(Align.center);

        // ── Desarrolladores (Equipo de 5 integrantes) ──
        Label devTitleLabel = new Label("DESARROLLADORES", skin);
        devTitleLabel.setFontScale(1.2f);

        Label devsLabel = new Label(
            "Freddy Marcano - Estructura, Modularizacion, Eventos como Votos y las Kills\n" +
                "Freddy Salazar - Colisiones y Menus\n" +
                "Sebastian Argotte - Implementacion de Misiones\n" +
                "Eliuber Gonzalez - Multijugador (Red)\n" +
                "Santiago Bolivar - Arte y Mapas (Villa Asia)", skin);
        devsLabel.setAlignment(Align.center);

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

        // ── Construcción de la tabla (Organización visual) ──
        table.add(titleLabel).padBottom(20).row();
        table.add(techLabel).padBottom(30).row();
        table.add(devTitleLabel).padBottom(10).row();
        table.add(devsLabel).padBottom(40).row();
        table.add(btnVolver).width(200).height(50); // Tamaño fijo para el botón

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1); // Un fondo ligeramente azulado oscuro
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
