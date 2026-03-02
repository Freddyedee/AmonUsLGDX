package com.amongus.core;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.actions.LocalActionSender;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.player.InputHandler;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerRenderer;
import com.amongus.core.view.PlayerView;
import com.amongus.core.view.TaskView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen implements Screen {

    // ── Dependencias ─────────────────────────────────────────
    private final GameEngine engine;
    private final PlayerId myPlayerId;

    // ── Rendering ─────────────────────────────────────────────
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private PlayerRenderer playerRenderer;
    private BitmapFont font;

    //para Task
    private ShapeRenderer shapeRenderer;

    // ── Recursos visuales ─────────────────────────────────────
    private Texture mapa;
    private Texture pixelRojo;

    // ── Input ─────────────────────────────────────────────────
    private InputHandler inputHandler;

    public GameScreen(GameEngine engine) {
        this.engine = engine;
        this.myPlayerId = engine.getLocalPlayerId();
    }

    @Override
    public void show() {
        batch          = new SpriteBatch();
        camera         = new OrthographicCamera();
        playerRenderer = new PlayerRenderer();
        font           = new BitmapFont();

        //task
        shapeRenderer = new ShapeRenderer();

        font.setColor(Color.WHITE);
        camera.setToOrtho(false, 800, 480);

        mapa      = new Texture("mapas/mapa1.png");
        pixelRojo = new Texture("sprites/PixelRojo.png");

        // LocalActionSender hoy, NetworkActionSender mañana — una sola línea cambia
        ActionSender sender = new LocalActionSender(engine);
        inputHandler = new InputHandler(sender, engine, myPlayerId);
    }

    // ── Loop principal ────────────────────────────────────────

    @Override
    public void render(float delta) {
        GameSnapshot snapshot = engine.getSnapshot();

        if (engine.getGameResult() != null) {
            renderEndGame();
            return;
        }

        if (snapshot.getState() == GameState.IN_GAME) {
            inputHandler.handleGameInput(snapshot, delta);
            snapshot = engine.getSnapshot();

            PlayerView nearbyCorpse = inputHandler.handleReportInput(snapshot);

            ScreenUtils.clear(0, 0, 0, 1);
            renderGameplay(snapshot);
            renderProgressBar(snapshot);
            renderBloodOverlay();

            if (nearbyCorpse != null) {
                drawReportHUD();
            }

        } else if (snapshot.getState() == GameState.MEETING) {
            inputHandler.handleMeetingInput(snapshot);
            renderVoting(snapshot);
        }
    }

    // ── Renderizado del juego ─────────────────────────────────

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

        renderTasks(snapshot);   // ← entre los dos batch

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

    // ── Efecto de sangre ──────────────────────────────────────

    private void renderBloodOverlay() {
        float overlay = inputHandler.getBloodOverlay();
        if (overlay <= 0) return;

        com.badlogic.gdx.math.Matrix4 saved = batch.getProjectionMatrix().cpy();
        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch.begin();
        batch.setColor(1, 0, 0, overlay);
        batch.draw(pixelRojo, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(1, 1, 1, 1);
        batch.end();

        batch.setProjectionMatrix(saved);
    }

    // ── HUD de reporte ────────────────────────────────────────

    private void drawReportHUD() {
        com.badlogic.gdx.math.Matrix4 saved = batch.getProjectionMatrix().cpy();
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

    // ── Pantalla de votación ───────────────────────────────────

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

        font.draw(batch, "Vota para expulsar:",
            50, Gdx.graphics.getHeight() - 80);

        int i = 0;
        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive()) {
                String nombre = pv.getId().equals(myPlayerId)
                    ? ">> TU <<"
                    : pv.getName();
                font.draw(batch,
                    "[" + (i + 1) + "] " + nombre,
                    50, Gdx.graphics.getHeight() - 120 - (i * 30));
                i++;
            }
        }

        font.draw(batch, "[ENTER] Confirmar voto", 50, 90);
        font.draw(batch, "[S] SKIP - No votar",    50, 60);

        batch.end();
    }


    // ── Pantalla de Tasks ───────────────────────────────────

    private void renderTasks(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);

        shapeRenderer.setProjectionMatrix(camera.combined);

        for (TaskView tv : snapshot.getTasks()) {
            float x = tv.getPosition().x();
            float y = tv.getPosition().y();

            float dist = (me != null)
                ? Vector2.dst(me.getPosition().x(), me.getPosition().y(), x, y)
                : Float.MAX_VALUE;
            boolean nearby     = dist <= 150f;
            boolean completed  = tv.isCompleted();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (completed) {
                shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 0.7f); // gris = completada
            } else {
                shapeRenderer.setColor(nearby ? Color.YELLOW : new Color(0.8f, 0.8f, 0f, 0.7f));
            }
            shapeRenderer.circle(x, y, 20);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(completed ? Color.DARK_GRAY : (nearby ? Color.WHITE : Color.YELLOW));
            shapeRenderer.circle(x, y, 30);
            shapeRenderer.end();
        }
        System.out.println("[renderTasks] tareas en snapshot: " + snapshot.getTasks().size());
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

        // ← Forzar proyección de PANTALLA, no del mundo
        com.badlogic.gdx.math.Matrix4 screenMatrix = new com.badlogic.gdx.math.Matrix4()
            .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        shapeRenderer.setProjectionMatrix(screenMatrix);

        // Fondo gris
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        // Relleno verde
        shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
        shapeRenderer.rect(x, y, barWidth * progress, barHeight);
        shapeRenderer.end();

        // Borde blanco
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.end();

        // Texto — también en coordenadas de pantalla
        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        font.draw(batch, "Tareas: " + completed + "/" + total, x, y + barHeight + 16f);
        batch.end();

        // Restaurar proyección del mundo para el siguiente frame
        batch.setProjectionMatrix(camera.combined);
    }

    // ── Pantalla de fin de juego ───────────────────────────────

    private void renderEndGame() {
        String result = engine.getGameResult();

        if ("IMPOSTOR".equals(result)) {
            ScreenUtils.clear(0.7f, 0.0f, 0.0f, 1);
        } else {
            ScreenUtils.clear(0.0f, 0.2f, 0.7f, 1);
        }

        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        float cx = Gdx.graphics.getWidth()  / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;

        batch.begin();

        if ("IMPOSTOR".equals(result)) {
            font.draw(batch, "EL IMPOSTOR GANA",          cx - 80,  cy + 40);
            font.draw(batch, "Los crewmates fueron eliminados", cx - 120, cy);
        } else {
            font.draw(batch, "LOS CREWMATES GANAN",       cx - 90,  cy + 40);
            font.draw(batch, "El impostor fue expulsado", cx - 100, cy);
        }

        batch.end();
    }

    // ── Helper ────────────────────────────────────────────────

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

        //Task
        shapeRenderer.dispose();
    }
}
