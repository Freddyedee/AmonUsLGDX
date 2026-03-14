package com.amongus.core.view;

import com.amongus.core.api.player.PlayerId;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class VotingRenderer {
    private final Texture imgTablet;
    private final Texture imgSkipButton;
    private final Texture imgIVoted;
    private final Texture imgMegafono;
    private final Texture imgSkippedVoting;
    private final Texture imgRectangulo;

    private final Texture imgConfirmar;
    private final Texture imgRechazar;

    private final BitmapFont fontNormal;
    private final BitmapFont fontTitle;

    // MAPAS PARA GUARDAR LAS ZONAS CLICABLES
    public final Map<PlayerId, Rectangle> playerHitboxes = new HashMap<>();
    public Rectangle skipHitbox;
    public Rectangle btnConfirmarHitbox;
    public Rectangle btnRechazarHitbox;

    public boolean isChatOpen = false;
    public Rectangle btnChatToggleHitbox;
    public Rectangle btnChatWriteHitbox;

    // Integración de Scene2D para el chat
    public Stage stage;
    public Skin skin;
    public TextField chatField;
    private String pendingMessage = null;

    public VotingRenderer() {
        imgTablet = new Texture(Gdx.files.internal("ui/voting/voting_tablet.png"));
        imgSkipButton = new Texture(Gdx.files.internal("ui/voting/Skip.png"));
        imgIVoted = new Texture(Gdx.files.internal("ui/voting/Ivoted.png"));
        imgMegafono = new Texture(Gdx.files.internal("ui/voting/Megafono.png"));
        imgSkippedVoting = new Texture(Gdx.files.internal("ui/voting/SkippedVoting.png"));
        imgRectangulo = new Texture(Gdx.files.internal("ui/voting/RectanguloBlanco.png"));

        // 👇 CARGAR TUS NUEVAS TEXTURAS
        imgConfirmar = new Texture(Gdx.files.internal("ui/voting/Confirmar.png"));
        imgRechazar = new Texture(Gdx.files.internal("ui/voting/Rechazar.png"));

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-bold.ttf"));

        FreeTypeFontParameter paramNormal = new FreeTypeFontParameter();
        paramNormal.size = 60;
        paramNormal.color = Color.WHITE;
        paramNormal.minFilter = TextureFilter.Linear;
        paramNormal.magFilter = TextureFilter.Linear;
        fontNormal = generator.generateFont(paramNormal);

        FreeTypeFontParameter paramTitle = new FreeTypeFontParameter();
        paramTitle.size = 110;
        paramTitle.color = Color.WHITE;
        paramTitle.minFilter = TextureFilter.Linear;
        paramTitle.magFilter = TextureFilter.Linear;
        paramTitle.borderColor = Color.BLACK;
        paramTitle.borderWidth = 3f;
        fontTitle = generator.generateFont(paramTitle);

        // --- CHAT SCENE2D UI ---
        skin = new Skin(Gdx.files.internal("ui/comic/comic-ui.json"));
        FreeTypeFontParameter paramChat = new FreeTypeFontParameter();
        paramChat.size = 50;
        paramChat.color = Color.DARK_GRAY; // Texto oscuro como en Among Us
        paramChat.minFilter = TextureFilter.Linear;
        paramChat.magFilter = TextureFilter.Linear;
        BitmapFont fontChat = generator.generateFont(paramChat);

        skin.add("font-chat", fontChat, BitmapFont.class);
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        tfStyle.font = fontChat;
        tfStyle.fontColor = Color.DARK_GRAY;

        stage = new Stage(new FitViewport(3840, 2160));
        chatField = new TextField("", tfStyle);
        chatField.setMessageText("Escribe y presiona ENTER...");

        float chatW = 1600f;
        float chatH = 1400f;
        float chatX = (3840f - chatW) / 2f;
        float chatY = (2160f - chatH) / 2f;

        chatField.setSize(chatW - 80f, 120f);
        chatField.setPosition(chatX + 40f, chatY + 40f);
        stage.addActor(chatField);

        chatField.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                if (c == '\r' || c == '\n') {
                    String text = textField.getText().trim();
                    if (!text.isEmpty()) {
                        pendingMessage = text;
                        textField.setText("");
                    }
                }
            }
        });

        generator.dispose();
    }

    public String getAndClearPendingMessage() {
        String msg = pendingMessage;
        pendingMessage = null;
        return msg;
    }

    public void draw(SpriteBatch batch, GameSnapshot snapshot, PlayerId reporterId,
                     float timer, Set<PlayerId> votedPlayers,
                     boolean showingResults, boolean skipped, boolean isEmergency,
                     PlayerId selectedPlayerId, java.util.List<ChatMessage> chatMessages) {

        // Limpiamos los hitboxes del frame anterior
        playerHitboxes.clear();
        btnConfirmarHitbox = null;
        btnRechazarHitbox = null;
        btnChatToggleHitbox = null;
        btnChatWriteHitbox = null;

        float tabletW = 3200f;
        float tabletH = 1800f;
        float tabletX = (3840f - tabletW) / 2f;
        float tabletY = (2160f - tabletH) / 2f;

        batch.draw(imgTablet, tabletX, tabletY, tabletW, tabletH);

        // --- TÍTULO ---
        fontTitle.setColor(isEmergency ? Color.RED : Color.CYAN);
        String titulo = isEmergency ? "REUNION DE EMERGENCIA" : "CUERPO REPORTADO";
        float titleX = tabletX + tabletW + 20f;
        float titleY = tabletY + tabletH + 100f;
        fontTitle.draw(batch, titulo, titleX, titleY, 0, Align.right, false);

        // --- TEXTOS SUPERIORES ---
        fontNormal.setColor(Color.WHITE);
        String instruccion;
        if (votedPlayers.contains(snapshot.getLocalPlayerId())) {
            instruccion = "Ya votaste.";
        } else if (timer < 15f) {
            instruccion = "Discusion en progreso.";
        } else {
            instruccion = "Votacion Abierta.";
        }
        fontNormal.draw(batch, instruccion, tabletX, tabletY + tabletH + 40f);

        String tiempoInfo = showingResults ? "Reunion Finalizada" : (timer < 15f ? "Discusion: " + (int) (15 - timer) + "s" : "Vota: " + (int) (60 - timer) + "s");
        fontNormal.draw(batch, tiempoInfo, tabletX + 600f, tabletY + tabletH + 40f);

        // --- DIBUJAR JUGADORES ---
        List<PlayerView> players = snapshot.getPlayers();
        // Validar si el jugador local está vivo para bloquear/desbloquear el chat
        boolean localIsAlive = players.stream()
            .filter(p -> p.getId().equals(snapshot.getLocalPlayerId()))
            .map(PlayerView::isAlive)
            .findFirst()
            .orElse(false);

        if (!localIsAlive) {
            chatField.setDisabled(true); // Bloquea la escritura
            chatField.setMessageText("Los inhabilitados no pueden hablar...");
        } else {
            chatField.setDisabled(false);
            chatField.setMessageText("Escribe y presiona ENTER...");
        }
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

            // Guardamos la zona clicable
            if (pv.isAlive()) {
                playerHitboxes.put(pv.getId(), new Rectangle(x, y, rectW, rectH));
            }

            // Si está seleccionado, oscurecemos un poco el fondo
            boolean isSelected = pv.getId().equals(selectedPlayerId);
            if (pv.isAlive()) {
                batch.setColor(isSelected ? Color.GRAY : Color.WHITE);
            } else {
                batch.setColor(Color.DARK_GRAY);
            }
            batch.draw(imgRectangulo, x, y, rectW, rectH);
            batch.setColor(Color.WHITE);

            // Megáfono
            if (pv.getId().equals(reporterId)) {
                batch.draw(imgMegafono, x - 60, y + 30, 120, 120);
            }

            // Nombre
            fontNormal.setColor(pv.isAlive() ? Color.BLACK : new Color(0.5f, 0f, 0f, 1f));
            String text = (i + 1) + ". " + pv.getName() + (pv.getId().equals(snapshot.getLocalPlayerId()) ? " (Tu)" : "");
            if (!pv.isAlive()) text += " [INHABILITADO]";
            fontNormal.draw(batch, text, x + 80, y + rectH / 2f + 20, rectW - 100, Align.left, false);

            // I Voted
            if (votedPlayers.contains(pv.getId())) {
                batch.draw(imgIVoted, x + rectW - 150, y + 30, 120, 120);
            }

            // DIBUJAR BOTONES DE CONFIRMAR/RECHAZAR SI ESTÁ SELECCIONADO
            if (isSelected && timer >= 15f && !votedPlayers.contains(snapshot.getLocalPlayerId())) {
                float btnSize = 120f;
                float btnY = y + (rectH - btnSize) / 2f; // Centrado verticalmente

                // Botón verde (Confirmar) a la derecha
                float confirmarX = x + rectW - btnSize - 20f;
                batch.draw(imgConfirmar, confirmarX, btnY, btnSize, btnSize);
                btnConfirmarHitbox = new Rectangle(confirmarX, btnY, btnSize, btnSize);

                // Botón rojo (Rechazar) un poco más a la izquierda
                float rechazarX = confirmarX - btnSize - 20f;
                batch.draw(imgRechazar, rechazarX, btnY, btnSize, btnSize);
                btnRechazarHitbox = new Rectangle(rechazarX, btnY, btnSize, btnSize);
            }
        }

        // --- BOTÓN SKIP ---
        float skipX = tabletX + tabletW - 950;
        float skipY = tabletY + 120;
        float skipW = 600;
        float skipH = 200;

        // Efecto visual si está "seleccionado" (opcional)
        boolean isSkipSelected = selectedPlayerId != null && selectedPlayerId.value().toString().equals("00000000-0000-0000-0000-000000000000"); // Usaremos este ID falso para representar el Skip
        batch.setColor(isSkipSelected ? Color.GRAY : Color.WHITE);
        batch.draw(imgSkipButton, skipX, skipY, skipW, skipH);
        batch.setColor(Color.WHITE);

        skipHitbox = new Rectangle(skipX, skipY, skipW, skipH);

        // Si el Skip está seleccionado, le ponemos los botones encima
        if (isSkipSelected && timer >= 15f && !votedPlayers.contains(snapshot.getLocalPlayerId())) {
            float btnSize = 100f;
            float btnY = skipY + (skipH - btnSize) / 2f;

            float confirmarX = skipX + skipW - btnSize - 10f;
            batch.draw(imgConfirmar, confirmarX, btnY, btnSize, btnSize);
            btnConfirmarHitbox = new Rectangle(confirmarX, btnY, btnSize, btnSize);

            float rechazarX = confirmarX - btnSize - 10f;
            batch.draw(imgRechazar, rechazarX, btnY, btnSize, btnSize);
            btnRechazarHitbox = new Rectangle(rechazarX, btnY, btnSize, btnSize);
        }

        // --- PANTALLA DE RESULTADOS ---
        if (showingResults && skipped) {
            batch.draw(imgSkippedVoting, (3840f - 1500f) / 2f, (2160f - 800f) / 2f, 1500f, 800f);
        }

        // --- BOTON TOGGLE CHAT ---
        float btnChatW = 200f;
        float btnChatH = 200f;
        float btnChatX = tabletX + tabletW - 250f;
        float btnChatY = tabletY + tabletH - 250f;
        batch.setColor(Color.LIGHT_GRAY);
        batch.draw(imgRectangulo, btnChatX, btnChatY, btnChatW, btnChatH);
        batch.setColor(Color.WHITE);
        fontNormal.draw(batch, "CHAT", btnChatX + 40f, btnChatY + btnChatH / 2f + 20f);
        btnChatToggleHitbox = new Rectangle(btnChatX, btnChatY, btnChatW, btnChatH);

        // --- DIBUJAR CHAT (Si está abierto) ---
        if (isChatOpen) {
            drawChatOverlay(batch, chatMessages);
        }
    }

    // Dibuja solo el chat para poder usarlo en cualquier pantalla
    public void drawChatOverlay(SpriteBatch batch, java.util.List<ChatMessage> chatMessages) {
        float chatW = 1600f;
        float chatH = 1400f;
        float chatX = (3840f - chatW) / 2f;
        float chatY = (2160f - chatH) / 2f;

        // Fondo oscuro
        batch.setColor(0f, 0f, 0f, 0.7f);
        batch.draw(imgRectangulo, 0, 0, 3840f, 2160f);

        // Fondo del chat
        batch.setColor(0f, 0f, 0f, 0.9f);
        batch.draw(imgRectangulo, chatX, chatY, chatW, chatH);
        batch.setColor(Color.WHITE);

        fontNormal.setColor(Color.CYAN);
        fontNormal.draw(batch, "CHAT DE LA PARTIDA", chatX + 40, chatY + chatH - 40);

        if (chatMessages != null && !chatMessages.isEmpty()) {
            float msgY = chatY + chatH - 150;
            int startIndex = Math.max(0, chatMessages.size() - 15);
            for (int i = startIndex; i < chatMessages.size(); i++) {
                ChatMessage msg = chatMessages.get(i);
                String line = msg.getSenderName() + ": " + msg.getText();
                GlyphLayout layout = new GlyphLayout();
                layout.setText(fontNormal, line, Color.WHITE, chatW - 80, Align.left, true);

                fontNormal.draw(batch, line, chatX + 40, msgY, chatW - 80, Align.left, true);
                msgY -= (layout.height + 30);
            }
        } else {
            fontNormal.setColor(Color.WHITE);
            fontNormal.draw(batch, "Sin mensajes.", chatX + 40, chatY + chatH - 150);
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
        imgConfirmar.dispose();
        imgRechazar.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        }
}
