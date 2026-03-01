package com.amongus.core.impl.minigame;


import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.task.Task;
import com.amongus.core.impl.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NumberCodeMinigameScreen extends AbstractMinigameScreen {

    private Stage stage;
    private Skin skin;                  // Skin para estilos de UI (botones, labels)
    private Label progressLabel;        // Muestra progreso: "Next: 4 / 10"
    private final List<TextButton> numberButtons = new ArrayList<>();
    private int currentExpected = 1;    // El siguiente número que debe pulsar (empieza en 1)

    public NumberCodeMinigameScreen(GameEngine engine, PlayerId playerId, Task task) {
        super(engine, playerId, task);
    }

    @Override
    public void show() {
        super.show();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // 1. LabelStyle
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // 2. TextButtonStyle
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pm.setColor(Color.DARK_GRAY); pm.fill();
        TextureRegionDrawable upDrawable = new TextureRegionDrawable(new Texture(pm));

        pm.setColor(Color.GRAY); pm.fill();
        TextureRegionDrawable downDrawable = new TextureRegionDrawable(new Texture(pm));

        pm.setColor(Color.LIGHT_GRAY); pm.fill();
        TextureRegionDrawable overDrawable = new TextureRegionDrawable(new Texture(pm));

        pm.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up   = upDrawable;
        buttonStyle.down = downDrawable;
        buttonStyle.over = overDrawable;
        skin.add("default", buttonStyle);

        // 3. Ahora sí crear la UI
        createUI();
        randomizeButtonPositions();
    }

    private void createUI() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Label de instrucción (arriba)
        // Instrucción: "Click in ascending order"
        Label instructionLabel = new Label("Click the numbers in ascending order", skin);
        instructionLabel.setFontScale(1.5f);
        instructionLabel.setPosition(screenWidth / 2f, screenHeight - 100, Align.center);
        stage.addActor(instructionLabel);

        // Label de progreso (debajo del título)
        progressLabel = new Label("Next: 1 / 10", skin);
        progressLabel.setFontScale(1.2f);
        progressLabel.setPosition(screenWidth / 2f, screenHeight - 180, Align.center);
        stage.addActor(progressLabel);

        // Crear los 10 botones (1 al 10)
        TextButton.TextButtonStyle style = skin.get("default", TextButton.TextButtonStyle.class);
        for (int i = 1; i <= 10; i++) {
            TextButton button = new TextButton(String.valueOf(i), style);
            button.setSize(120, 120); // Tamaño de cada botón

            // Listener para cuando se pulsa
            final int number = i;
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onNumberClicked(number);
                }
            });

            numberButtons.add(button);
            stage.addActor(button);
        }
    }

    private void randomizeButtonPositions() {
        // Mezclar la lista de botones para orden aleatorio
        Collections.shuffle(numberButtons);

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Grid 2 filas x 5 columnas
        float buttonSize = 120;
        float spacing = 40;
        float startX = (screenWidth - (5 * buttonSize + 4 * spacing)) / 2f;
        float startYTop = screenHeight / 2f + 50;    // Fila superior
        float startYBottom = screenHeight / 2f - 170; // Fila inferior

        int index = 0;
        // Fila superior (5 botones)
        for (int col = 0; col < 5; col++) {
            TextButton btn = numberButtons.get(index++);
            btn.setPosition(startX + col * (buttonSize + spacing), startYTop);
        }

        // Fila inferior (5 botones)
        for (int col = 0; col < 5; col++) {
            TextButton btn = numberButtons.get(index++);
            btn.setPosition(startX + col * (buttonSize + spacing), startYBottom);
        }
    }

    private void onNumberClicked(int clickedNumber) {
        if (clickedNumber == currentExpected) {
            // Acierto
            currentExpected++;
            progressLabel.setText("Next: " + currentExpected + " / 10");

            if (currentExpected > 10) {
                complete(); // ¡Victoria!
            }
        } else {
            // Fallo: reiniciar contador
            currentExpected = 1;
            progressLabel.setText("Error! Starting over... Next: 1 / 10");

            // Aquí podrías añadir efectos: shake de pantalla, sonido, etc.
        }
    }

    @Override
    protected void renderContent(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        // Reposicionar elementos si quieres que se adapten al resize
        randomizeButtonPositions(); // Opcional: reordenar al cambiar tamaño
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        super.dispose();
    }

    // Si quieres versión con assets personalizados por número (opcional)
    private void setCustomBackground(TextButton button, int number) {
        // Ejemplo: si tienes boton1.png ... boton10.png
        // Texture tex = new Texture("minigames/boton" + number + ".png");
        // button.getStyle().up = new TextureRegionDrawable(tex);
        // (Cargar y dispose en show/dispose)
    }
}
