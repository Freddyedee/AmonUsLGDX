package com.amongus.core.api.player;
import com.badlogic.gdx.graphics.Color;

public enum SkinColor {

    RED    (new Color(0.9f, 0.1f, 0.1f, 1f)),
    BLUE   (new Color(0.1f, 0.3f, 0.9f, 1f)),
    GREEN  (new Color(0.1f, 0.8f, 0.2f, 1f)),
    YELLOW (new Color(0.9f, 0.9f, 0.1f, 1f)),
    ORANGE (new Color(0.9f, 0.5f, 0.1f, 1f)),
    PINK   (new Color(0.9f, 0.4f, 0.7f, 1f)),
    PURPLE (new Color(0.6f, 0.1f, 0.9f, 1f)),
    WHITE  (new Color(0.9f, 0.9f, 0.9f, 1f)),
    BLACK  (new Color(0.2f, 0.2f, 0.2f, 1f)),
    CYAN   (new Color(0.1f, 0.9f, 0.9f, 1f));

    private final Color color;

    SkinColor(Color color) { this.color = color; }

    public Color getColor() { return color; }

}
