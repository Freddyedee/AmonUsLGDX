package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.amongus.core.GameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenuScreen implements Screen {

    // Referencia al juego principal para poder cambiar de pantallas
    private final AmongUsGame game;

    // Stage es el "escenario" donde colocamos los botones
    private Stage stage;
    // Skin contiene los estilos visuales (fuentes, colores, texturas de los botones)
    private Skin skin;

    public MainMenuScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Inicializamos el escenario
        stage = new Stage(new ScreenViewport());

        // Es VITAL decirle a LibGDX que este escenario va a recibir los clicks del mouse
        Gdx.input.setInputProcessor(stage);

        // Cargamos el nuevo skin Comic.
        // Asegúrate de que la ruta coincida exactamente con la ubicación en tu carpeta assets.
        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        // Creamos la tabla principal y le decimos que ocupe toda la pantalla
        Table table = new Table();
        table.setFillParent(true);
        // table.setDebug(true); // Descomenta esto para ver las líneas de la tabla y ajustar el diseño

        // ── Creación de los botones ──
        TextButton btnJugar = new TextButton("Jugar", skin);
        TextButton btnAyuda = new TextButton("Ayuda", skin);
        TextButton btnAcercaDe = new TextButton("Acerca de", skin);
        TextButton btnSettings = new TextButton("Configuracion", skin);
        TextButton btnSalir = new TextButton("Salir", skin);

        // ── Agregando funcionalidad (Listeners) ──

        btnJugar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Cambiamos a la pantalla de selección de red
                game.setScreen(new PlayMenuScreen(game));
                dispose();
            }
        });

        btnAyuda.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new HelpScreen(game)); // ¡Llamamos a la pantalla de Ayuda!
                dispose();
            }
        });

        btnAcercaDe.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new AboutScreen(game)); //
                dispose(); // Liberamos la pantalla actual
            }
        });

        // Listener para el botón de Configuración / Opciones
        btnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Cambiamos a la nueva pantalla de ajustes
                game.setScreen(new SettingsScreen(game));
                // Destruimos el menú principal para liberar memoria
                dispose();
            }
        });

        btnSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit(); // Cierra el juego
            }
        });

        // ── Organizando los botones en la tabla ──
        // .pad(10) añade un margen de separación entre botones
        // .fillX() hace que todos los botones tengan el mismo ancho

        table.add(btnJugar).fillX().uniformX().pad(10);
        table.row(); // Salto de línea (siguiente fila)

        table.add(btnAyuda).fillX().uniformX().pad(10);
        table.row();

        table.add(btnAcercaDe).fillX().uniformX().pad(10);
        table.row();

        table.add(btnSettings).fillX().uniformX().pad(10);
        table.row();

        table.add(btnSalir).fillX().uniformX().pad(10);

        // Finalmente, añadimos la tabla al escenario
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        // Limpiamos la pantalla con un color oscuro (RGBA)
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Actualizamos y dibujamos el escenario (los botones)
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // Actualiza el tamaño del escenario si el usuario redimensiona la ventana
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
