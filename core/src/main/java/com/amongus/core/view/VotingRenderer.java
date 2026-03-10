package com.amongus.core.view;

import com.amongus.core.api.player.PlayerId;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    private final BitmapFont font;

    public VotingRenderer() {
        imgTablet = new Texture(Gdx.files.internal("ui/voting/voting_tablet.png"));
        imgSkipButton = new Texture(Gdx.files.internal("ui/voting/Skip.png"));
        imgIVoted = new Texture(Gdx.files.internal("ui/voting/Ivoted.png"));
        imgMegafono = new Texture(Gdx.files.internal("ui/voting/Megafono.png"));
        imgSkippedVoting = new Texture(Gdx.files.internal("ui/voting/SkippedVoting.png"));
        imgRectangulo = new Texture(Gdx.files.internal("ui/voting/RectanguloBlanco.png"));
        font = new BitmapFont();
        font.getData().setScale(3.5f);
    }

    public void draw(SpriteBatch batch, GameSnapshot snapshot, PlayerId reporterId,
                     float timer, Set<PlayerId> votedPlayers,
                     boolean showingResults, boolean skipped) {

        // --- FORZAR TAMAÑO 4K PARA LA TABLET ---
        float tabletW = 3200f;
        float tabletH = 1800f;
        float tabletX = (3840f - tabletW) / 2f; // Centrado horizontal
        float tabletY = (2160f - tabletH) / 2f; // Centrado vertical

        batch.draw(imgTablet, tabletX, tabletY, tabletW, tabletH);

        // --- TEXTOS SUPERIORES ---
        font.setColor(Color.WHITE);
        String instruccion;
        if (votedPlayers.contains(snapshot.getLocalPlayerId())) {
            instruccion = "Ya has votado. Esperando a los demas...";
        } else if (timer < 15f) {
            instruccion = "CONTROLES: [1] al [" + snapshot.getPlayers().size() + "] para Votar (Bloqueado)  |  [S] para Skip (Bloqueado)";
        } else {
            instruccion = "CONTROLES: Teclas [1] al [" + snapshot.getPlayers().size() + "] para Votar  |  Tecla [S] para Skip";
        }

        // Textos alineados arriba a la izquierda de la pantalla
        font.draw(batch, instruccion, 150, 2100);

        String tiempoInfo = showingResults ? "Reunion Finalizada" : (timer < 15f ? "Tiempo de discusion: " + (int)(15 - timer) + "s" : "Tiempo para votar: " + (int)(60 - timer) + "s");
        font.draw(batch, tiempoInfo, 150, 2020);

        // --- DIBUJAR JUGADORES (CUADRÍCULA DENTRO DE LA TABLET) ---
        List<PlayerView> players = snapshot.getPlayers();

        // Tamaño forzado para los rectángulos blancos
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

            // 1. Dibujar el Rectángulo Blanco
            batch.setColor(pv.isAlive() ? Color.WHITE : Color.LIGHT_GRAY);
            batch.draw(imgRectangulo, x, y, rectW, rectH);
            batch.setColor(Color.WHITE);

            // 2. Icono de Megáfono
            if (pv.getId().equals(reporterId)) {
                batch.draw(imgMegafono, x - 60, y + 30, 120, 120);
            }

            // 3. Escribir el Nombre
            font.setColor(Color.BLACK);
            String text = (i + 1) + ". " + pv.getName() + (pv.getId().equals(snapshot.getLocalPlayerId()) ? " (Tu)" : "");
            font.draw(batch, text, x + 80, y + rectH / 2f + 25, rectW - 100, Align.left, false);

            // 4. Icono de "I Voted"
            if (votedPlayers.contains(pv.getId())) {
                batch.draw(imgIVoted, x + rectW - 150, y + 30, 120, 120);
            }


        }

        // --- BOTÓN SKIP ---
        batch.setColor(Color.WHITE);
        batch.draw(imgSkipButton, tabletX + tabletW - 600, tabletY + 100, 600, 200);

        // --- PANTALLA DE RESULTADOS ---
        if (showingResults && skipped) {
            batch.draw(imgSkippedVoting, (3840f - 1500f) / 2f, (2160f - 800f) / 2f, 1500f, 800f);
        }
    }

    public void dispose() {
        font.dispose();
        imgTablet.dispose();
        imgSkipButton.dispose();
        imgIVoted.dispose();
        imgMegafono.dispose();
        imgSkippedVoting.dispose();
        imgRectangulo.dispose();
    }
}
