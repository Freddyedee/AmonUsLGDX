package com.amongus.core.view.voting;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderiza la pantalla de votación. Responsabilidad única: dibujar.
 *
 * <p>Diseño de la pantalla:
 * <pre>
 *   ┌─────────────────────────────────────┐
 *   │  REUNIÓN DE EMERGENCIA      1:30    │  ← header con timer
 *   ├─────────────────────────────────────┤
 *   │ [■] Jugador1  [VOT] │ [■] Jugador2  │  ← grid 2 columnas
 *   │ [■] Jugador3        │ [✗] Muerto    │
 *   ├─────────────────────────────────────┤
 *   │   [SKIP VOTE]    [CONTINUAR]        │  ← barra inferior
 *   └─────────────────────────────────────┘
 * </pre>
 *
 * <p>Todas las coordenadas son relativas a la resolución real de pantalla
 * ({@code Gdx.graphics.getWidth/Height()}) — funciona en cualquier tamaño.
 *
 * <p>Patrón de interacción (igual que {@code HudRenderer}):
 * <ol>
 *   <li>{@code GameScreen} llama {@link #render} cada frame</li>
 *   <li>{@code GameScreen} llama {@link #handleClick} cuando hay un click</li>
 *   <li>{@code GameScreen} consulta {@link #isVoteClicked()}, {@link #isSkipClicked()},
 *       {@link #isProceedClicked()} y ejecuta la acción de dominio correspondiente</li>
 * </ol>
 */
public class VotingRenderer {

    private static final int COLS = 2;

    // ── Paleta de colores de la UI ────────────────────────────────────
    private static final Color BG_COLOR       = new Color(0.08f, 0.08f, 0.18f, 1f);
    private static final Color HEADER_COLOR   = new Color(0.12f, 0.12f, 0.25f, 1f);
    private static final Color CARD_COLOR     = new Color(0.18f, 0.20f, 0.30f, 1f);
    private static final Color CARD_DEAD      = new Color(0.10f, 0.10f, 0.14f, 1f);
    private static final Color SEPARATOR      = new Color(0.25f, 0.25f, 0.40f, 1f);
    private static final Color VOTE_BTN       = new Color(0.15f, 0.50f, 0.20f, 1f);
    private static final Color VOTE_BTN_HOVER = new Color(0.25f, 0.70f, 0.30f, 1f);
    private static final Color SKIP_BTN       = new Color(0.35f, 0.35f, 0.45f, 1f);
    private static final Color SKIP_BTN_HOVER = new Color(0.50f, 0.50f, 0.60f, 1f);
    private static final Color PROCEED        = new Color(0.20f, 0.55f, 0.25f, 1f);
    private static final Color PROCEED_HOVER  = new Color(0.30f, 0.70f, 0.35f, 1f);

    // ── Recursos gráficos ─────────────────────────────────────────────
    /** Textura 1x1 blanca — base para dibujar rectángulos de color sin ShapeRenderer. */
    private Texture    pixel;
    private BitmapFont font;

    // ── Dependencias ──────────────────────────────────────────────────
    private final VotingState state;
    private       PlayerId    myId;

    // ── Áreas clickeables — recalculadas cada frame en render() ───────
    private final List<CardSlot> cardSlots   = new ArrayList<>();
    private Rectangle            skipRect    = null;
    private Rectangle            proceedRect = null;

    // ── Flags de click — consumidos por GameScreen este frame ─────────
    private PlayerId clickedVoteTarget = null;
    private boolean  skipClicked       = false;
    private boolean  proceedClicked    = false;

    // ── Cache del snapshot para consultas fuera de render() ───────────
    private List<PlayerView> lastPlayers = new ArrayList<>();

    // ══════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR E INICIALIZACIÓN
    // ══════════════════════════════════════════════════════════════════

    /**
     * @param assets assets de votación (reservados para futura integración de sprites)
     * @param state  estado mutable de la votación actual
     */
    public VotingRenderer(VotingAssets assets, VotingState state) {
        this.state = state;
    }

    /**
     * Inicializa recursos que requieren el contexto gráfico de LibGDX.
     * Debe llamarse después de que la pantalla esté activa (en {@code show()}).
     *
     * @param myId ID del jugador local — para marcar su card y ocultar su botón VOT
     */
    public void init(PlayerId myId) {
        this.myId = myId;
        this.font = new BitmapFont();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
    }

    // ══════════════════════════════════════════════════════════════════
    //  RENDER PRINCIPAL
    // ══════════════════════════════════════════════════════════════════

    /**
     * Dibuja la pantalla completa de votación.
     * Requiere que el batch esté en proyección {@code setToOrtho2D(0,0,screenW,screenH)}.
     *
     * @param batch    SpriteBatch activo (begin/end manejados por GameScreen)
     * @param snapshot estado actual del juego
     */
    public void render(SpriteBatch batch, GameSnapshot snapshot) {
        float SW = Gdx.graphics.getWidth();
        float SH = Gdx.graphics.getHeight();

        lastPlayers = snapshot.getPlayers();
        cardSlots.clear();
        resetClickFlags();

        drawRect(batch, 0, 0, SW, SH, BG_COLOR);
        drawHeader(batch, SW, SH);
        drawPlayerCards(batch, snapshot.getPlayers(), SW, SH);
        drawBottomBar(batch, SW, SH);
    }

    // ══════════════════════════════════════════════════════════════════
    //  SECCIONES DE DIBUJO
    // ══════════════════════════════════════════════════════════════════

    /**
     * Header superior con título y timer.
     * El timer cambia de color: blanco → amarillo (≤30s) → rojo (≤10s).
     */
    private void drawHeader(SpriteBatch batch, float SW, float SH) {
        float h = SH * 0.12f;
        float y = SH - h;

        drawRect(batch, 0, y, SW, h, HEADER_COLOR);
        drawRect(batch, 0, y - 2, SW, 2, SEPARATOR);

        float t = state.getTimer();
        font.getData().setScale(SW / 550f);

        font.setColor(Color.WHITE);
        font.draw(batch, "REUNIÓN DE EMERGENCIA", SW * 0.04f, y + h * 0.65f);

        font.setColor(t > 30f ? Color.WHITE : t > 10f ? Color.YELLOW : Color.RED);
        font.draw(batch, String.format("%d:%02d", (int)t / 60, (int)t % 60),
            SW * 0.87f, y + h * 0.65f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /**
     * Grid de cards — 2 columnas, centrado verticalmente en el área entre
     * header y barra inferior. Registra las áreas clickeables en {@link #cardSlots}.
     */
    private void drawPlayerCards(SpriteBatch batch, List<PlayerView> players,
                                 float SW, float SH) {
        float headerH = SH * 0.12f;
        float bottomH = SH * 0.14f;
        float areaY   = bottomH;
        float areaH   = SH - headerH - bottomH;

        float padX  = SW * 0.03f;
        float gapX  = SW * 0.02f;
        float gapY  = SH * 0.025f;
        float colW  = (SW - padX * 2 - gapX) / 2f;
        float btnW  = SW * 0.07f;
        float cardW = colW - btnW - SW * 0.01f;
        float cardH = SH * 0.15f;

        int   rows   = (int) Math.ceil(players.size() / 2.0);
        float totalH = rows * cardH + Math.max(0, rows - 1) * gapY;
        float startY = areaY + (areaH - totalH) / 2f + totalH - cardH;

        int col = 0, row = 0;
        for (PlayerView pv : players) {
            float cx = padX + col * (colW + gapX);
            float cy = startY - row * (cardH + gapY);
            float bh = cardH * 0.45f;
            float bx = cx + cardW + SW * 0.008f;
            float by = cy + (cardH - bh) / 2f;

            cardSlots.add(new CardSlot(pv, new Rectangle(bx, by, btnW, bh)));
            drawOneCard(batch, pv, cx, cy, cardW, cardH, bx, by, btnW, bh, SW);

            if (++col >= COLS) { col = 0; row++; }
        }
    }

    /**
     * Dibuja una card individual con fondo, color del jugador, nombre,
     * botón VOT (si aplica) e indicador de voto seleccionado.
     */
    private void drawOneCard(SpriteBatch batch, PlayerView pv,
                             float x, float y, float w, float h,
                             float bx, float by, float bw, float bh,
                             float SW) {
        boolean dead       = !pv.isAlive();
        boolean isMe       = pv.getId().equals(myId);
        boolean isSelected = pv.getId().equals(state.getSelectedTarget());

        // Fondo de la card
        drawRect(batch, x, y, w, h, dead ? CARD_DEAD : CARD_COLOR);

        // Borde amarillo si es el voto actual
        if (isSelected) drawBorder(batch, x, y, w, h, 2f, Color.YELLOW);

        // Cuadrado de color del jugador
        float dotS = h * 0.55f;
        float dotX = x + h * 0.12f;
        float dotY = y + (h - dotS) / 2f;
        Color pc   = dead
            ? toGdxColor(pv.getSkinColor()).mul(0.4f, 0.4f, 0.4f, 1f)
            : toGdxColor(pv.getSkinColor());
        drawRect(batch, dotX, dotY, dotS, dotS, pc);

        // X roja sobre el cuadrado si el jugador está muerto
        if (dead) {
            float m = dotS * 0.15f;
            drawRect(batch, dotX + m, dotY + m, dotS - m * 2, dotS - m * 2,
                new Color(0.8f, 0.1f, 0.1f, 0.75f));
        }

        // Nombre del jugador
        float nameX = dotX + dotS + h * 0.12f;
        font.getData().setScale(SW / 780f);
        font.setColor(dead ? new Color(0.5f, 0.5f, 0.5f, 1f) : Color.WHITE);
        font.draw(batch, (isMe ? "▶ " : "") + pv.getName(), nameX, y + h * 0.65f);

        if (dead) {
            font.getData().setScale(SW / 980f);
            font.setColor(new Color(0.75f, 0.2f, 0.2f, 1f));
            font.draw(batch, "ELIMINADO", nameX, y + h * 0.35f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        // Botón VOT — solo si: jugador vivo, no soy yo, no he votado, el local está vivo
        if (!dead && !isMe && !state.isVoted() && !isLocalDead()) {
            Color bc = isMouseOver(bx, by, bw, bh) ? VOTE_BTN_HOVER : VOTE_BTN;
            drawRect(batch, bx, by, bw, bh, bc);
            font.getData().setScale(SW / 900f);
            font.setColor(Color.WHITE);
            font.draw(batch, "VOT", bx + bw * 0.12f, by + bh * 0.72f);
            font.getData().setScale(1f);
        }

        // Indicador del voto seleccionado
        if (isSelected) {
            font.getData().setScale(SW / 750f);
            font.setColor(Color.YELLOW);
            font.draw(batch, "◄", bx + bw + SW * 0.005f, by + bh * 0.72f);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
        }
    }

    /**
     * Barra inferior con botones SKIP VOTE y CONTINUAR,
     * y el mensaje de estado del voto actual.
     */
    private void drawBottomBar(SpriteBatch batch, float SW, float SH) {
        float barH = SH * 0.12f;
        drawRect(batch, 0, 0, SW, barH, HEADER_COLOR);
        drawRect(batch, 0, barH, SW, 2, SEPARATOR);

        float btnH = barH * 0.60f;
        float btnW = SW * 0.18f;
        float btnY = (barH - btnH) / 2f;

        // SKIP VOTE — oculto si ya votó o si el local está muerto
        if (!state.isVoted() && !isLocalDead()) {
            float skipX = SW * 0.50f - btnW - SW * 0.02f;
            skipRect = new Rectangle(skipX, btnY, btnW, btnH);
            Color sc = isMouseOver(skipRect) ? SKIP_BTN_HOVER : SKIP_BTN;
            drawRect(batch, skipX, btnY, btnW, btnH, sc);
            font.getData().setScale(SW / 820f);
            font.setColor(Color.WHITE);
            font.draw(batch, "SKIP VOTE", skipX + btnW * 0.12f, btnY + btnH * 0.70f);
            font.getData().setScale(1f);
        }

        // CONTINUAR — visible solo cuando isProceedReady
        if (state.isProceedReady()) {
            float procX = SW * 0.50f + SW * 0.02f;
            proceedRect = new Rectangle(procX, btnY, btnW, btnH);
            Color pc = isMouseOver(proceedRect) ? PROCEED_HOVER : PROCEED;
            drawRect(batch, procX, btnY, btnW, btnH, pc);
            font.getData().setScale(SW / 820f);
            font.setColor(Color.WHITE);
            font.draw(batch, "CONTINUAR", procX + btnW * 0.08f, btnY + btnH * 0.70f);
            font.getData().setScale(1f);
        }

        // Mensaje de estado
        font.getData().setScale(SW / 900f);
        font.setColor(state.isVoted() ? Color.GREEN : new Color(0.65f, 0.65f, 0.65f, 1f));
        font.draw(batch,
            state.isVoted() ? "✓ Voto enviado" : "Selecciona un jugador o salta",
            SW * 0.02f, barH * 0.65f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // ══════════════════════════════════════════════════════════════════
    //  DETECCIÓN DE CLICK
    // ══════════════════════════════════════════════════════════════════

    /**
     * Procesa un click en coordenadas raw de pantalla (Y desde arriba).
     * Convierte a coordenadas LibGDX (Y desde abajo) internamente.
     * Los jugadores muertos no pueden interactuar.
     *
     * @param rawX {@code Gdx.input.getX()}
     * @param rawY {@code Gdx.input.getY()}
     */
    public void handleClick(float rawX, float rawY) {
        if (isLocalDead()) return;

        float mx = rawX;
        float my = Gdx.graphics.getHeight() - rawY;

        // Si ya votó, solo puede clickear CONTINUAR
        if (state.isVoted()) {
            if (state.isProceedReady() && proceedRect != null
                && proceedRect.contains(mx, my))
                proceedClicked = true;
            return;
        }

        if (skipRect != null && skipRect.contains(mx, my)) {
            skipClicked = true;
            return;
        }

        for (CardSlot slot : cardSlots) {
            if (slot.player().isAlive()
                && !slot.player().getId().equals(myId)
                && slot.btn().contains(mx, my)) {
                clickedVoteTarget = slot.player().getId();
                return;
            }
        }
    }

    // ── Getters de flags — consumidos por GameScreen ──────────────────

    /** @return true si se clickeó el botón VOT de alguna card este frame */
    public boolean  isVoteClicked()    { return clickedVoteTarget != null; }

    /** @return ID del jugador objetivo del voto, o null si no se votó */
    public PlayerId getVoteTarget()    { return clickedVoteTarget; }

    /** @return true si se clickeó SKIP VOTE este frame */
    public boolean  isSkipClicked()    { return skipClicked; }

    /** @return true si se clickeó CONTINUAR este frame */
    public boolean  isProceedClicked() { return proceedClicked; }

    private void resetClickFlags() {
        clickedVoteTarget = null;
        skipClicked       = false;
        proceedClicked    = false;
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILIDADES DE DIBUJO
    // ══════════════════════════════════════════════════════════════════

    /** Dibuja un rectángulo sólido usando la textura pixel 1x1. */
    private void drawRect(SpriteBatch batch, float x, float y,
                          float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** Dibuja el borde de un rectángulo con el grosor y color dados. */
    private void drawBorder(SpriteBatch batch, float x, float y,
                            float w, float h, float t, Color c) {
        drawRect(batch, x,     y,     w, t, c);
        drawRect(batch, x,     y+h-t, w, t, c);
        drawRect(batch, x,     y,     t, h, c);
        drawRect(batch, x+w-t, y,     t, h, c);
    }

    private boolean isMouseOver(float x, float y, float w, float h) {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean isMouseOver(Rectangle r) {
        return isMouseOver(r.x, r.y, r.width, r.height);
    }

    /**
     * @return true si el jugador local está muerto en el snapshot actual.
     *         Los jugadores muertos no pueden votar ni interactuar con la UI.
     */
    private boolean isLocalDead() {
        return lastPlayers.stream()
            .filter(pv -> pv.getId().equals(myId))
            .findFirst()
            .map(pv -> !pv.isAlive())
            .orElse(false);
    }

    /**
     * Convierte un {@link SkinColor} del dominio a un {@link Color} de LibGDX.
     * Centralizado aquí para no contaminar el dominio con dependencias de LibGDX.
     */
    private Color toGdxColor(SkinColor c) {
        if (c == null) return Color.WHITE;
        return switch (c) {
            case RED    -> Color.RED;
            case BLUE   -> Color.BLUE;
            case GREEN  -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case CYAN   -> Color.CYAN;
            case PURPLE -> new Color(0.6f, 0f,   0.8f, 1f);
            case ORANGE -> new Color(1f,   0.5f, 0f,   1f);
            case PINK   -> new Color(1f,   0.4f, 0.7f, 1f);
            default     -> Color.WHITE;
        };
    }

    // ══════════════════════════════════════════════════════════════════
    //  CICLO DE VIDA
    // ══════════════════════════════════════════════════════════════════

    public void dispose() {
        if (font  != null) font.dispose();
        if (pixel != null) pixel.dispose();
    }

    // ── Record interno ────────────────────────────────────────────────
    private record CardSlot(PlayerView player, Rectangle btn) {}
}
