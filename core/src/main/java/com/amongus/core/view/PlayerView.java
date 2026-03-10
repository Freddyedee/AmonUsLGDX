package com.amongus.core.view;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.player.Role; // IMPORTANTE: Usamos tu propia clase Role
import com.amongus.core.model.Position;
import java.util.Objects;

public final class PlayerView {
    private final PlayerId id;
    private final boolean alive;
    private final Position position;
    private final String name;
    private SkinColor skinColor;

    private boolean moving;
    private int direction = 1;

    private final Role role;
    private boolean venting = false;

    // Constructor
    public PlayerView(PlayerId id, boolean alive, Position position, String name, SkinColor skinColor, Role role) {
        this.id = Objects.requireNonNull(id, "Id no puede ser null");
        this.alive = alive;
        this.position = Objects.requireNonNull(position, "Posición no puede ser null");
        this.name = name;
        this.skinColor = skinColor;
        this.role = role; // Guardamos el rol
    }

    // --- NUEVOS GETTERS Y SETTERS ---
    public Role getRole() { return role; }

    public boolean isVenting() { return venting; }
    public void setVenting(boolean venting) { this.venting = venting; }

    // --- GETTERS Y SETTERS CLÁSICOS ---
    public PlayerId getId() { return id; }
    public boolean isMoving() { return moving; }
    public void setMoving(boolean moving) { this.moving = moving; }
    public int getDirection() { return direction; }
    public void setDirection(int direction) { this.direction = direction; }
    public String getName() { return name; }
    public boolean isAlive() { return alive; }
    public Position getPosition() { return position; }
    public SkinColor getSkinColor() { return skinColor; }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        PlayerView that = (PlayerView) object;
        return alive == that.alive && Objects.equals(id, that.id) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, alive, position);
    }

    @Override
    public String toString() {
        return "PlayerView{" +
            "id=" + id +
            ", alive=" + alive +
            ", position=" + position +
            ", role=" + role +
            '}';
    }
}
