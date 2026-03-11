package com.amongus.core;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.actions.NetworkActionSender;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.impl.player.InputHandler;
import com.amongus.core.view.*;
import com.amongus.core.view.screens.MainMenuScreen;
import com.amongus.debug.DebugConfig;
import com.amongus.debug.DebugEndGame;
import com.amongus.debug.DebugRenderer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

import static com.badlogic.gdx.utils.ScreenUtils.clear;

public class GameScreen implements Screen {

    private final GameEngine engine;
    private final PlayerId myPlayerId;
    private final GameClient clienteRed;
    private final boolean isHost;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private PlayerRenderer playerRenderer;
    private BitmapFont font;

    private Texture mapa;
    private Texture mapaLobby;
    private Texture pixelBlanco; // Renombrado de pixelAmarillo
    private Texture imgBtnPlayAgain;
    private Texture imgBtnQuit;

    private InputHandler inputHandler;
    private HudRenderer hudRenderer;
    private VotingRenderer votingRenderer;
    private DebugRenderer debugRenderer;
    private DebugEndGame debugEndGame;
    private boolean showDebug = false;
    private boolean showDebugEndGame = false;

    private boolean isMenuOpen = false;
    private int keyEscape = Input.Keys.ESCAPE;

    // --- VARIABLES DE LA CONSOLA DEL LOBBY ---
    private MapType currentLoadedMap;
    private final float CONSOLE_X = 1467f;
    private final float CONSOLE_Y = 1064f;
    private final float CONSOLE_RADIUS = 40f;

    // Variables de control de la reunión
    private float meetingTimer = 0f;
    private boolean showingMeetingResults = false;
    private boolean meetingWasSkipped = false;
    private boolean wasEmergency = false;

    // --- VARIABLES DE TAREAS (De Eliuber) ---
    private ShapeRenderer shapeRenderer;
    private final java.util.Map<String, Texture> taskSprites = new java.util.HashMap<>();
    private TaskArrowRenderer taskArrowRenderer;

    public GameScreen(GameEngine engine, GameClient clienteRed, boolean isHost) {
        this.engine = engine;
        this.myPlayerId = engine.getLocalPlayerId();
        this.clienteRed = clienteRed;
        this.isHost = isHost;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        debugRenderer = new DebugRenderer(batch);
        debugEndGame = new DebugEndGame();
        camera = new OrthographicCamera();
        playerRenderer = new PlayerRenderer();
        font = new BitmapFont();
        hudRenderer = new HudRenderer();
        votingRenderer = new VotingRenderer();
        imgBtnPlayAgain = new Texture(Gdx.files.internal("hud/JugarOtraVez.png"));
        imgBtnQuit = new Texture(Gdx.files.internal("hud/Salir.png"));

        // Inicializar Tareas
        shapeRenderer = new ShapeRenderer();
        taskArrowRenderer = new TaskArrowRenderer();

        // Generamos un píxel de 1x1 color blanco puro
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelBlanco = new Texture(pixmap);
        pixmap.dispose();

        font.setColor(Color.WHITE);
        camera.setToOrtho(false, 800, 480);

        mapa = new Texture(engine.getMapType().getVisualPath());
        mapaLobby = new Texture("mapas/SalaEspera.png");

        ActionSender sender = new NetworkActionSender(engine, clienteRed);
        inputHandler = new InputHandler(sender, engine, myPlayerId);
    }

    @Override
    public void render(float delta) {
        GameSnapshot snapshot = engine.getSnapshot();

        // ── ATAJOS DE DEBUG (Solo para desarrollo) ──

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            DebugConfig.IGNORE_WIN_CONDITIONS = !DebugConfig.IGNORE_WIN_CONDITIONS;
            System.out.println("[DEBUG] Ignorar Victorias: " + DebugConfig.IGNORE_WIN_CONDITIONS);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            // Spawnea un bot azul para que lo uses de víctima
            engine.spawnTestingBot("Victima", SkinColor.AZUL);
        }

        if (engine.getGameResult() != null) {
            renderEndGame();
            return;
        }

