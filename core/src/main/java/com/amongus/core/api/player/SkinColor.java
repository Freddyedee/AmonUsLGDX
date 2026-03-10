package com.amongus.core.api.player;

public enum SkinColor {
    // Los nombres deben coincidir EXACTAMENTE con los prefijos de tus imágenes
    AZUL("azul"),
    MARRON("marron"),
    MORADO("morado"), // Este parece ser el azul oscuro normal
    NEGRO("negro"),
    ROJO("rojo"),
    ROSADO("rosado"),
    VIOLETA("violeta");

    private final String prefix;

    SkinColor(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
