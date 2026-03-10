package com.amongus.core.impl.player;

import com.amongus.core.api.player.Player;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.model.Position;

public class PlayerImpl implements Player {

    private final PlayerId id;
    private final String name;
    private Position position;
    private SkinColor skinColor;


    private Role role;
    private boolean alive;
    private boolean connected;
    private boolean venting = false;

    private boolean moving = false;
    private int direction = 1;

    public PlayerImpl(PlayerId id, String name, SkinColor skinColor){
        this.id = id;
        this.name = name;
        this.skinColor = skinColor;
        this.alive = true;
        this.connected = true;
        this.position=new Position(2500,2500);

    }

    public void revive() {
        this.alive = true;
        this.venting = false;
    }

    //Implementa los metodos definidos en la intrface Player
    @Override
    public PlayerId getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setSkinColor(SkinColor color) {
        this.skinColor = color;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) { this.position = position; }

    @Override
    public void updatePosition(Position targetPos) {
        this.position = targetPos;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public boolean alive() {
        return this.alive;
    }

    @Override
    public boolean connected() {
        return connected;
    }


    @Override
    public void move(float deltaX, float deltaY) {
        this.position = new Position(this.position.x() + deltaX, this.position.y() + deltaY);
    }

    @Override
    public boolean isVenting() { return venting; }

    @Override
    public void setVenting(boolean venting) { this.venting = venting; }

    /*Metodos que usan gameSesion*/


    public void assignRole(Role role){
        this.role = role;
    }

    public void kill(){
        this.alive = false;
    }

    public void disconnect(){
        this.connected = false;
    }

    public int getDirection() {
        return direction;
    }

    public SkinColor getSkinColor() {
        return skinColor;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }
}
