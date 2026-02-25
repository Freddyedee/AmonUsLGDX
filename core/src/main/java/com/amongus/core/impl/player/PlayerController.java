package com.amongus.core.impl.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class PlayerController {
    private ShowPlayer showPlayer;
    private int direccion;

    public PlayerController(ShowPlayer showPlayer) {
        this.showPlayer=showPlayer;
        this.direccion=1;
    }

    public int getDireccion() {
        return direccion;
    }

    //funcion que recibe si el jugador se esta moviendo
    //si el jugador se va hacia la izquierda
    //y actualiza la posicion
    public boolean playerController(){
        //velocidad en la que se mueve el jugador
        int speed=5;
        boolean move=false;
        //recibimos la direccion en la cual se quiere mover el jugador y actualizamos los datos
        if(Gdx.input.isKeyPressed(Input.Keys.A)&&Gdx.input.isKeyPressed(Input.Keys.W)){
            showPlayer.getPlayer().move(-speed,speed);
            direccion=-1;
            return move=true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)&&Gdx.input.isKeyPressed(Input.Keys.S)){
            showPlayer.getPlayer().move(-speed,-speed);
            direccion=-1;
            return move=true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)&&Gdx.input.isKeyPressed(Input.Keys.W)){
            showPlayer.getPlayer().move(speed,speed);
            direccion=1;
            return move=true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)&&Gdx.input.isKeyPressed(Input.Keys.S)){
            showPlayer.getPlayer().move(speed,-speed);
            direccion=1;
            return move=true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)){
            showPlayer.getPlayer().move(-speed,0);
            direccion=-1;
            return move=true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)){
            showPlayer.getPlayer().move(speed,0);
            direccion=1;
            return move=true;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.W)){
            showPlayer.getPlayer().move(0,speed);
            direccion=1;
            return move=true;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)){
            showPlayer.getPlayer().move(0,-speed);
            direccion=1;
            return move=true;
        }
        return false;
    }

}
