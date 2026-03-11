package com.amongus.core;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.actions.LocalActionSender;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.map.MapConfig;
import com.amongus.core.impl.player.InputHandler;
import com.amongus.core.impl.sabotage.SabotageManager;
import com.amongus.core.view.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen implements Screen {

    // ── Dependencias ──────────────────────────────────────────
    private final GameEngine engine;
    private final PlayerId   myPlayerId;

    // ── Rendering ─────────────────────────────────────────────
    private SpriteBatch        batch;
    private OrthographicCamera camera;
    private PlayerRenderer     playerRenderer;
    private BitmapFont         font;
    private ShapeRenderer      shapeRenderer;

    // Proyección de pantalla reutilizada — se recalcula en render()
    private final Matrix4 screenMatrix = new Matrix4();

    // ── Tasks ─────────────────────────────────────────────────
    private final java.util.Map<String, Texture> taskSprites = new java.util.HashMap<>();
    private TaskArrowRenderer taskArrowRenderer;

    // ── Recursos visuales ─────────────────────────────────────
    private Texture mapa;
    private Texture pixelRojo;

    // ── Minimapa normal ───────────────────────────────────────
    private MinimapOverlay minimap;
    private Texture        minimapButtonTexture;
    private Rectangle      minimapButtonRect;
    private static final float MINIMAP_BTN_SIZE = 52f;

    // ── Mapa activo ───────────────────────────────────────────
    private final MapConfig activeMap = MapConfig.MAPA_1;

    // ── Sabotaje ──────────────────────────────────────────────
    private SabotageMapOverlay sabotageMapOverlay;
    private Texture            sabotageButtonTexture;
    private Rectangle          sabotageButtonRect;
    private static final float SAB_BTN_SIZE = 56f;
    private boolean            internetSabotaged = false;

    // ── Input ─────────────────────────────────────────────────
    private InputHandler inputHandler;

    // ─────────────────────────────────────────────────────────

    public GameScreen(GameEngine engine) {
        this.engine     = engine;
        this.myPlayerId = engine.getLocalPlayerId();
    }

    // ── Ciclo de vida ─────────────────────────────────────────

    @Override
    public void show() {
        batch          = new SpriteBatch();
        camera         = new OrthographicCamera();
        playerRenderer = new PlayerRenderer();
        font           = new BitmapFont();
        shapeRenderer  = new ShapeRenderer();
        taskArrowRenderer = new TaskArrowRenderer();

        font.setColor(Color.WHITE);
        camera.setToOrtho(false, 800, 480);

        mapa      = new Texture("mapas/mapa1.png");
        pixelRojo = new Texture("sprites/PixelRojo.png");

        initMinimap();
        initSabotage();

        ActionSender sender = new LocalActionSender(engine);
        inputHandler = new InputHandler(sender, engine, myPlayerId);
    }

    private void initMinimap() {
        minimap = new MinimapOverlay();
        minimap.setMap(activeMap.texturePath);

        minimapButtonTexture = new Texture(Gdx.files.internal("sprites/minimapButton.png"));
        minimapButtonRect = new Rectangle(
            Gdx.graphics.getWidth()  - MINIMAP_BTN_SIZE - 14f,
            Gdx.graphics.getHeight() - MINIMAP_BTN_SIZE - 14f,
            MINIMAP_BTN_SIZE, MINIMAP_BTN_SIZE
        );
    }

    private void initSabotage() {
        sabotageMapOverlay = new SabotageMapOverlay();
        sabotageMapOverlay.setMap(activeMap.texturePath);
        sabotageMapOverlay.setInternetButtonTexture("sprites/wifiSabotagebtton-removebg-preview.png");
        sabotageMapOverlay.setLightsButtonTexture("sprites/sabotageLightsbtton-removebg-preview.png");
        sabotageMapOverlay.setSelectListener((type, btnIndex) ->
            engine.activateSabotageTask(type));

        sabotageButtonTexture = new Texture(
            Gdx.files.internal("sprites/iconSabotage-removebg-preview.png"));
        sabotageButtonRect = new Rectangle(
            Gdx.graphics.getWidth() / 2f - SAB_BTN_SIZE / 2f,
            10f, SAB_BTN_SIZE, SAB_BTN_SIZE
        );

        engine.getSabotageManager().setListener(new SabotageManager.SabotageListener() {
            @Override
            public void onSabotageActivated(SabotageManager.SabotageType type) {
                if (type == SabotageManager.SabotageType.INTERNET) internetSabotaged = true;
            }
            @Override
            public void onSabotageResolved(SabotageManager.SabotageType type) {
                internetSabotaged = false;
            }
        });
    }

    // ── Loop principal ────────────────────────────────────────

    @Override
    public void render(float delta) {
        // Recalcular screenMatrix una sola vez por frame
        screenMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        GameSnapshot snapshot = engine.getSnapshot();

        if (engine.getGameResult() != null) {
            renderEndGame();
            return;
        }

        if (snapshot.getState() == GameState.IN_GAME) {
            renderInGame(snapshot, delta);
        } else if (snapshot.getState() == GameState.MEETING) {
            inputHandler.handleMeetingInput(snapshot);
            renderVoting(snapshot);
        }
    }

    private void renderInGame(GameSnapshot snapshot, float delta) {
        inputHandler.handleGameInput(snapshot, delta);
        snapshot = engine.getSnapshot();

        PlayerView nearbyCorpse = inputHandler.handleReportInput(snapshot);

        engine.getSabotageManager().update(delta);

        ScreenUtils.clear(0, 0, 0, 1);

        // ── 1. Mundo ──────────────────────────────────────────
        renderGameplay(snapshot);

        // ── 2. HUD de progreso ────────────────────────────────
        renderProgressBar(snapshot);

        // ── 3. Flechas de tareas normales ─────────────────────
        renderTaskArrows(snapshot);

        // ── 4. Flecha de sabotaje ─────────────────────────────
        if (internetSabotaged) renderSabotageArrow(snapshot);

        // ── 5. Minimapa ───────────────────────────────────────
        handleMinimapInput();
        if (minimap.isVisible() && !internetSabotaged) {
            batch.setProjectionMatrix(screenMatrix);
            shapeRenderer.setProjectionMatrix(screenMatrix);
            minimap.render(batch, shapeRenderer, snapshot,
                activeMap.worldWidth, activeMap.worldHeight);
            batch.setProjectionMatrix(camera.combined);
        } else if (internetSabotaged) {
            minimap.hide();
        }

        // ── 6. HUD de sabotaje (solo impostores) ──────────────
        boolean isImpostor = engine.isLocalPlayerImpostor();
        if (isImpostor) {
            renderSabotageHUD();
            handleSabotageInput();
            if (sabotageMapOverlay.isVisible()) {
                batch.setProjectionMatrix(screenMatrix);
                sabotageMapOverlay.render(batch, shapeRenderer, engine.getSabotageManager());
                batch.setProjectionMatrix(camera.combined);
            }
        }

        // ── 7. Minijuego overlay (va último, tapa todo) ───────
        if (engine.getActiveMinigame() != null) {
            renderMinigameOverlay(engine.getActiveMinigame(), delta);
            return;
        }

        // ── 8. Efectos y HUD adicional ────────────────────────
        renderBloodOverlay();
        if (nearbyCorpse != null) drawReportHUD();
    }

    // ── Minimapa ──────────────────────────────────────────────

    private void handleMinimapInput() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (minimapButtonRect.contains(mx, my)) minimap.toggle();
        }
        minimap.handleInput();
    }

    // ── Sabotaje HUD e input ──────────────────────────────────

    private void renderSabotageHUD() {
        SabotageManager sm     = engine.getSabotageManager();
        boolean         canSab = sm.canSabotage();

        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        batch.setColor(canSab ? 1f : 0.5f, canSab ? 1f : 0.5f, canSab ? 1f : 0.5f, 1f);
        batch.draw(sabotageButtonTexture,
            sabotageButtonRect.x, sabotageButtonRect.y,
            sabotageButtonRect.width, sabotageButtonRect.height);
        batch.setColor(1f, 1f, 1f, 1f);

        if (!canSab && sm.getCooldownRemaining() > 0f) {
            font.draw(batch,
                String.format("%.0f", sm.getCooldownRemaining()),
                sabotageButtonRect.x + sabotageButtonRect.width / 2f - 8f,
                sabotageButtonRect.y + sabotageButtonRect.height + 18f);
        }
        if (sm.hasSabotageActive()) {
            font.draw(batch, "SABOTAJE ACTIVO",
                sabotageButtonRect.x - 40f,
                sabotageButtonRect.y + sabotageButtonRect.height + 18f);
        }
        batch.end();
        batch.setProjectionMatrix(camera.combined);
    }

    private void handleSabotageInput() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (sabotageButtonRect.contains(mx, my)) sabotageMapOverlay.toggle();
        }
        sabotageMapOverlay.handleInput(engine.getSabotageManager());
    }

    // ── Flechas ───────────────────────────────────────────────

    private void renderTaskArrows(GameSnapshot snapshot) {
        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        if (!internetSabotaged) {
            taskArrowRenderer.draw(batch, snapshot, myPlayerId,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
        }
        batch.end();
        batch.setProjectionMatrix(camera.combined);
    }

    private void renderSabotageArrow(GameSnapshot snapshot) {
        snapshot.getTasks().stream()
            .filter(tv -> tv.getMapSpritePath() != null
                && tv.getMapSpritePath().contains("internetObj")
                && !tv.isCompleted())
            .findFirst()
            .ifPresent(sabTask -> {
                Vector3 worldPos = new Vector3(
                    sabTask.getPosition().x(),
                    sabTask.getPosition().y(), 0);
                camera.project(worldPos);

                float sw = Gdx.graphics.getWidth();
                float sh = Gdx.graphics.getHeight();
                boolean onScreen = worldPos.x >= 0 && worldPos.x <= sw
                    && worldPos.y >= 0 && worldPos.y <= sh;

                batch.setProjectionMatrix(screenMatrix);

                if (!onScreen) {
                    float margin   = 40f;
                    float clampedX = MathUtils.clamp(worldPos.x, margin, sw - margin);
                    float clampedY = MathUtils.clamp(worldPos.y, margin, sh - margin);
                    float angle    = (float) Math.toDegrees(
                        Math.atan2(worldPos.y - sh / 2f, worldPos.x - sw / 2f));

                    batch.begin();
                    batch.setColor(1f, 0.15f, 0.15f, 1f);
                    taskArrowRenderer.drawSingleArrow(batch, clampedX, clampedY, angle, 45f);
                    batch.setColor(1f, 1f, 1f, 1f);
                    batch.end();
                } else {
                    // Pulso rojo si ya está en pantalla
                    shapeRenderer.setProjectionMatrix(screenMatrix);
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                    shapeRenderer.setColor(1f, 0.1f, 0.1f, 1f);
                    shapeRenderer.circle(worldPos.x, worldPos.y, 30f);
                    shapeRenderer.end();
                }

                batch.setProjectionMatrix(camera.combined);
            });
    }

    // ── Renderizado del mundo ─────────────────────────────────

    private void renderGameplay(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me != null) {
            camera.position.set(me.getPosition().x(), me.getPosition().y(), 0);
            camera.update();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapa, 0, 0, 5000, 4600);
        for (PlayerView pv : snapshot.getPlayers()) {
            if (!pv.isAlive()) {
                playerRenderer.draw(batch, pv.getPosition().x(), pv.getPosition().y(),
                    pv.getId(), 1, false, false);
            }
        }
        batch.end();

        renderTasks(snapshot);

        batch.begin();
        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive()) {
                boolean isMe   = pv.getId().equals(myPlayerId);
                int     dir    = isMe ? inputHandler.getDireccion() : pv.getDirection();
                boolean moving = pv.isMoving();
                playerRenderer.draw(batch, pv.getPosition().x(), pv.getPosition().y(),
                    pv.getId(), dir, moving, true);
            }
        }
        batch.end();
    }

    // ── Tasks en el mundo ─────────────────────────────────────

    private void renderTasks(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        shapeRenderer.setProjectionMatrix(camera.combined);

        for (TaskView tv : snapshot.getTasks()) {
            float x = tv.getPosition().x();
            float y = tv.getPosition().y();

            float dist = (me != null)
                ? com.badlogic.gdx.math.Vector2.dst(
                me.getPosition().x(), me.getPosition().y(), x, y)
                : Float.MAX_VALUE;

            boolean nearby    = dist <= 150f;
            boolean completed = tv.isCompleted();
            String  spritePath = tv.getMapSpritePath();

            if (spritePath != null) {
                taskSprites.computeIfAbsent(spritePath, Texture::new);
                Texture tex     = taskSprites.get(spritePath);
                float   targetH = 80f * tv.getMapSpriteScale();
                float   targetW = targetH * ((float) tex.getWidth() / tex.getHeight());

                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                if (completed)     batch.setColor(0.4f, 0.4f, 0.4f, 0.7f);
                else if (nearby)   batch.setColor(1f, 1f, 0.5f, 1f);
                else               batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(tex, x - targetW / 2f, y - targetH / 2f, targetW, targetH);
                batch.setColor(1f, 1f, 1f, 1f);
                batch.end();

                if (nearby && !completed) {
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                    shapeRenderer.setColor(Color.YELLOW);
                    shapeRenderer.circle(x, y, (80f / 2f) + 8f);
                    shapeRenderer.end();
                }
            } else {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                if (completed) shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 0.7f);
                else shapeRenderer.setColor(nearby ? Color.YELLOW
                    : new Color(0.8f, 0.8f, 0f, 0.7f));
                shapeRenderer.circle(x, y, 20);
                shapeRenderer.end();

                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(completed ? Color.DARK_GRAY
                    : (nearby ? Color.WHITE : Color.YELLOW));
                shapeRenderer.circle(x, y, 30);
                shapeRenderer.end();
            }
        }
    }

    // ── Minijuego overlay ─────────────────────────────────────

    private void renderMinigameOverlay(
        com.amongus.core.api.minigame.MinigameScreen minigame, float delta) {
        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(pixelRojo, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        minigame.render(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) minigame.cancel();
    }

    // ── Barra de progreso ─────────────────────────────────────

    private void renderProgressBar(GameSnapshot snapshot) {
        int total     = snapshot.getTotalTasks();
        int completed = snapshot.getCompletedTasks();
        if (total == 0) return;

        float progress  = (float) completed / total;
        float barWidth  = 200f;
        float barHeight = 16f;
        float x         = 20f;
        float y         = Gdx.graphics.getHeight() - 40f;

        // Botón minimapa
        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        batch.draw(minimapButtonTexture,
            minimapButtonRect.x, minimapButtonRect.y,
            minimapButtonRect.width, minimapButtonRect.height);
        batch.end();

        // Barra
        shapeRenderer.setProjectionMatrix(screenMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.setColor(internetSabotaged
            ? new Color(0.7f, 0.1f, 0.1f, 1f)
            : new Color(0.2f, 0.8f, 0.2f, 1f));
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

    // ── Efectos y HUD ─────────────────────────────────────────

    private void renderBloodOverlay() {
        float overlay = inputHandler.getBloodOverlay();
        if (overlay <= 0) return;

        Matrix4 saved = batch.getProjectionMatrix().cpy();
        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        batch.setColor(1, 0, 0, overlay);
        batch.draw(pixelRojo, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(1, 1, 1, 1);
        batch.end();
        batch.setProjectionMatrix(saved);
    }

    private void drawReportHUD() {
        Matrix4 saved = batch.getProjectionMatrix().cpy();
        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        batch.setColor(1f, 0.9f, 0f, 0.85f);
        batch.draw(pixelRojo,
            Gdx.graphics.getWidth() / 2f - 80, 60, 160, 36);
        batch.setColor(1, 1, 1, 1);
        batch.end();
        batch.setProjectionMatrix(saved);
    }

    // ── Votación ──────────────────────────────────────────────

    private void renderVoting(GameSnapshot snapshot) {
        ScreenUtils.clear(0.1f, 0.1f, 0.3f, 1);
        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();

        font.draw(batch, "REUNION DE EMERGENCIA",
            50, Gdx.graphics.getHeight() - 30);

        PlayerId reporterId = inputHandler.getReporterId();
        if (reporterId != null) {
            String texto = reporterId.equals(myPlayerId)
                ? "Tú reportaste un cuerpo"
                : "Un jugador reportó un cuerpo";
            font.draw(batch, texto, 50, Gdx.graphics.getHeight() - 55);
        }

        font.draw(batch, "Vota para expulsar:", 50, Gdx.graphics.getHeight() - 80);

        int i = 0;
        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive()) {
                String nombre = pv.getId().equals(myPlayerId) ? ">> TU <<" : pv.getName();
                font.draw(batch, "[" + (i + 1) + "] " + nombre,
                    50, Gdx.graphics.getHeight() - 120 - (i * 30));
                i++;
            }
        }

        font.draw(batch, "[ENTER] Confirmar voto", 50, 90);
        font.draw(batch, "[S] SKIP - No votar",    50, 60);
        batch.end();
    }

    // ── Fin de partida ────────────────────────────────────────

    private void renderEndGame() {
        String result = engine.getGameResult();
        if ("IMPOSTOR".equals(result)) ScreenUtils.clear(0.7f, 0f, 0f, 1);
        else                           ScreenUtils.clear(0f, 0.2f, 0.7f, 1);

        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float cx = Gdx.graphics.getWidth()  / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;

        batch.begin();
        if ("IMPOSTOR".equals(result)) {
            font.draw(batch, "EL IMPOSTOR GANA",              cx - 80,  cy + 40);
            font.draw(batch, "Los crewmates fueron eliminados", cx - 120, cy);
        } else {
            font.draw(batch, "LOS CREWMATES GANAN",           cx - 90,  cy + 40);
            font.draw(batch, "El impostor fue expulsado",     cx - 100, cy);
        }
        batch.end();
    }

    // ── Helpers ───────────────────────────────────────────────

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(myPlayerId))
            .findFirst().orElse(null);
    }

    // ── Ciclo de vida ─────────────────────────────────────────

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        batch.dispose();
        mapa.dispose();
        pixelRojo.dispose();
        font.dispose();
        playerRenderer.dispose();
        shapeRenderer.dispose();
        taskArrowRenderer.dispose();
        taskSprites.values().forEach(Texture::dispose);
        minimap.dispose();
        minimapButtonTexture.dispose();
        sabotageButtonTexture.dispose();
        sabotageMapOverlay.dispose();
    }
}
