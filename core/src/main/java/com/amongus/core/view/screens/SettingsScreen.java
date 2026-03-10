package com.amongus.core.view.screens;

import com.amongus.core.AmongUsGame;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.utils.SettingsManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class SettingsScreen implements Screen {

    private final AmongUsGame game;
    private Stage stage;
    private Skin skin;
    private Texture bgTexture;
    private Texture bgBoxTexture;

    public SettingsScreen(AmongUsGame game) {
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

        // ── 3. APLICAR LAS FUENTES A LA UI ──

        // Registramos las fuentes en el recurso del skin para que sean compatibles
        skin.add("font-regular", fontRegular, BitmapFont.class);
        skin.add("font-bold", fontBold, BitmapFont.class);
        skin.get(Label.LabelStyle.class).font = fontRegular;
        skin.get(Label.LabelStyle.class).fontColor = Color.WHITE;
        skin.get(TextButton.TextButtonStyle.class).font = fontRegular;
        skin.get(TextField.TextFieldStyle.class).font = fontRegular;

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

        // ── Elementos de la UI ──

        // Creamos un estilo único para el título que use la fuente BOLD
        Label.LabelStyle titleStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        titleStyle.font = fontBold;
        titleStyle.fontColor = Color.WHITE;

        Label titleLabel = new Label("CONFIGURACION", titleStyle);
        // Eliminamos el setFontScale(1.5f) porque ahora es de tamaño 50 nativo
        titleLabel.setAlignment(Align.center);

        // --- Área del Nombre ---
        Label nameLabel = new Label("Nombre del Jugador:", skin);
        // Cargamos el nombre directamente del SettingsManager
        TextField nameField = new TextField(SettingsManager.playerName, skin);
        nameField.setAlignment(Align.center);

        Label resLabel = new Label("Resolucion de Pantalla:", skin);

        TextButton btnRes1 = new TextButton("800 x 600 (Ventana)", skin);
        TextButton btnRes2 = new TextButton("1280 x 720 (HD)", skin);
        TextButton btnRes3 = new TextButton("1920 x 1080 (Full HD)", skin);

        TextButton btnVolver = new TextButton("Guardar y Volver", skin);

        // ── Listeners de Botones ──
        btnRes1.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                SettingsManager.width = 800;
                SettingsManager.height = 600;
                SettingsManager.applyResolution();
            }
        });

        btnRes2.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                SettingsManager.width = 1280;
                SettingsManager.height = 720;
                SettingsManager.applyResolution();
            }
        });

        btnRes3.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                SettingsManager.width = 1920;
                SettingsManager.height = 1080;
                SettingsManager.applyResolution();
            }
        });

        // --- Área del Color ---
        Label colorLabel = new Label("Color del Personaje:", skin);
        colorLabel.setAlignment(Align.center);

        final SkinColor[] colores = SkinColor.values();
        final int[] colorIndex = {0};

        // Buscar qué índice tiene guardado actualmente
        for(int i=0; i<colores.length; i++) {
            if(colores[i].name().equals(SettingsManager.playerColor)) colorIndex[0] = i;
        }

        TextButton btnColor = new TextButton("Color: " + colores[colorIndex[0]].name(), skin);

        btnColor.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // Ciclar al siguiente color
                colorIndex[0] = (colorIndex[0] + 1) % colores.length;
                btnColor.setText("Color: " + colores[colorIndex[0]].name());
            }
        });

        btnVolver.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String newName = nameField.getText().trim();
                if(newName.isEmpty()) newName = "Jugador";

                // Actualizamos el nombre en memoria y escribimos el XML
                SettingsManager.playerName = newName;
                SettingsManager.playerColor = colores[colorIndex[0]].name();
                SettingsManager.save();

                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        // ── Organización visual en la tabla ──
        contentTable.add(titleLabel).padBottom(30).colspan(2).row();

        // Agregamos el área de nombre a la tabla
        contentTable.add(nameLabel).padBottom(5).row();
        contentTable.add(nameField).width(300).height(50).padBottom(20).row();
        contentTable.add(btnColor).width(300).height(50).padBottom(20).row();
        contentTable.add(colorLabel).row();
        contentTable.add(resLabel).padBottom(10).row();
        contentTable.add(btnRes1).width(300).height(50).padBottom(10).row();
        contentTable.add(btnRes2).width(300).height(50).padBottom(10).row();
        contentTable.add(btnRes3).width(300).height(50).padBottom(30).row();

        contentTable.add(btnVolver).width(250).height(50).padTop(10);

        // 5. ENSAMBLAR TODO
        rootTable.add(contentTable); // Metemos el cuadro oscuro centrado en la pantalla
        stage.addActor(rootTable);   // Metemos la pantalla al Stage
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
