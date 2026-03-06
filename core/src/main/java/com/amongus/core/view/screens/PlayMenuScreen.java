package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.amongus.core.api.map.MapType; // <-- IMPORTANTE: Importamos el Enum de los mapas
import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class PlayMenuScreen implements Screen {

    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;

    public PlayMenuScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        Table table = new Table();
        table.setFillParent(true);

        // ── Elementos de la UI ──
        Label titleLabel = new Label("MODO MULTIJUGADOR", skin);
        titleLabel.setFontScale(1.5f);
        titleLabel.setAlignment(Align.center);

        // Campo para el nombre
        Label nameLabel = new Label("Tu Nombre:", skin);
        TextField nameField = new TextField("Jugador", skin);
        nameField.setAlignment(Align.center);

        // ── NUEVO: Elementos para elegir el Mapa ──
        // Un arreglo de 1 elemento para poder modificar la variable dentro del ClickListener
        final MapType[] mapaSeleccionado = {MapType.MAPA_1};
        TextButton btnMapa = new TextButton("Mapa: Aulas Principal", skin);

        // Botón Host
        TextButton btnHost = new TextButton("Crear Partida (Host)", skin);

        // Campo para la IP y botón de unirse
        Label ipLabel = new Label("IP del Host:", skin);
        TextField ipField = new TextField("127.0.0.1", skin);
        ipField.setAlignment(Align.center);
        TextButton btnJoin = new TextButton("Conectarse a IP", skin);

        TextButton btnVolver = new TextButton("Volver", skin);

        // ── Listeners ──

        // Listener para el botón del mapa (Alterna entre Mapa 1 y Mapa 2)
        btnMapa.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (mapaSeleccionado[0] == MapType.MAPA_1) {
                    mapaSeleccionado[0] = MapType.MAPA_2;
                    btnMapa.setText("Mapa: Cancha y Estacionamiento");
                } else {
                    mapaSeleccionado[0] = MapType.MAPA_1;
                    btnMapa.setText("Mapa: Aulas Principal");
                }
            }
        });

        btnHost.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String miNombre = nameField.getText().trim();
                if (miNombre.isEmpty()) miNombre = "Host";

                System.out.println("Iniciando como HOST. Nombre: " + miNombre);

                // NUEVO: Ahora pasamos el mapaSeleccionado[0] como 4to parámetro
                game.startNetworkGame(true, miNombre, "localhost", mapaSeleccionado[0]);
                dispose();
            }
        });

        btnJoin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String miNombre = nameField.getText().trim();
                if (miNombre.isEmpty()) miNombre = "Jugador";

                String ip = ipField.getText().trim();
                if (ip.isEmpty()) ip = "127.0.0.1";

                System.out.println("Iniciando como CLIENTE. Nombre: " + miNombre + " | IP: " + ip);

                // NUEVO: Ahora pasamos el mapaSeleccionado[0] como 4to parámetro
                game.startNetworkGame(false, miNombre, ip, mapaSeleccionado[0]);
                dispose();
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
        table.add(titleLabel).padBottom(30).colspan(2).row();

        // Fila Nombre
        table.add(nameLabel).right().padRight(10).padBottom(20);
        table.add(nameField).width(200).height(40).padBottom(20).row();

        // NUEVO: Fila del Mapa
        table.add(btnMapa).width(350).height(50).padBottom(30).colspan(2).row();

        // Fila Host
        table.add(btnHost).width(250).height(50).padBottom(30).colspan(2).row();

        // Fila IP y Join
        table.add(ipLabel).right().padRight(10).padBottom(10);
        table.add(ipField).width(200).height(40).padBottom(10).row();
        table.add(btnJoin).width(250).height(50).padBottom(30).colspan(2).row();

        table.add(btnVolver).width(150).height(50).colspan(2).padTop(10);

        stage.addActor(table);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); skin.dispose(); }
}
