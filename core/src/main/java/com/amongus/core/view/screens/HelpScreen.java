package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class HelpScreen implements Screen {
    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;
    private Texture bgTexture;
    private Texture bgBoxTexture;

    public HelpScreen(AmongUsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        // 1. IMAGEN DE FONDO GLOBAL
        bgTexture = new Texture(Gdx.files.internal("ui/Imagen_Menu.png"));
        Image bgImage = new Image(bgTexture);
        bgImage.setSize(1280, 720);
        stage.addActor(bgImage);

        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));

        // 2. FUENTES MÁS PEQUEÑAS
        FreeTypeFontGenerator genRegular = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-regular.ttf"));
        FreeTypeFontParameter paramRegular = new FreeTypeFontParameter();
        paramRegular.size = 24; // ANTES 28 [cite: 556]
        paramRegular.minFilter = TextureFilter.Linear;
        paramRegular.magFilter = TextureFilter.Linear;
        BitmapFont fontRegular = genRegular.generateFont(paramRegular);
        genRegular.dispose();

        FreeTypeFontGenerator genBold = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-bold.ttf"));
        FreeTypeFontParameter paramBold = new FreeTypeFontParameter();
        paramBold.size = 40; // ANTES 50 [cite: 558]
        paramBold.minFilter = TextureFilter.Linear;
        paramBold.magFilter = TextureFilter.Linear;
        BitmapFont fontBold = genBold.generateFont(paramBold);
        genBold.dispose();

        // Aplicamos fuentes
        skin.add("font-regular", fontRegular, BitmapFont.class);
        skin.add("font-bold", fontBold, BitmapFont.class);
        skin.get(Label.LabelStyle.class).font = fontRegular;
        skin.get(Label.LabelStyle.class).fontColor = Color.WHITE; // Fuerza color blanco para los párrafos
        skin.get(TextButton.TextButtonStyle.class).font = fontRegular;

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        titleStyle.font = fontBold;
        titleStyle.fontColor = Color.WHITE;

        // 3. CREAR EL RECUADRO NEGRO SEMITRANSPARENTE
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.75f); // Negro al 75% de opacidad
        pixmap.fill();
        bgBoxTexture = new Texture(pixmap);
        pixmap.dispose();

        // 4. DOBLE TABLA (Raíz y Contenido)
        Table rootTable = new Table();
        rootTable.setFillParent(true); // Ocupa todo el 1280x720 para poder centrar

        Table contentTable = new Table();
        // Le asignamos el fondo oscuro
        contentTable.setBackground(new TextureRegionDrawable(bgBoxTexture));
        contentTable.pad(40f); // Un margen interno agradable para que el texto respire

        // ── Título ──
        Label titleLabel = new Label("COMO JUGAR", titleStyle);
        titleLabel.setAlignment(Align.center);

        // ── Reglas del Juego ──
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
        rulesLabel.setWrap(true);

        // ── Botón de regreso ──
        TextButton btnVolver = new TextButton("Volver", skin);
        btnVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // 5. AGREGAR A LA TABLA DE CONTENIDO (En lugar de la tabla principal)
        contentTable.add(titleLabel).padBottom(30).row();
        contentTable.add(rulesLabel).width(800).padBottom(40).row(); // Reduje un poco el ancho [cite: 569]
        contentTable.add(btnVolver).width(200).height(50); // Botón un poco más pequeño

        // 6. ENSAMBLAR
        rootTable.add(contentTable); // Centra el recuadro negro en la pantalla
        stage.addActor(rootTable);
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
    @Override public void dispose() { stage.dispose(); skin.dispose(); if (bgBoxTexture != null) bgBoxTexture.dispose();}
}
