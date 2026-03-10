package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.amongus.core.api.map.MapType;
import com.amongus.core.utils.SettingsManager;
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

public class PlayMenuScreen implements Screen {
    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;
    private Texture bgTexture;
    private Texture bgBoxTexture;

    public PlayMenuScreen(AmongUsGame game) {
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

        // 2. FUENTES
        FreeTypeFontGenerator genRegular = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-regular.ttf"));
        FreeTypeFontParameter paramRegular = new FreeTypeFontParameter();
        paramRegular.size = 24;
        paramRegular.minFilter = TextureFilter.Linear;
        paramRegular.magFilter = TextureFilter.Linear;
        BitmapFont fontRegular = genRegular.generateFont(paramRegular);
        genRegular.dispose();

        FreeTypeFontGenerator genBold = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-bold.ttf"));
        FreeTypeFontParameter paramBold = new FreeTypeFontParameter();
        paramBold.size = 40;
        paramBold.minFilter = TextureFilter.Linear;
        paramBold.magFilter = TextureFilter.Linear;
        BitmapFont fontBold = genBold.generateFont(paramBold);
        genBold.dispose();

        // Aplicamos fuentes al Skin
        skin.add("font-regular", fontRegular, BitmapFont.class);
        skin.add("font-bold", fontBold, BitmapFont.class);
        skin.get(Label.LabelStyle.class).font = fontRegular;
        skin.get(Label.LabelStyle.class).fontColor = Color.WHITE;
        skin.get(TextButton.TextButtonStyle.class).font = fontRegular;
        skin.get(TextField.TextFieldStyle.class).font = fontRegular;

        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        titleStyle.font = fontBold;

        // 3. CREAR EL RECUADRO NEGRO SEMITRANSPARENTE
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.75f);
        pixmap.fill();
        bgBoxTexture = new Texture(pixmap);
        pixmap.dispose();

        Table rootTable = new Table();
        rootTable.setFillParent(true);

        Table contentTable = new Table();
        contentTable.setBackground(new TextureRegionDrawable(bgBoxTexture));
        contentTable.pad(40f);

        // ── Elementos de la UI ──
        Label titleLabel = new Label("MODO MULTIJUGADOR", titleStyle);
        titleLabel.setAlignment(Align.center);

        // Botón Host
        TextButton btnHost = new TextButton("Crear Partida (Host)", skin);

        // Área de conexión por IP
        Label ipLabel = new Label("IP del Host:", skin);
        TextField ipField = new TextField("127.0.0.1", skin);
        ipField.setAlignment(Align.center);

        TextButton btnJoin = new TextButton("Conectarse a IP", skin);
        TextButton btnVolver = new TextButton("Volver", skin);

        // ── Listeners ──
        btnHost.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String miNombre = SettingsManager.playerName;
                game.startNetworkGame(true, miNombre, "localhost", MapType.MAPA_1);
                dispose();
            }
        });

        btnJoin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String miNombre = SettingsManager.playerName;
                String ip = ipField.getText().trim();
                if (ip.isEmpty()) ip = "127.0.0.1";
                game.startNetworkGame(false, miNombre, ip, MapType.MAPA_1);
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

        // ── Organización visual ──
        contentTable.add(titleLabel).padBottom(50).colspan(2).row();
        contentTable.add(btnHost).width(350).height(60).padBottom(40).colspan(2).row();
        contentTable.add(ipLabel).right().padRight(15).padBottom(15);
        contentTable.add(ipField).width(300).height(50).padBottom(15).row();
        contentTable.add(btnJoin).width(350).height(60).padBottom(40).colspan(2).row();
        contentTable.add(btnVolver).width(250).height(60).colspan(2).padTop(10);

        // ENSAMBLAR
        rootTable.add(contentTable);
        stage.addActor(rootTable);
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
