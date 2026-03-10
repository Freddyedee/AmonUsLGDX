package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MainMenuScreen implements Screen {

    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;
    private Texture bgTexture;

    public MainMenuScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        // 1. Agregar la imagen de fondo al fondo del Stage
        bgTexture = new Texture(Gdx.files.internal("ui/Imagen_Menu.png")); // Ruta del fondo de pantalla
        Image bgImage = new com.badlogic.gdx.scenes.scene2d.ui.Image(bgTexture);
        bgImage.setSize(1280, 720);
        stage.addActor(bgImage); // Se añade de primero para que quede atrás

        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        // ── GENERACIÓN DE FUENTES ──
        FreeTypeFontGenerator genRegular = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-regular.ttf"));
        FreeTypeFontParameter paramRegular = new FreeTypeFontParameter();
        paramRegular.size = 28;
        paramRegular.minFilter = TextureFilter.Linear;
        paramRegular.magFilter = TextureFilter.Linear;
        BitmapFont fontRegular = genRegular.generateFont(paramRegular);
        genRegular.dispose();

        // Aplicamos las fuentes al Skin
        skin.add("font-regular", fontRegular, BitmapFont.class);
        skin.get(TextButton.TextButtonStyle.class).font = fontRegular;
        skin.get(Label.LabelStyle.class).font = fontRegular;

        Table table = new Table();
        table.setFillParent(true);

        // 2. Alinear a la izquierda
        table.align(Align.left);
        table.padLeft(60f);

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
                game.setScreen(new PlayMenuScreen(game));
                dispose();
            }
        });

        btnAyuda.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new HelpScreen(game));
                dispose();
            }
        });

        btnAcercaDe.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new AboutScreen(game));
                dispose();
            }
        });

        btnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SettingsScreen(game));
                dispose();
            }
        });

        btnSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // ── Organizando los botones en la tabla ──
        float btnWidth = 350f;
        float btnHeight = 60f;
        float padding = 15f;

        table.add(btnJugar).width(btnWidth).height(btnHeight).padBottom(padding).row();
        table.add(btnAyuda).width(btnWidth).height(btnHeight).padBottom(padding).row();
        table.add(btnAcercaDe).width(btnWidth).height(btnHeight).padBottom(padding).row();
        table.add(btnSettings).width(btnWidth).height(btnHeight).padBottom(padding).row();
        table.add(btnSalir).width(btnWidth).height(btnHeight).padBottom(padding).row();

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1); // Fondo unificado con el resto de pantallas
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); skin.dispose(); if(bgTexture != null) bgTexture.dispose();}
}
