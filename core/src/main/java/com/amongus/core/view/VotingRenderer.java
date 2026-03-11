package com.amongus.core.view;

import com.amongus.core.api.player.PlayerId;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import java.util.Set;

public class VotingRenderer {
    private final Texture imgTablet;
    private final Texture imgSkipButton;
    private final Texture imgIVoted;
    private final Texture imgMegafono;
    private final Texture imgSkippedVoting;
    private final Texture imgRectangulo;

    // Usaremos dos fuentes nítidas en lugar de escalar la de defecto
    private final BitmapFont fontNormal;
    private final BitmapFont fontTitle;

    public VotingRenderer() {
        imgTablet = new Texture(Gdx.files.internal("ui/voting/voting_tablet.png"));
        imgSkipButton = new Texture(Gdx.files.internal("ui/voting/Skip.png"));
        imgIVoted = new Texture(Gdx.files.internal("ui/voting/Ivoted.png"));
        imgMegafono = new Texture(Gdx.files.internal("ui/voting/Megafono.png"));
        imgSkippedVoting = new Texture(Gdx.files.internal("ui/voting/SkippedVoting.png"));
        imgRectangulo = new Texture(Gdx.files.internal("ui/voting/RectanguloBlanco.png"));

        // --- GENERACIÓN DE FUENTES FREETYPE DE ALTA CALIDAD ---
        // Usamos la fuente que ya tenías en el menú. Si quieres otra, solo cambia la ruta del .ttf
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-bold.ttf"));

        // 1. Fuente Normal (Para nombres e instrucciones)
        FreeTypeFontParameter paramNormal = new FreeTypeFontParameter();
        paramNormal.size = 60; // Equivalente a tu antiguo scale(3.5f) pero en HD
        paramNormal.color = Color.WHITE;
        paramNormal.minFilter = TextureFilter.Linear;
        paramNormal.magFilter = TextureFilter.Linear;
        fontNormal = generator.generateFont(paramNormal);

        // 2. Fuente Título (Gigante para "REUNIÓN DE EMERGENCIA")
        FreeTypeFontParameter paramTitle = new FreeTypeFontParameter();
        paramTitle.size = 110;
        paramTitle.color = Color.WHITE;
        paramTitle.minFilter = TextureFilter.Linear;
        paramTitle.magFilter = TextureFilter.Linear;
        // Añadimos un borde negro sutil para que resalte más
        paramTitle.borderColor = Color.BLACK;
        paramTitle.borderWidth = 3f;
        fontTitle = generator.generateFont(paramTitle);

        generator.dispose(); // Importante liberar el generador
    }

    public void draw(SpriteBatch batch, GameSnapshot snapshot, PlayerId reporterId,
                     float timer, Set<PlayerId> votedPlayers,
                     boolean showingResults, boolean skipped, boolean isEmergency) {

        // --- FORZAR TAMAÑO 4K PARA LA TABLET ---
        float tabletW = 3200f;
        float tabletH = 1800f;
        float tabletX = (3840f - tabletW) / 2f;
        float tabletY = (2160f - tabletH) / 2f;

        batch.draw(imgTablet, tabletX, tabletY, tabletW, tabletH);

        // --- TÍTULO DE LA REUNIÓN (Arriba a la Derecha) ---
        fontTitle.setColor(isEmergency ? Color.RED : Color.CYAN);
        String titulo = isEmergency ? "REUNION DE EMERGENCIA" : "CUERPO REPORTADO";

        // Lo alineamos a la derecha, dándole un margen de 150px desde el borde derecho de la tablet
        float titleX = tabletX + tabletW + 20f;
        float titleY = tabletY + tabletH + 100f;
        fontTitle.draw(batch, titulo, titleX, titleY, 0, Align.right, false);

        // --- TEXTOS SUPERIORES ---
        fontNormal.setColor(Color.WHITE);
        String instruccion;
        if (votedPlayers.contains(snapshot.getLocalPlayerId())) {
            instruccion = "Ya has votado. Esperando a los demas...";
        } else if (timer < 15f) {
            instruccion = "CONTROLES: [1] al [" + snapshot.getPlayers().size() + "] para Votar (Bloqueado)  |  [S] para Skip (Bloqueado)";
        } else {
            instruccion = "CONTROLES: Teclas [1] al [" + snapshot.getPlayers().size() + "] para Votar  |  Tecla [S] para Skip";
        }

        fontNormal.draw(batch, instruccion, 150, 2100);

        String tiempoInfo = showingResults ? "Reunion Finalizada" : (timer < 15f ? "Tiempo de discusion: " + (int)(15 - timer) + "s" : "Tiempo para votar: " + (int)(60 - timer) + "s");
        fontNormal.draw(batch, tiempoInfo, 150, 2020);

        // --- DIBUJAR JUGADORES ---
        List<PlayerView> players = snapshot.getPlayers();

        float rectW = 1200f;
        float rectH = 180f;
        float paddingX = 150f;
        float paddingY = 60f;

        float startX = tabletX + 250f;
        float startY = tabletY + tabletH - 350f;

        for (int i = 0; i < players.size(); i++) {
            PlayerView pv = players.get(i);
            int col = i % 2;
            int row = i / 2;

            float x = startX + col * (rectW + paddingX);
            float y = startY - row * (rectH + paddingY);

            // 1. Rectángulo Blanco
            batch.setColor(pv.isAlive() ? Color.WHITE : Color.LIGHT_GRAY);
            batch.draw(imgRectangulo, x, y, rectW, rectH);
            batch.setColor(Color.WHITE);

            // 2. Megáfono
            if (pv.getId().equals(reporterId)) {
                batch.draw(imgMegafono, x - 60, y + 30, 120, 120);
            }

            // 3. Escribir el Nombre
            // Si está vivo, negro. Si está muerto, un rojo oscuro para que se note la baja.
            fontNormal.setColor(pv.isAlive() ? Color.BLACK : new Color(0.5f, 0f, 0f, 1f));
            String text = (i + 1) + ". " + pv.getName() + (pv.getId().equals(snapshot.getLocalPlayerId()) ? " (Tu)" : "");
            if(!pv.isAlive()) text += " [INHABILITADO]"; // Un recordatorio extra
            fontNormal.draw(batch, text, x + 80, y + rectH / 2f + 20, rectW - 100, Align.left, false);

            // 4. I Voted
            if (votedPlayers.contains(pv.getId())) {
                batch.draw(imgIVoted, x + rectW - 150, y + 30, 120, 120);
            }
        }

        // --- BOTÓN SKIP ---
        batch.setColor(Color.WHITE);
        batch.draw(imgSkipButton, tabletX + tabletW - 950, tabletY + 120, 600, 200);

        // --- PANTALLA DE RESULTADOS ---
        if (showingResults && skipped) {
            batch.draw(imgSkippedVoting, (3840f - 1500f) / 2f, (2160f - 800f) / 2f, 1500f, 800f);
        }
    }

    public void dispose() {
        fontNormal.dispose();
        fontTitle.dispose();
        imgTablet.dispose();
        imgSkipButton.dispose();
        imgIVoted.dispose();
        imgMegafono.dispose();
        imgSkippedVoting.dispose();
        imgRectangulo.dispose();
    }
}
