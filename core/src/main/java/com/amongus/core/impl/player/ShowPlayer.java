package com.amongus.core.impl.player;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;

public final class ShowPlayer extends ApplicationAdapter {

    //Aspecto del jugador
    private Texture[] sprites;//arreglo para almacenar los sprites
    private int frame;
    private PlayerImpl player;

    public ShowPlayer(PlayerImpl player) {
        this.player=player;
        this.sprites = new Texture[13];
        this.frame=0;
    }

    public PlayerImpl getPlayer() {
        return player;
    }

    //funcion que almacena todo el sprite en el arreglo
    public void showPlayer(){
        sprites[0]=new Texture("sprites/idle.png");
        for(int i=1;i<=12;i++){
            //String para especificar que el archivo tiene el el nombre 4 digitos
            // y uno es incremental
            String num= String.format("%04d", i);
            sprites[i]=new Texture("sprites/Walk" + num + ".png");
        }
    }

    //funcion para cambiar de sprite
    public Texture chanceSprite(){
        frame+=1;
        if(frame>=sprites.length){
            frame=0;
        }
        if(sprites[frame]!=null){
            return sprites[frame];
        }
        return null;
    }

    //funcion para mostrar el jugador quieto
    public Texture staticSprite(){
        return sprites[0];
    }

    //funcion para mostrar si se esta moviendo o no
    public Texture getFrame(boolean isMoving) {
        if (isMoving) {
            return chanceSprite();
        }
        return staticSprite();
    }


}
