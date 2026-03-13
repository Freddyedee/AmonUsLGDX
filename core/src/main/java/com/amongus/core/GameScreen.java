package com.amongus.core;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.minigame.MinigameScreen;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.TaskType;
import com.amongus.core.impl.actions.NetworkActionSender;
import com.amongus.core.impl.actions.SabotageAction;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.network.GameClient;
import com.amongus.core.impl.player.InputHandler;
import com.amongus.core.impl.sabotage.SabotageManager;
import com.amongus.core.view.*;
import com.amongus.core.view.screens.MainMenuScreen;
import com.amongus.debug.DebugConfig;
import com.amongus.debug.DebugEndGame;
import com.amongus.debug.DebugRenderer;
import com.amongus.debug.DebugWinCondAction;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

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
    private ActionSender actionSender;

    private Texture mapa;
    private Texture mapaLobby;
    private Texture pixelBlanco; // Renombrado de pixelAmarillo
    private Texture imgBtnPlayAgain;
    private Texture imgBtnQuit;

    private InputHandler inputHandler;
    private HudRenderer hudRenderer;
    private VotingRenderer votingRenderer;
    private IntroOverlay introOverlay;
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

    // --- VARIABLES DE TAREAS ---
    private ShapeRenderer shapeRenderer;
    private final java.util.Map<String, Texture> taskSprites = new java.util.HashMap<>();
    private TaskArrowRenderer taskArrowRenderer;

    // --- VARIABLES DE SABOTAJE ---
    private SabotageMapOverlay sabotageMapOverlay;
    private Texture sabotageButtonTexture;
    private Rectangle sabotageButtonRect;
    private static final float SAB_BTN_SIZE = 80f;
    private boolean internetSabotaged = false;

    // --- VARIABLES DE LUCES ---
    private Texture visionMask;
    private boolean lightsSabotaged = false;
    private static final float DEFAULT_VISION_RADIUS = 280f;
    private static final float SABOTAGED_VISION_RADIUS = 110f; // Súper reducido
    private float darknessAlpha = 0f;

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
        visionMask = new Texture(Gdx.files.internal("sprites/vision_mask.png"));

        // --- INICIALIZAR FUENTE FREETYPE PARA EL OVERLAY ---
        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal("ui/comic/fuente-regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 72; // Tamaño base grande para que no se pixele
        parameter.color = Color.WHITE;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;

        BitmapFont introFont = generator.generateFont(parameter);
        generator.dispose(); // IMPORTANTE: Liberar el generador de memoria

        this.introOverlay = new IntroOverlay(batch, introFont);

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

        actionSender = new NetworkActionSender(engine, clienteRed);
        inputHandler = new InputHandler(actionSender, engine, myPlayerId);

        // --- INICIALIZAR SABOTAJE ---
        sabotageMapOverlay = new SabotageMapOverlay();
        sabotageMapOverlay.setMap(engine.getMapType());

        // Carga de texturas de botones (asegúrate de tener estos archivos en assets)
        sabotageMapOverlay.setInternetButtonTexture("minijuegos/sabotageInternet/internetObj-removebg-preview.png");
        sabotageMapOverlay.setLightsButtonTexture("minijuegos/fixLights/fixLightsObjt-removebg-preview.png");

        sabotageMapOverlay.setSelectListener((type, btnIndex) -> {
            // Cuando el impostor hace click en el mapa de sabotaje
            actionSender.send(new SabotageAction(myPlayerId, type));
        });

        sabotageButtonTexture = new Texture(Gdx.files.internal("hud/Sabotaje.png"));
        // Posición del botón en la esquina justo por encima del botón de kill
        float sabX = Gdx.graphics.getWidth() - 220f;
        float sabY = 200f;
        sabotageButtonRect = new Rectangle(sabX, sabY, SAB_BTN_SIZE, SAB_BTN_SIZE);

        engine.getSabotageManager().setListener(new SabotageManager.SabotageListener() {
            @Override
            public void onSabotageActivated(SabotageManager.SabotageType type) {
                if (type == SabotageManager.SabotageType.INTERNET) internetSabotaged = true;
                if (type == SabotageManager.SabotageType.LIGHTS) lightsSabotaged = true;
            }

            @Override
            public void onSabotageResolved(SabotageManager.SabotageType type) {
                if (type == SabotageManager.SabotageType.INTERNET) internetSabotaged = false;
                if (type == SabotageManager.SabotageType.LIGHTS) lightsSabotaged = false;
            }
        });
    }

    @Override
    public void render(float delta) {
        GameSnapshot snapshot = engine.getSnapshot();

        // Obtenemos al jugador local para revisar su rol
        PlayerView me = findLocalPlayer(snapshot);
        boolean isImpostor = (me != null && me.getRole() == Role.IMPOSTOR);

        // Luces: Si hay sabotaje y NO eres impostor, se oscurece.
        if (lightsSabotaged && !isImpostor) {
            darknessAlpha = Math.min(1f, darknessAlpha + delta * 2f); // Oscurece en 0.5s
        } else {
            darknessAlpha = Math.max(0f, darknessAlpha - delta * 4f); // Aclara más rápido
        }

        //* ── ATAJOS DE DEBUG (Solo para desarrollo) ──
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            // Enviamos el valor INVERSO al actual para alternarlo (Toggle)
            boolean newState = !DebugConfig.IGNORE_WIN_CONDITIONS;
            actionSender.send(new DebugWinCondAction(myPlayerId, newState));
            System.out.println("[DEBUG] Ignorar Victorias: " + newState);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            // Spawnea un bot azul para que lo uses de víctima
            engine.spawnTestingBot("Victima", SkinColor.AZUL);
        }
        //*/
        if (engine.getGameResult() != null) {
            if (engine.getActiveMinigame() != null) {
                engine.setActiveMinigame(null);
            }
            renderEndGame();
            return;
        }

        if (currentLoadedMap != engine.getMapType()) {
            if (mapa != null) mapa.dispose();
            currentLoadedMap = engine.getMapType();
            mapa = new Texture(currentLoadedMap.getVisualPath());
            if (sabotageMapOverlay != null) {
                sabotageMapOverlay.setMap(currentLoadedMap);
            }
            return;
        }
        // Solo permitimos abrir el menú si estamos en el LOBBY
        if (snapshot.getState() == GameState.LOBBY) {
            if (hudRenderer.isConfigurationClicked() || Gdx.input.isKeyJustPressed(keyEscape)) {
                isMenuOpen = !isMenuOpen;
            }
        } else {
            // Si no estamos en el lobby, forzamos a que el menú esté cerrado
            isMenuOpen = false;
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
            me = findLocalPlayer(snapshot);
            batch.begin();
            if (isHost) {
                if (me != null) {
                    float dist = Vector2.dst(me.getPosition().x(), me.getPosition().y(), CONSOLE_X, CONSOLE_Y);

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
                // Verificamos cuántos somos para mostrar el mensaje correcto
                int playerCount = snapshot.getPlayers().size();
                if (playerCount >= 4) {
                    font.setColor(Color.GREEN);
                    font.draw(batch, "ERES EL HOST. PRESIONA [ENTER] O [START] PARA INICIAR", Gdx.graphics.getWidth() / 2f - 250, 50);
                    font.setColor(Color.WHITE);
                } else {
                    font.setColor(Color.RED);
                    font.draw(batch, "FALTAN JUGADORES PARA INICIAR (" + playerCount + "/2)", Gdx.graphics.getWidth() / 2f - 180, 50);
                    font.setColor(Color.WHITE);
                }
            } else {
                String nombreMapa = engine.getMapType() == MapType.MAPA_1 ? "Aulas Principal" : "Cancha y Estacionamiento";
                font.draw(batch, "Mapa seleccionado por el Host: " + nombreMapa, 20, Gdx.graphics.getHeight() - 20);
                font.draw(batch, "ESPERANDO A QUE EL HOST INICIE LA PARTIDA...", Gdx.graphics.getWidth() / 2f - 200, 50);
            }
            batch.end();

            // 1. Dibujamos el Botón de Configuración
            hudRenderer.drawConfigButton(batch);

            // 2. Dibujamos los botones del HUD (Engañamos al HUD diciéndole que somos CREWMATE para que ponga el botón USAR a la derecha)
            hudRenderer.draw(batch, false, false, false, canUseConsole, delta, 0f, Role.CREWMATE);

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
                renderProgressBar(snapshot);

                batch.getProjectionMatrix().setToOrtho2D(0, 0, 3840, 2160);
                batch.begin();
                votingRenderer.draw(batch, snapshot, engine.getCurrentReporterId(),
                    meetingTimer, engine.getVotedPlayers(),
                    true, meetingWasSkipped, wasEmergency, null, engine.getChatMessages());
                batch.end();

                if (votingRenderer.isChatOpen && votingRenderer.stage != null) {
                    votingRenderer.stage.act(delta);
                    votingRenderer.stage.draw();
                }

                if (meetingTimer >= 5f) {
                    showingMeetingResults = false;
                    meetingTimer = 0f;

                    // Asegurar que el chat se cierre al volver al juego
                    if (votingRenderer.isChatOpen) {
                        votingRenderer.isChatOpen = false;
                        Gdx.input.setInputProcessor(null);
                        if (votingRenderer.stage != null) votingRenderer.stage.unfocusAll();
                    }
                }
                return;
            }

            inputHandler.handleGameInput(snapshot, delta);
            engine.getSabotageManager().update(delta);
            snapshot = engine.getSnapshot();

            me = findLocalPlayer(snapshot);
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

            // Tinte Rojo si hay sabotaje de Internet
            if (internetSabotaged) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                batch.begin();
                batch.setColor(1f, 0f, 0f, 0.3f); // Tinte rojo semitransparente
                batch.draw(pixelBlanco, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                batch.setColor(Color.WHITE);
                batch.end();
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            // --- BARRAS DE PROGRESO Y FLECHAS ---
            renderProgressBar(snapshot);

            Matrix4 screenMatrix = new Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            batch.setProjectionMatrix(screenMatrix);
            batch.begin();

            boolean isSabotageActive = internetSabotaged || lightsSabotaged;

            if (!isSabotageActive) {
                // Si no hay sabotaje, dibujamos las flechas amarillas de las tareas
                taskArrowRenderer.draw(batch, snapshot, myPlayerId, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
            } else {
                // Si hay sabotaje, OCULTAMOS las amarillas y dibujamos SOLO la roja apuntando a la emergencia
                renderSabotageArrow(snapshot);
            }

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

                if (nearbyCorpse != null) { drawReportHUD(); }
                // NUEVO: HUD del Impostor (Botón de Sabotaje)
                if (me.getRole() == Role.IMPOSTOR) {
                    renderSabotageHUD();
                    handleSabotageInput();

                    if (sabotageMapOverlay.isVisible()) {
                        batch.setProjectionMatrix(screenMatrix);
                        sabotageMapOverlay.render(batch, shapeRenderer, engine.getSabotageManager());
                        batch.setProjectionMatrix(camera.combined);
                    }
                }
            } else {
                batch.begin();
                font.draw(batch, "ESTAS INHABILITADO - MODO ESPECTADOR", 20, Gdx.graphics.getHeight() - 20);
                font.draw(batch, "[ ESPACIO / FLECHAS ] para cambiar de camara", 20, Gdx.graphics.getHeight() - 40);
                batch.end();
            }

            //  Dibujar el Overlay de Introducción por encima de TODO el HUD
            introOverlay.render(snapshot, inputHandler.getFreezeTimer());

            if (showDebugEndGame) {
                batch.begin();
                debugEndGame.drawAndHandleInput(batch, font, engine);
                batch.end();
            }

            // ── LÓGICA DE REUNIÓN (MEETING) ──
        } else if (snapshot.getState() == GameState.MEETING) {

            meetingTimer += delta;

            // PASAMOS EL RENDERER AL INPUT HANDLER PARA QUE DETECTE CLICS
            inputHandler.handleMeetingInput(snapshot, meetingTimer, votingRenderer);

            long vivos = snapshot.getPlayers().stream().filter(PlayerView::isAlive).count();
            boolean todosVotaron = engine.getVotedPlayers().size() >= vivos;

            clear(0.05f, 0.05f, 0.1f, 1);
            batch.getProjectionMatrix().setToOrtho2D(0, 0, 3840, 2160);
            batch.begin();

            // 👇 PASAMOS EL JUGADOR SELECCIONADO PARA QUE DIBUJE LOS BOTONES
            votingRenderer.draw(batch, snapshot, engine.getCurrentReporterId(),
                meetingTimer, engine.getVotedPlayers(),
                false, false, engine.isEmergencyMeeting(),
                inputHandler.getSelectedVoteTarget(), engine.getChatMessages()); // <--- NUEVO

            batch.end();

            if (votingRenderer.isChatOpen && votingRenderer.stage != null) {
                votingRenderer.stage.act(delta);
                votingRenderer.stage.draw();
            }

            if (meetingTimer >= 60f || todosVotaron) {
                wasEmergency = engine.isEmergencyMeeting();
                java.util.Optional<PlayerId> expulsado = engine.resolveVoting();
                meetingWasSkipped = expulsado.isEmpty();

                expulsado.ifPresent(id -> {
                    // Evitar que el cuerpo del expulsado se pueda reportar al volver a la nave
                    inputHandler.addStaleCorpse(id);
                });

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

        // ── 1. DIBUJAR FONDO Y OTROS JUGADORES (LO QUE SE OSCURECE) ──
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (snapshot.getState() == GameState.LOBBY) {
            batch.draw(mapaLobby, 0, 0, 3840, 2160);
        } else {
            batch.draw(mapa, 0, 0, 3840, 2160);
        }

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.getId().equals(targetId)) continue;
            if (pv.isVenting()) continue;

            int dir = pv.getDirection();
            boolean moving = pv.isMoving();
            playerRenderer.draw(batch, pv.getPosition().x(), pv.getPosition().y(), pv.getId(), dir, moving, pv.isAlive(), pv.getSkinColor());
        }

        if (showDebug) debugRenderer.drawHitboxes(snapshot);
        batch.end();

        // DIBUJAR TAREAS EN EL SUELO (También se oscurecen)
        renderTasks(snapshot);

        // ── 2. APLICAR EL EFECTO DE VISIÓN SUAVE (MÁSCARA DE OSCURIDAD) ──
        if (snapshot.getState() == GameState.IN_GAME && darknessAlpha > 0 && targetView != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);

            batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.begin();

            batch.setColor(1f, 1f, 1f, darknessAlpha);

            float radius = lightsSabotaged ? SABOTAGED_VISION_RADIUS : DEFAULT_VISION_RADIUS;
            // Hacemos el degradado amplio (puedes jugar con este 2.8f si quieres más o menos difuminado)
            float size = radius * 2.8f;
            float halfSize = size / 2f;

            Vector3 screenPos = new Vector3(targetView.getPosition().x(), targetView.getPosition().y(), 0);
            camera.project(screenPos);

            float cx = screenPos.x;
            float cy = screenPos.y;
            float sw = Gdx.graphics.getWidth();
            float sh = Gdx.graphics.getHeight();

            // Calculamos los bordes exactos donde termina la textura
            float leftEdge = cx - halfSize;
            float rightEdge = cx + halfSize;
            float bottomEdge = cy - halfSize;
            float topEdge = cy + halfSize;

            // 1. Dibujamos el círculo de visión exactamente en sus bordes
            batch.draw(visionMask, leftEdge, bottomEdge, size, size);

            // 2. Rellenamos el resto con negro puro transparente, respetando los bordes exactos
            batch.setColor(0, 0, 0, darknessAlpha);
            // Rectángulo Arriba
            batch.draw(pixelBlanco, 0, topEdge, sw, sh - topEdge);
            // Rectángulo Abajo
            batch.draw(pixelBlanco, 0, 0, sw, bottomEdge);
            // Rectángulo Izquierda
            batch.draw(pixelBlanco, 0, bottomEdge, leftEdge, size);
            // Rectángulo Derecha
            batch.draw(pixelBlanco, rightEdge, bottomEdge, sw - rightEdge, size);

            batch.setColor(Color.WHITE); // Restauramos
            batch.end();

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // ── 3. DIBUJAR AL JUGADOR LOCAL Y NOMBRES (POR ENCIMA DE LA OSCURIDAD) ──
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (targetView != null && !targetView.isVenting()) {
            // Si somos nosotros, usamos nuestro teclado. Si estamos espectando, usamos su dirección de red.
            int renderDir = targetView.getId().equals(myPlayerId) ? inputHandler.getDireccion() : targetView.getDirection();
            playerRenderer.draw(batch, targetView.getPosition().x(), targetView.getPosition().y(), targetView.getId(), renderDir, targetView.isMoving(), targetView.isAlive(), targetView.getSkinColor());
        }

        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isVenting()) continue;

            float dist = Vector2.dst(targetView.getPosition().x(), targetView.getPosition().y(), pv.getPosition().x(), pv.getPosition().y());
            float currentVisRadius = lightsSabotaged ? SABOTAGED_VISION_RADIUS + 50f : DEFAULT_VISION_RADIUS + 100f;

            if (dist < currentVisRadius) {
                String nombre = pv.getId().equals(myPlayerId) ? "Tú" : pv.getName();
                float yOffset = pv.isAlive() ? 85 : 30;
                font.draw(batch, nombre, pv.getPosition().x() - 50, pv.getPosition().y() + yOffset, 100, com.badlogic.gdx.utils.Align.center, false);
            }
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
        // Añade esto solo para probar:
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            System.out.println("[DEBUG HUD] Tareas: " + snapshot.getCompletedTasks() + "/" + snapshot.getTotalTasks());
        }
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

    // ── Helpers de Sabotaje ──
    private void renderSabotageHUD() {
        SabotageManager sm = engine.getSabotageManager();
        boolean canSab = sm.canSabotage();

        // Definir y aplicar tanaño
        float newSabSize = 120f; // Aumentado de 80f para coincidir con el tamaño de "KILL"

        // Actualizamos el rectángulo con las nuevas dimensiones y la posición de anclaje dinámica
        sabotageButtonRect.width = newSabSize;
        sabotageButtonRect.height = newSabSize;
        // Ajustamos la X para un botón más grande, manteniendo el anclaje a la derecha
        sabotageButtonRect.x = Gdx.graphics.getWidth() - 140f; // Ajustado para un botón más grande
        sabotageButtonRect.y = 200f; // Alineación vertical

        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        // Aplicar el color base (normal o desaturado si está en cooldown)
        batch.setColor(canSab ? 1f : 0.5f, canSab ? 1f : 0.5f, canSab ? 1f : 0.5f, 1f);

        // Ahora dibujamos usando el rectángulo actualizado con el nuevo tamaño
        batch.draw(sabotageButtonTexture, sabotageButtonRect.x, sabotageButtonRect.y, sabotageButtonRect.width, sabotageButtonRect.height);

        // Restaurar el color a blanco puro para el cooldown
        batch.setColor(Color.WHITE);

        // Dibujar el temporizador de cooldown si está activo
        if (!canSab && sm.getCooldownRemaining() > 0f) {
            font.setColor(Color.RED);
            // El posicionamiento ya usa las dimensiones actualizadas, por lo que se centrará automáticamente
            font.draw(batch, String.format("%.0f", sm.getCooldownRemaining()), sabotageButtonRect.x + sabotageButtonRect.width / 2f - 8f, sabotageButtonRect.y + sabotageButtonRect.height + 18f);
            font.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void handleSabotageInput() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (sabotageButtonRect.contains(mx, my)) {
                sabotageMapOverlay.toggle();
            }
        }
        sabotageMapOverlay.handleInput(engine.getSabotageManager());
    }

    private void renderSabotageArrow(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me == null) return;

        snapshot.getTasks().stream()
            .filter(tv -> tv.getTaskType() == TaskType.SABOTAGE)
            .filter(tv -> {
                // SOLUCIÓN 1: Convertir el UUID a String antes de comparar
                String idStr = tv.getId().value().toString();
                if (internetSabotaged && idStr.equals("11111111-1111-1111-1111-111111111111")) return true;
                if (lightsSabotaged && idStr.equals("22222222-2222-2222-2222-222222222222")) return true;
                return false;
            })
            .findFirst()
            .ifPresent(sabTask -> {
                // SOLUCIÓN 2: Calcular la matemática exacta en el mundo y la pantalla
                float px = me.getPosition().x();
                float py = me.getPosition().y();
                float tx = sabTask.getPosition().x();
                float ty = sabTask.getPosition().y();

                // Ángulo real en el mundo (desde el jugador hacia el sabotaje)
                float angleDeg = (float) Math.toDegrees(Math.atan2(ty - py, tx - px));

                // Proyectamos a la pantalla para saber hacia dónde ir desde el centro
                Vector3 taskScreen = new Vector3(tx, ty, 0);
                camera.project(taskScreen);

                float sw = Gdx.graphics.getWidth();
                float sh = Gdx.graphics.getHeight();
                float cx = sw / 2f;
                float cy = sh / 2f;

                float dx = taskScreen.x - cx;
                float dy = taskScreen.y - cy;

                float size = 50f;
                float padding = 40f;
                float innerW = sw / 2f - padding - size / 2f;
                float innerH = sh / 2f - padding - size / 2f;

                if (dx == 0 && dy == 0) return; // Evitar división por cero si estás exactamente encima

                float arrowX, arrowY;
                // Lógica de anclaje a los bordes (la misma que usaste en TaskArrowRenderer)
                if (Math.abs(dx) * innerH > Math.abs(dy) * innerW) {
                    float scale = innerW / Math.abs(dx);
                    arrowX = cx + dx * scale;
                    arrowY = cy + dy * scale;
                } else {
                    float scale = innerH / Math.abs(dy);
                    arrowX = cx + dx * scale;
                    arrowY = cy + dy * scale;
                }

                batch.setColor(1f, 0.15f, 0.15f, 1f); // Flecha Roja
                taskArrowRenderer.drawSingleArrow(batch, arrowX, arrowY, angleDeg, size);
                batch.setColor(1f, 1f, 1f, 1f);
            });
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
        if (sabotageButtonTexture != null) sabotageButtonTexture.dispose();
        if (sabotageMapOverlay != null) sabotageMapOverlay.dispose();
    }
}
