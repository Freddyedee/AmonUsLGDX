package com.amongus.core;

import com.amongus.core.api.actions.ActionSender;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.state.GameState;
import com.amongus.core.impl.actions.LocalActionSender;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.player.InputHandler;
import com.amongus.core.model.Position;
import com.amongus.core.view.*;
import com.amongus.core.view.voting.VotingAssets;
import com.amongus.core.view.voting.VotingRenderer;
import com.amongus.core.view.voting.VotingState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Pantalla principal del juego. Gestiona el ciclo de render y delega
 * en subsistemas especializados según el estado actual.
 *
 * <p>Estados manejados:
 * <ul>
 *   <li>{@link GameState#IN_GAME} — gameplay normal (movimiento, kill, reporte)</li>
 *   <li>{@link GameState#MEETING} — pantalla de votación</li>
 *   <li>Resultado final — pantalla de victoria/derrota</li>
 * </ul>
 *
 * <p>No contiene lógica de dominio — delega en {@link GameEngine},
 * {@link InputHandler}, {@link HudRenderer} y {@link VotingRenderer}.
 */
public class GameScreen implements Screen {

    // ── Motor y jugador local ─────────────────────────────────
    private final GameEngine   engine;
    private final PlayerId     myPlayerId;
    private       ActionSender actionSender;

    // ── Rendering general ─────────────────────────────────────
    private SpriteBatch        batch;
    private OrthographicCamera camera;
    private PlayerRenderer     playerRenderer;
    private BitmapFont         font;

    // ── Assets de mapa y efectos ──────────────────────────────
    private Texture mapa;
    private Texture pixelRojo;

    // ── Input y HUD ───────────────────────────────────────────
    private InputHandler inputHandler;
    private HudRenderer  hudRenderer;

    // ── Animación de kill ─────────────────────────────────────
    private AnimationPlayer killAnimation;

    // ── Sistema de votación ───────────────────────────────────
    private VotingAssets   votingAssets;
    private VotingState    votingState;
    private VotingRenderer votingRenderer;
    private boolean        meetingStarted = false;
    private boolean        votingResolved = false;

    // ── Jugadores expulsados por votación (no dejan cadáver) ──
    private final Set<PlayerId> expelledPlayers = new HashSet<>();

    // ══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════

    public GameScreen(GameEngine engine) {
        this.engine     = engine;
        this.myPlayerId = engine.getLocalPlayerId();
    }

    // ══════════════════════════════════════════════════════════════════
    //  CICLO DE VIDA
    // ══════════════════════════════════════════════════════════════════

    @Override
    public void show() {
        batch          = new SpriteBatch();
        camera         = new OrthographicCamera();
        playerRenderer = new PlayerRenderer();
        font           = new BitmapFont();
        hudRenderer    = new HudRenderer();
        killAnimation  = new AnimationPlayer();

        font.setColor(Color.WHITE);
        camera.setToOrtho(false, 800, 480);

        mapa      = new Texture("mapas/mapa1.png");
        pixelRojo = new Texture("fx/PixelRojo.png");

        // Una sola instancia de ActionSender compartida
        actionSender = new LocalActionSender(engine);
        inputHandler = new InputHandler(actionSender, engine, myPlayerId);

        votingAssets   = new VotingAssets();
        votingAssets.load();
        votingState    = new VotingState();
        votingRenderer = new VotingRenderer(votingAssets, votingState);
        votingRenderer.init(myPlayerId);
    }

    // ══════════════════════════════════════════════════════════════════
    //  LOOP PRINCIPAL
    // ══════════════════════════════════════════════════════════════════

    @Override
    public void render(float delta) {
        // El resultado final tiene prioridad sobre cualquier otro estado
        if (engine.getGameResult() != null) {
            renderEndGame();
            return;
        }

        GameSnapshot snapshot = engine.getSnapshot();

        switch (snapshot.getState()) {
            case IN_GAME -> renderInGame(snapshot, delta);
            case MEETING -> renderMeeting(snapshot, delta);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ESTADO: IN_GAME
    // ══════════════════════════════════════════════════════════════════

    /**
     * Renderiza el gameplay normal: movimiento, kills, reportes y HUD.
     */
    private void renderInGame(GameSnapshot snapshot, float delta) {
        inputHandler.handleGameInput(snapshot, delta);
        snapshot = engine.getSnapshot();

        // Trigger de animación de kill — solo en el frame en que ocurre
        if (inputHandler.didKillThisFrame()) {
            killAnimation.play("animations/kill/", "Dead", 33);
            inputHandler.resetKillFlag();
        }

        // Detectar cadáver cercano para el botón de reporte
        PlayerView nearbyCorpse = null;
        if (snapshot.getState() == GameState.IN_GAME) {
            nearbyCorpse = inputHandler.handleReportInput(snapshot);
        }

        ScreenUtils.clear(0, 0, 0, 1);
        renderGameplay(snapshot);
        renderBloodOverlay();

        // El botón kill solo se muestra a los impostores
        PlayerView localView  = findLocalPlayer(snapshot);
        boolean    isImpostor = localView != null && localView.getRole() == Role.IMPOSTOR;

        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudRenderer.draw(batch, inputHandler.isKillReady(),
            nearbyCorpse != null, delta,
            inputHandler.getKillCooldown(), isImpostor);

        if (nearbyCorpse != null) drawReportHUD();
        inputHandler.handleHudClick(hudRenderer, snapshot);

        // Animación de kill sobre la posición del cadáver
        if (killAnimation.isPlaying()) {
            killAnimation.update(delta);
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            Position killPos = inputHandler.getKillPosition();
            if (killPos != null)
                killAnimation.drawAtPosition(batch, killPos.x(), killPos.y(), 50f);
            batch.end();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ESTADO: MEETING
    // ══════════════════════════════════════════════════════════════════

    /**
     * Inicializa el estado de votación al primer frame y delega en renderVoting.
     */
    private void renderMeeting(GameSnapshot snapshot, float delta) {
        if (!meetingStarted) {
            votingState.reset();
            votingResolved = false;
            meetingStarted = true;
        }
        inputHandler.handleMeetingInput(snapshot);
        renderVoting(snapshot, delta);
    }

    /**
     * Dibuja la pantalla de votación y procesa clicks.
     * Auto-skip cuando el timer llega a cero.
     */
    private void renderVoting(GameSnapshot snapshot, float delta) {
        ScreenUtils.clear(0.05f, 0.05f, 0.15f, 1);

        votingState.update(delta);
        if (votingState.isTimerExpired() && !votingState.isVoted()) {
            votingState.registerSkip();
            engine.castVote(new com.amongus.core.impl.voting.VoteImpl(myPlayerId, null));
            votingState.setProceedReady(true);
        }

        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        votingRenderer.render(batch, snapshot);
        batch.end();

        if (Gdx.input.justTouched()) {
            votingRenderer.handleClick(Gdx.input.getX(), Gdx.input.getY());
            handleVotingClicks(snapshot);
        }
    }

    /**
     * Procesa los clicks detectados por {@link VotingRenderer} este frame.
     * Patrón idéntico al de {@link HudRenderer}: el renderer detecta,
     * GameScreen ejecuta la acción de dominio.
     */
    private void handleVotingClicks(GameSnapshot snapshot) {
        if (votingRenderer.isVoteClicked()) {
            PlayerId target = votingRenderer.getVoteTarget();
            votingState.registerVote(target);
            engine.castVote(new com.amongus.core.impl.voting.VoteImpl(myPlayerId, target));
            votingState.setProceedReady(true);
        }

        if (votingRenderer.isSkipClicked()) {
            votingState.registerSkip();
            engine.castVote(new com.amongus.core.impl.voting.VoteImpl(myPlayerId, null));
            votingState.setProceedReady(true);
        }

        // resolveVoting() se llama una sola vez por reunión — flag votingResolved lo garantiza
        if (votingRenderer.isProceedClicked() && !votingResolved) {
            votingResolved = true;
            Optional<PlayerId> expelled = engine.resolveVoting();
            meetingStarted = false;
            expelled.ifPresent(id -> {
                expelledPlayers.add(id);
                inputHandler.markCorpseReported(id);
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  RENDERIZADO DEL MUNDO
    // ══════════════════════════════════════════════════════════════════

    /**
     * Dibuja el mapa, jugadores y nombres.
     * Capas en orden: mapa → muertos → vivos → nombres.
     * Los jugadores expulsados por votación no se renderizan.
     */
    private void renderGameplay(GameSnapshot snapshot) {
        PlayerView me = findLocalPlayer(snapshot);
        if (me != null) {
            camera.position.set(me.getPosition().x(), me.getPosition().y(), 0);
            camera.update();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(mapa, 0, 0, 5000, 4600);

        // Capa 1 — muertos (debajo de los vivos)
        for (PlayerView pv : snapshot.getPlayers()) {
            if (pv.isAlive()) continue;
            if (expelledPlayers.contains(pv.getId())) continue;

            // Ocultar cadáver durante la animación de kill para evitar superposición
            if (killAnimation.isPlaying() && inputHandler.getKillPosition() != null) {
                float dist = com.badlogic.gdx.math.Vector2.dst(
                    pv.getPosition().x(), pv.getPosition().y(),
                    inputHandler.getKillPosition().x(),
                    inputHandler.getKillPosition().y());
                if (dist < 80f) continue;
            }
            playerRenderer.draw(batch,
                pv.getPosition().x(), pv.getPosition().y(),
                pv.getId(), 1, false, false, pv.getSkinColor());
        }

        // Capa 2 — vivos (encima de los muertos)
        for (PlayerView pv : snapshot.getPlayers()) {
            if (!pv.isAlive()) continue;
            boolean isMe = pv.getId().equals(myPlayerId);
            int     dir  = isMe ? inputHandler.getDireccion() : pv.getDirection();
            playerRenderer.draw(batch,
                pv.getPosition().x(), pv.getPosition().y(),
                pv.getId(), dir, pv.isMoving(), true, pv.getSkinColor());
        }

        batch.end();

        // Capa 3 — nombres (batch separado para evitar conflictos de proyección)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (PlayerView pv : snapshot.getPlayers()) {
            if (!pv.isAlive()) continue;
            String nombre = pv.getId().equals(myPlayerId) ? "Tú" : pv.getName();
            font.draw(batch, nombre,
                pv.getPosition().x() - 10,
                pv.getPosition().y() + 60);
        }
        batch.end();
    }

    // ══════════════════════════════════════════════════════════════════
    //  EFECTOS VISUALES
    // ══════════════════════════════════════════════════════════════════

    /** Overlay rojo semitransparente que aparece al ejecutar un kill. */
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

    /** Indicador visual amarillo de cadáver cercano reportable. */
    private void drawReportHUD() {
        com.badlogic.gdx.math.Matrix4 saved = batch.getProjectionMatrix().cpy();
        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.begin();
        batch.setColor(1f, 0.9f, 0f, 0.85f);
        batch.draw(pixelRojo, Gdx.graphics.getWidth() / 2f - 80, 60, 160, 36);
        batch.setColor(1, 1, 1, 1);
        batch.end();
        batch.setProjectionMatrix(saved);
    }

    // ══════════════════════════════════════════════════════════════════
    //  FIN DE JUEGO
    // ══════════════════════════════════════════════════════════════════

    /** Muestra el resultado final de la partida con color según el ganador. */
    private void renderEndGame() {
        boolean impostorWon = "IMPOSTOR".equals(engine.getGameResult());
        if (impostorWon) ScreenUtils.clear(0.7f, 0.0f, 0.0f, 1);
        else             ScreenUtils.clear(0.0f, 0.2f, 0.7f, 1);

        batch.getProjectionMatrix().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float cx = Gdx.graphics.getWidth()  / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;

        batch.begin();
        if (impostorWon) {
            font.draw(batch, "EL IMPOSTOR GANA",               cx - 80,  cy + 40);
            font.draw(batch, "Los crewmates fueron eliminados", cx - 120, cy);
        } else {
            font.draw(batch, "LOS CREWMATES GANAN",      cx - 90,  cy + 40);
            font.draw(batch, "El impostor fue expulsado", cx - 100, cy);
        }
        batch.end();
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ══════════════════════════════════════════════════════════════════

    private PlayerView findLocalPlayer(GameSnapshot snapshot) {
        return snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(myPlayerId))
            .findFirst().orElse(null);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CALLBACKS LIBGDX
    // ══════════════════════════════════════════════════════════════════

    @Override public void resize(int w, int h) {}
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
        killAnimation.dispose();
        votingAssets.dispose();
        votingRenderer.dispose();
    }
}
