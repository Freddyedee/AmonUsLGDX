package com.amongus.core.view.voting;

import com.amongus.core.api.player.PlayerId;

/**
 * Estado puro de la votación en curso. Sin dibujo, sin input.
 *
 * <p>Ciclo de vida por reunión:
 * <ol>
 *   <li>{@link #reset()} — al inicio de cada reunión (llamado por GameScreen)</li>
 *   <li>{@link #update(float)} — cada frame para decrementar el timer</li>
 *   <li>{@link #registerVote(PlayerId)} o {@link #registerSkip()} — al votar</li>
 *   <li>{@link #setProceedReady(boolean) setProceedReady(true)} — habilita el botón CONTINUAR</li>
 * </ol>
 *
 * <p>{@code VotingRenderer} lo lee para dibujar.
 * {@code GameScreen} lo escribe al procesar clicks.
 */
public class VotingState {

    /** Duración de cada reunión en segundos. */
    public static final float VOTE_DURATION = 120f;

    private PlayerId selectedTarget = null;
    private boolean  voted          = false;
    private boolean  skipChosen     = false;
    private boolean  proceedReady   = false;
    private float    timer          = VOTE_DURATION;
    private boolean  timerExpired   = false;

    // ══════════════════════════════════════════════════════════════════
    //  CICLO DE VIDA
    // ══════════════════════════════════════════════════════════════════

    /** Reinicia todo el estado para una nueva reunión. */
    public void reset() {
        selectedTarget = null;
        voted          = false;
        skipChosen     = false;
        proceedReady   = false;
        timer          = VOTE_DURATION;
        timerExpired   = false;
    }

    /**
     * Decrementa el timer cada frame. Se detiene si el timer expiró
     * o si ya está listo para continuar.
     *
     * @param delta segundos transcurridos desde el frame anterior
     */
    public void update(float delta) {
        if (timerExpired || proceedReady) return;
        timer -= delta;
        if (timer <= 0f) {
            timer        = 0f;
            timerExpired = true;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACCIONES
    // ══════════════════════════════════════════════════════════════════

    /**
     * Registra un voto hacia un jugador específico.
     *
     * @param target ID del jugador votado
     */
    public void registerVote(PlayerId target) {
        this.selectedTarget = target;
        this.voted          = true;
    }

    /** Registra que el jugador saltó la votación (skip). */
    public void registerSkip() {
        this.skipChosen = true;
        this.voted      = true;
    }

    // ══════════════════════════════════════════════════════════════════
    //  GETTERS Y SETTERS
    // ══════════════════════════════════════════════════════════════════

    /** @return ID del jugador seleccionado para votar, o {@code null} si no se votó */
    public PlayerId getSelectedTarget() { return selectedTarget; }

    /** @return true si el jugador ya emitió su voto (incluye skip) */
    public boolean isVoted()            { return voted; }

    /** @return true si el jugador eligió saltar la votación */
    public boolean isSkipChosen()       { return skipChosen; }

    /** @return true si el botón CONTINUAR está habilitado */
    public boolean isProceedReady()     { return proceedReady; }

    /** @return segundos restantes en el timer de votación */
    public float getTimer()             { return timer; }

    /** @return true si el timer llegó a cero */
    public boolean isTimerExpired()     { return timerExpired; }

    /**
     * Habilita o deshabilita el botón CONTINUAR.
     * Se activa cuando el jugador vota, hace skip, o el timer expira.
     *
     * @param v true para habilitar CONTINUAR
     */
    public void setProceedReady(boolean v) { proceedReady = v; }
}
