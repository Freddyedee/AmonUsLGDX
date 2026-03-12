package com.amongus.core.impl.sabotage;

/**
 * Gestiona el sistema de sabotaje:
 *  - Solo 1 sabotaje activo a la vez
 *  - Cooldown de 10 segundos entre sabotajes
 *  - Cuando el sabotaje se completa, notifica a GameScreen para restaurar efectos
 */
public class SabotageManager {

    public enum SabotageType {
        NONE,
        INTERNET,
        LIGHTS
    }

    private SabotageType activeSabotage = SabotageType.NONE;
    private float        cooldownTimer  = 0f;
    private static final float COOLDOWN = 20f;

    // Listener para que GameScreen reaccione
    public interface SabotageListener {
        void onSabotageActivated(SabotageType type);
        void onSabotageResolved(SabotageType type);
    }
    private SabotageListener listener;

    public void setListener(SabotageListener listener) {
        this.listener = listener;
    }

    /** Llamar cada frame para reducir el cooldown */
    public void update(float delta) {
        if (cooldownTimer > 0f) {
            cooldownTimer = Math.max(0f, cooldownTimer - delta);
        }
    }

    /** ¿Puede el impostor sabotear ahora? */
    public boolean canSabotage() {
        return activeSabotage == SabotageType.NONE && cooldownTimer <= 0f;
    }

    public float getCooldownRemaining() { return cooldownTimer; }

    public boolean hasSabotageActive() { return activeSabotage != SabotageType.NONE; }

    public SabotageType getActiveSabotage() { return activeSabotage; }

    /**
     * Activar un sabotaje. Solo el impostor puede llamar esto.
     */
    public boolean activateSabotage(SabotageType type) {
        if (!canSabotage()) return false;
        activeSabotage = type;
        System.out.println("[Sabotage] Sabotaje activado: " + type);
        if (listener != null) listener.onSabotageActivated(type);
        return true;
    }

    /**
     * Resolver el sabotaje activo (llamado cuando un crewmate lo completa).
     */
    public void resolveSabotage() {
        if (activeSabotage == SabotageType.NONE) return;
        System.out.println("[Sabotage] Sabotaje resuelto: " + activeSabotage);
        SabotageType resolved = activeSabotage;
        activeSabotage  = SabotageType.NONE;
        cooldownTimer   = COOLDOWN;
        if (listener != null) listener.onSabotageResolved(resolved);
    }

    public void reset() {
        // Guardamos los que estaban activos para apagarlos visualmente
        SabotageType oldSabotage = activeSabotage;

        this.activeSabotage = SabotageType.NONE;
        this.cooldownTimer = COOLDOWN; // Le damos cooldown inicial para que no saboteen en el segundo 1

        // Le decimos a la UI que limpie la pantalla si había un sabotaje activo
        if (listener != null && oldSabotage != SabotageType.NONE) {
            listener.onSabotageResolved(oldSabotage);
        }
    }

    public void forceActivateSabotage(SabotageType type) {
        activeSabotage = type;
        System.out.println("[Sabotage] Sabotaje forzado (Red): " + type);
        if (listener != null) listener.onSabotageActivated(type);
    }

    /** ¿El sabotaje de internet está activo? */
    public boolean isInternetSabotaged() {
        return activeSabotage == SabotageType.INTERNET;
    }

    public boolean isLightsSabotaged() {
        return activeSabotage == SabotageType.LIGHTS;
    }
}