        if (currentLoadedMap != engine.getMapType()) {
            if (mapa != null) mapa.dispose();
            currentLoadedMap = engine.getMapType();
            mapa = new Texture(currentLoadedMap.getVisualPath());
            return;
        }

        if (hudRenderer.isConfigurationClicked() || Gdx.input.isKeyJustPressed(keyEscape)) {
            isMenuOpen = !isMenuOpen;
        }

        if (isMenuOpen) {
            clear(0, 0, 0, 1);
            renderGameplay(snapshot);

            renderMenu();

            batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            hudRenderer.drawConfigButton(batch);

            return;
        }

        // ── LÓGICA DE SALA DE ESPERA ──
        if (snapshot.getState() == GameState.LOBBY) {

            // Limpieza visual de la reunión
            showingMeetingResults = false;
            meetingWasSkipped = false;
            meetingTimer = 0f;

            inputHandler.handleGameInput(snapshot, delta);
            snapshot = engine.getSnapshot();

            clear(0, 0, 0, 1);
            renderGameplay(snapshot);

            batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            boolean canUseConsole = false;
            PlayerView me = findLocalPlayer(snapshot);
            batch.begin();
            if (isHost) {
                if (me != null) {
                    float dist = com.badlogic.gdx.math.Vector2.dst(me.getPosition().x(), me.getPosition().y(), CONSOLE_X, CONSOLE_Y);

                    // Si estamos cerca, habilitamos el botón de usar
                    if (dist <= CONSOLE_RADIUS) {
                        canUseConsole = true;
                        font.setColor(Color.YELLOW);
                        font.draw(batch, "Presiona [ E ] o USAR para cambiar el mapa", Gdx.graphics.getWidth() / 2f - 180, 150);
                        font.setColor(Color.WHITE);
                    }
                }

                String nombreMapa = engine.getMapType() == MapType.MAPA_1 ? "Aulas Principal" : "Cancha y Estacionamiento";
                font.draw(batch, "Mapa actual: " + nombreMapa, 20, Gdx.graphics.getHeight() - 20);
                font.draw(batch, "ERES EL HOST. PRESIONA [ENTER] O [START] PARA INICIAR", Gdx.graphics.getWidth() / 2f - 250, 50);
            } else {
                String nombreMapa = engine.getMapType() == MapType.MAPA_1 ? "Aulas Principal" : "Cancha y Estacionamiento";
                font.draw(batch, "Mapa seleccionado por el Host: " + nombreMapa, 20, Gdx.graphics.getHeight() - 20);
                font.draw(batch, "ESPERANDO A QUE EL HOST INICIE LA PARTIDA...", Gdx.graphics.getWidth() / 2f - 200, 50);
            }
            batch.end();

            // 1. Dibujamos el Botón de Configuración
            hudRenderer.drawConfigButton(batch);

            // 2. Dibujamos los botones del HUD (Engañamos al HUD diciéndole que somos CREWMATE para que ponga el botón USAR a la derecha)
            hudRenderer.draw(batch, false, false, false, canUseConsole, delta, 0f, com.amongus.core.api.player.Role.CREWMATE);

            // Lógica exclusiva del HOST
            if (isHost) {
                // 3. Dibujamos el botón de Start (ahora aparece arriba del Usar)
                hudRenderer.drawStartButton(batch);

                // --- ACCIÓN: Cambiar Mapa ---
                // Detecta tanto la tecla E como el click en el botón en pantalla
                if (canUseConsole && (Gdx.input.isKeyJustPressed(Input.Keys.E) || hudRenderer.isUseClicked())) {
                    MapType[] maps = MapType.values();
                    int nextIdx = (engine.getMapType().ordinal() + 1) % maps.length;
                    MapType nextMap = maps[nextIdx];

                    engine.setMapType(nextMap);
                    if (clienteRed != null) {
                        clienteRed.enviarMensaje("CHANGE_MAP:" + nextMap.name());
                    }
                }

                // --- ACCIÓN: Iniciar Partida ---
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || hudRenderer.isStartClicked()) {
                    engine.startGameHost(clienteRed);
                }
            }
            return;
        }

        // ── LÓGICA DE PARTIDA (IN_GAME) ──
        if (snapshot.getState() == GameState.IN_GAME) {

            if (showingMeetingResults) {
                meetingTimer += delta;
                clear(0, 0, 0, 1);
                renderGameplay(snapshot);

                batch.getProjectionMatrix().setToOrtho2D(0, 0, 3840, 2160);
                batch.begin();
                votingRenderer.draw(batch, snapshot, engine.getCurrentReporterId(),
                    meetingTimer, engine.getVotedPlayers(),
                    true, meetingWasSkipped, wasEmergency);
                batch.end();

                if (meetingTimer >= 5f) {
                    showingMeetingResults = false;
                    meetingTimer = 0f;
                }
                return;
            }

            inputHandler.handleGameInput(snapshot, delta);
            snapshot = engine.getSnapshot();

            PlayerView me = findLocalPlayer(snapshot);
            boolean isAlive = me != null && me.isAlive();

            PlayerView nearbyCorpse = null;
            if (isAlive) {
                nearbyCorpse = inputHandler.handleReportInput(snapshot);
            }

            clear(0, 0, 0, 1);

            // --- MINIJUEGO OVERLAY ---
            if (engine.getActiveMinigame() != null) {
                renderGameplay(snapshot); // Mapa de fondo
                renderMinigameOverlay(engine.getActiveMinigame(), delta);
                return; // Evitamos dibujar el HUD encima
            }

            // Dibujamos el mundo
            renderGameplay(snapshot);

            // --- BARRAS DE PROGRESO Y FLECHAS (De Eliuber) ---
            renderProgressBar(snapshot);

            Matrix4 screenMatrix = new Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            batch.setProjectionMatrix(screenMatrix);
            batch.begin();
            taskArrowRenderer.draw(batch, snapshot, myPlayerId, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
            batch.end();
            batch.setProjectionMatrix(camera.combined); // Restaurar cámara

            // --- HUD NORMAL ---
            batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            if (isAlive) {
                // Le pasamos el 'canUse' desde el InputHandler y el 'myRole' del jugador
                boolean canUse = inputHandler.canUse(snapshot);

                hudRenderer.draw(batch, inputHandler.isKillReady(), nearbyCorpse != null, inputHandler.canVent(), canUse, delta, inputHandler.getKillCooldown(), me.getRole());

                inputHandler.handleHudClick(hudRenderer, snapshot);

                // Si el jugador hace clic físico en el botón USAR de la pantalla
                if (hudRenderer.isUseClicked()) {
                    inputHandler.executeInteraction(snapshot);
                }

                if (nearbyCorpse != null) {
                    drawReportHUD();
                }
            } else {
                batch.begin();
                font.draw(batch, "ESTAS INHABILITADO - MODO ESPECTADOR", 20, Gdx.graphics.getHeight() - 20);
                font.draw(batch, "[ ESPACIO / FLECHAS ] para cambiar de camara", 20, Gdx.graphics.getHeight() - 40);
                batch.end();
            }

            if (showDebugEndGame) {
                batch.begin();
                debugEndGame.drawAndHandleInput(batch, font, engine);
                batch.end();
            }

            // ── LÓGICA DE REUNIÓN (MEETING) ──
        } else if (snapshot.getState() == GameState.MEETING) {

            meetingTimer += delta;
            inputHandler.handleMeetingInput(snapshot, meetingTimer);

            long vivos = snapshot.getPlayers().stream().filter(PlayerView::isAlive).count();
            boolean todosVotaron = engine.getVotedPlayers().size() >= vivos;

            clear(0.05f, 0.05f, 0.1f, 1);
            batch.getProjectionMatrix().setToOrtho2D(0, 0, 3840, 2160);
            batch.begin();
            votingRenderer.draw(batch, snapshot, engine.getCurrentReporterId(),
                meetingTimer, engine.getVotedPlayers(),
                false, false, engine.isEmergencyMeeting());
            batch.end();

            if (meetingTimer >= 60f || todosVotaron) {
                wasEmergency = engine.isEmergencyMeeting();
                java.util.Optional<PlayerId> expulsado = engine.resolveVoting();
                meetingWasSkipped = expulsado.isEmpty();

                if (isHost) {
                    expulsado.ifPresent(id -> {
                        engine.requestKill(myPlayerId, id);
                    });
                }

                showingMeetingResults = true;
                meetingTimer = 0f;
            }
        }
    }

    private void renderGameplay(GameSnapshot snapshot) {
        PlayerId targetId = inputHandler.getSpectatedPlayerId();
        PlayerView targetView = snapshot.getPlayers().stream().filter(p -> p.getId().equals(targetId)).findFirst().orElse(null);

        if (targetView != null) {
            camera.position.set(targetView.getPosition().x(), targetView.getPosition().y(), 0);
            camera.update();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (snapshot.getState() == GameState.LOBBY) {
            batch.draw(mapaLobby, 0, 0, 3840, 2160);
        } else {
            batch.draw(mapa, 0, 0, 3840, 2160);
        }

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive()) {
                if (pv.isVenting()) continue;
                boolean isMe = pv.getId().equals(myPlayerId);
                int dir = isMe ? inputHandler.getDireccion() : pv.getDirection();
                boolean moving = pv.isMoving();
                playerRenderer.draw(batch, pv.getPosition().x(), pv.getPosition().y(), pv.getId(), dir, moving, true, pv.getSkinColor());
            } else {
                boolean isMe = pv.getId().equals(myPlayerId);
                int dir = isMe ? inputHandler.getDireccion() : pv.getDirection();
                boolean moving = pv.isMoving();
                playerRenderer.draw(batch, pv.getPosition().x(), pv.getPosition().y(), pv.getId(), dir, moving, false, pv.getSkinColor());
            }
        }
        if (showDebug) {
            debugRenderer.drawHitboxes(snapshot);
        }
        batch.end();

        // DIBUJAR TAREAS EN EL SUELO
        renderTasks(snapshot);

        // DIBUJAR LOS NOMBRES
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isVenting()) continue;

            String nombre = pv.getId().equals(myPlayerId) ? "Tú" : pv.getName();
            float yOffset = pv.isAlive() ? 85 : 30;

            font.draw(batch, nombre, pv.getPosition().x() - 50, pv.getPosition().y() + yOffset, 100, com.badlogic.gdx.utils.Align.center, false);
        }
        batch.end();
    }

    private void renderTasks(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        for (TaskView tv : snapshot.getTasks()) {
            float x = tv.getPosition().x();
            float y = tv.getPosition().y();

            float dist = (me != null)
                ? Vector2.dst(
                me.getPosition().x(), me.getPosition().y(), x, y)
                : Float.MAX_VALUE;

            boolean nearby    = dist <= 150f;
            boolean completed = tv.isCompleted();

            String spritePath = tv.getMapSpritePath();

            if (spritePath != null) {
                taskSprites.computeIfAbsent(spritePath,
                    path -> new Texture(path));

                Texture tex = taskSprites.get(spritePath);

                float targetH = 80f * tv.getMapSpriteScale();
                float targetW = targetH * ((float) tex.getWidth() / tex.getHeight());

                batch.setProjectionMatrix(camera.combined);
                batch.begin();

                if (completed) {
                    batch.setColor(0.4f, 0.4f, 0.4f, 0.7f);
                } else if (nearby) {
                    batch.setColor(1f, 1f, 0.5f, 1f);
                } else {
                    batch.setColor(1f, 1f, 1f, 1f);
                }

                batch.draw(tex, x - targetW / 2f, y - targetH / 2f, targetW, targetH);
                batch.setColor(1f, 1f, 1f, 1f);
                batch.end();

                if (nearby && !completed) {
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                    shapeRenderer.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
                    float outlineRadius = (80f / 2f) + 8f;
                    shapeRenderer.circle(x, y, outlineRadius);
                    shapeRenderer.end();
                }

            } else {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                if (completed) {
                    shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 0.7f);
                } else {
                    shapeRenderer.setColor(nearby
                        ? com.badlogic.gdx.graphics.Color.YELLOW
                        : new com.badlogic.gdx.graphics.Color(0.8f, 0.8f, 0f, 0.7f));
                }
                shapeRenderer.circle(x, y, 20);
                shapeRenderer.end();

                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(completed
                    ? Color.DARK_GRAY
                    : (nearby ? Color.WHITE
                    : Color.YELLOW));
                shapeRenderer.circle(x, y, 30);
                shapeRenderer.end();
            }
        }
    }

    private void renderMinigameOverlay(MinigameScreen minigame, float delta) {
        Matrix4 screenMatrix = new Matrix4()
            .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(pixelBlanco, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        minigame.render(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            minigame.cancel();
        }
    }

    private void renderProgressBar(GameSnapshot snapshot) {
        int total     = snapshot.getTotalTasks();
        int completed = snapshot.getCompletedTasks();
        if (total == 0) return;

        float progress  = (float) completed / total;
        float barWidth  = 200f;
        float barHeight = 16f;
        float x         = 20f;
        float y         = Gdx.graphics.getHeight() - 40f;

        Matrix4 screenMatrix = new Matrix4()
            .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        shapeRenderer.setProjectionMatrix(screenMatrix);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
        shapeRenderer.rect(x, y, barWidth * progress, barHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.end();

        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        font.draw(batch, "Tareas: " + completed + "/" + total, x, y + barHeight + 16f);
        batch.end();

        batch.setProjectionMatrix(camera.combined);
    }

    private void renderMenu() {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.begin();
        batch.setColor(0, 0, 0, 0.6f);
        batch.draw(pixelBlanco, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        float menuW = 400;
        float menuH = 300;
        float menuX = (Gdx.graphics.getWidth() - menuW) / 2f;
        float menuY = (Gdx.graphics.getHeight() - menuH) / 2f;

        batch.setColor(Color.BLACK);
        batch.draw(pixelBlanco, menuX, menuY, menuW, menuH);

        batch.setColor(Color.WHITE);
        batch.draw(pixelBlanco, menuX, menuY, menuW, 3);
        batch.draw(pixelBlanco, menuX, menuY + menuH - 3, menuW, 3);
        batch.draw(pixelBlanco, menuX, menuY, 3, menuH);
        batch.draw(pixelBlanco, menuX + menuW - 3, menuY, 3, menuH);

        batch.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
        font.draw(batch, "PAUSA", menuX + 160, menuY + 260);

        float btnW = 300;
        float btnH = 50;
        float btnX = menuX + 50;

        float btnMenuY = menuY + 160;
        float btnSalirY = menuY + 90;
        float btnVolverY = menuY + 20;

        batch.setColor(0.1f, 0.2f, 0.5f, 1f);
        batch.draw(pixelBlanco, btnX, btnMenuY, btnW, btnH);

        batch.setColor(0.1f, 0.2f, 0.5f, 1f);
        batch.draw(pixelBlanco, btnX, btnSalirY, btnW, btnH);

        batch.setColor(0.1f, 0.2f, 0.5f, 1f);
        batch.draw(pixelBlanco, btnX, btnVolverY, btnW, btnH);

        font.getData().setScale(1.2f);
        batch.setColor(Color.WHITE);
        font.draw(batch, "MENÚ PRINCIPAL", btnX + 70, btnMenuY + 33);
        font.draw(batch, "SALIR DEL JUEGO", btnX + 65, btnSalirY + 33);
        font.draw(batch, "VOLVER AL JUEGO", btnX + 65, btnVolverY + 33);

        font.getData().setScale(1f);
        batch.end();

        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (mx >= btnX && mx <= btnX + btnW) {
                if (my >= btnMenuY && my <= btnMenuY + btnH) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            if (clienteRed != null) {
                                clienteRed.disconnect();
                            }
                            AmongUsGame mainGame = (AmongUsGame) Gdx.app.getApplicationListener();
                            mainGame.setScreen(new MainMenuScreen(mainGame));
                            dispose();
                        }
                    });
                } else if (my >= btnSalirY && my <= btnSalirY + btnH) {
                    if (clienteRed != null) {
                        clienteRed.disconnect();
                    }
                    Gdx.app.exit();
                } else if (my >= btnVolverY && my <= btnVolverY + btnH) {
                    isMenuOpen = false;
                }
            }
        }
    }

    private void drawReportHUD() {
        Matrix4 matrixOriginal = batch.getProjectionMatrix().cpy();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch.begin();
        batch.setColor(1, 1, 0, 0.3f);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float grosor = 10f;

        batch.draw(pixelBlanco, 0, 0, w, grosor);
        batch.draw(pixelBlanco, 0, h - grosor, w, grosor);
        batch.draw(pixelBlanco, 0, 0, grosor, h);
        batch.draw(pixelBlanco, w - grosor, 0, grosor, h);

        batch.setColor(Color.WHITE);
        batch.end();

        batch.setProjectionMatrix(matrixOriginal);
    }

    private void renderEndGame() {
        String result = engine.getGameResult();
        if ("IMPOSTOR".equals(result)) {
            clear(0.7f, 0.0f, 0.0f, 1);
        } else {
            clear(0.0f, 0.2f, 0.7f, 1);
        }

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float cx = sw / 2f;
        float cy = sh / 2f;

        batch.begin();
        if ("IMPOSTOR".equals(result)) {
            font.draw(batch, "EL IMPOSTOR GANA", cx - 80, cy + 40);
            font.draw(batch, "Los tripulantes fueron inhabilitados", cx - 120, cy);
        } else {
            font.draw(batch, "LOS TRIPULANTES GANAN", cx - 90, cy + 40);
            font.draw(batch, "El impostor fue expulsado", cx - 100, cy);
        }

        float btnSize = 120f;
        float padding = 40f;
        float playX = cx - btnSize - padding;
        float quitX = cx + padding;
        float btnY = 80f;

        batch.draw(imgBtnPlayAgain, playX, btnY, btnSize, btnSize);
        batch.draw(imgBtnQuit, quitX, btnY, btnSize, btnSize);
        batch.end();

        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = sh - Gdx.input.getY();

            if (my >= btnY && my <= btnY + btnSize) {
                if (mx >= playX && mx <= playX + btnSize) {
                    if (isHost) {
                        System.out.println("[UI] Reiniciando la partida...");
                        if (clienteRed != null) {
                            clienteRed.enviarMensaje("RESTART:ALL");
                        }
                        engine.restartToLobby();

                        showingMeetingResults = false;
                        meetingWasSkipped = false;
                        meetingTimer = 0f;
                    } else {
                        System.out.println("[UI] Solo el Host puede reiniciar la partida.");
                    }
                } else if (mx >= quitX && mx <= quitX + btnSize) {
                    System.out.println("[UI] Saliendo al menú principal...");
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            if (clienteRed != null) {
                                clienteRed.disconnect();
                            }
                            AmongUsGame mainGame = (AmongUsGame) Gdx.app.getApplicationListener();
                            mainGame.setScreen(new MainMenuScreen(mainGame));
                            dispose();
                        }
                    });
                }
            }
        }
    }

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream().filter(p -> p.getId().equals(myPlayerId)).findFirst().orElse(null);
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        mapa.dispose();
        if (mapaLobby != null) mapaLobby.dispose();
        if (pixelBlanco != null) pixelBlanco.dispose();
        font.dispose();
        playerRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
        if (votingRenderer != null) votingRenderer.dispose();
        if (imgBtnPlayAgain != null) imgBtnPlayAgain.dispose();
        if (imgBtnQuit != null) imgBtnQuit.dispose();
        shapeRenderer.dispose();
        taskArrowRenderer.dispose();
        taskSprites.values().forEach(Texture::dispose);
    }
}
