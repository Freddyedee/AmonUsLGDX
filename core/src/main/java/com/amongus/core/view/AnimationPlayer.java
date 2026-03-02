package com.amongus.core.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;



//Clase generica para las animaciones.

/**
 * AnimationPlayer reproduce una secuencia de frames una sola vez.
 * Es genérico — sirve para kill, meeting, eject, o cualquier animación.
 *
 * Uso:
 *   animationPlayer.play("animations/kill/", "Dead", 33);
 *   if (animationPlayer.isPlaying()) {
 *       animationPlayer.update(delta);
 *       animationPlayer.draw(batch, x, y, width, height);
 *   }
 */

public class AnimationPlayer {


    private Texture[] frames;
    private int totalFrames = 0;
    private int currentFrames = 0;
    private float timer = 0f;
    private float frameDuration = 0.5f; //SEGUNDOS POR FRAME
    private boolean playing = false;


    public void play(String folder, String prefix, int frameCount, float frameDuration){
        //Liberar frames residuales en caso de que existan.

        this.totalFrames = frameCount;
        this.frameDuration = frameDuration;
        this.currentFrames = 0;
        this.timer = 0f;
        this.playing = true;
        this.frames = new Texture[frameCount];

        for (int i = 0; i < frameCount; i++) {
            String num = String.format("%04d", (i+1));
            String path = folder + prefix +  num + ".png";
            frames[i] = new Texture(Gdx.files.internal(path));
        }
    }

    public void play(String folder, String prefix, int frameCount){
        play(folder, prefix, frameCount, 0.05f);
    }

    public void update(float delta){
        if(!playing) {
            return;
        }

        timer+=delta;

        if(timer>= frameDuration){
            timer = 0f;
            currentFrames++;
            if(currentFrames >= totalFrames){
                currentFrames = totalFrames -1;
                playing = false;
            }
        }
    }

    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        if (frames == null || currentFrames >= totalFrames) return;
        batch.draw(frames[currentFrames], x, y, width, height);
    }

    public void drawCentered(SpriteBatch batch){
        if(frames == null || currentFrames >= totalFrames){
            return;
        }

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        batch.draw(frames[currentFrames], 0, 0, w, h);
    }

    //metodo ara dibujar en la posción en la cual deberia aparecer el cadaver.

    public void drawAtPosition(SpriteBatch batch, float x, float y, float size){
        if(frames  == null || currentFrames >= totalFrames){
            return;
        }
        batch.draw(frames[currentFrames], x, y, size, size); // ✅ sin restar size/2
    }

    public boolean isPlaying(){
        return playing;
    }

    public  boolean isFinished(){
        return !playing && frames != null;
    }

    public void stop(){
        playing = false;
        currentFrames = 0;
    }

    public void dispose(){
        if(frames != null){
            for(Texture t : frames){
                if(t != null){
                    t.dispose();
                }
                playing = false;
            }
            frames = null;
        }
    }





}
