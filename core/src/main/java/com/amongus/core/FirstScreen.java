package com.amongus.core;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.impl.player.PlayerController;
import com.amongus.core.impl.player.PlayerImpl;
import com.amongus.core.impl.player.ShowPlayer;
import com.amongus.infrastructure.network.Server;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {
    private SpriteBatch batch;//para dibujar
    private ShowPlayer showPlayer;
    private PlayerController controller;
    private OrthographicCamera camera;
    private Texture mapa;
    private PlayerId id;

    //metodo que funciona como constructor que se ejecuta cuando se crea la interfaz
    //y permite crear nuestro escenario
    @Override
    public void show() {
        id=PlayerId.random();
        PlayerImpl player=new PlayerImpl(id,"hola");//prueba
        //creamos la camara para que siga al jugador
        camera=new OrthographicCamera();
        //le establecemos el tamaño
        camera.setToOrtho(false, 800, 480);
        batch=new SpriteBatch();
        //generamos el aspecto del jugador y le establecemos los controles
        showPlayer =new ShowPlayer(player);
        showPlayer.showPlayer();
        controller=new PlayerController(showPlayer);
        mapa=new Texture("mapas/mapa1.png");

        //prueba del servidor
        Server server=new Server();
        server.start();
    }

    //funcion que se ejecuta una y otra vez permitiendo ir actualizando la pantalla
    @Override
    public void render(float delta) {
        //Limpia pantalla y muestra fondo negro
        com.badlogic.gdx.utils.ScreenUtils.clear(0, 0, 0, 1);
        boolean isMoving= controller.playerController();
        int direccion=controller.getDireccion();

        //obtenemos la posicion
        float posX = showPlayer.getPlayer().getPosition().x();
        float posY = showPlayer.getPlayer().getPosition().y();

        camera.position.set(posX + 25, posY + 25, 0);
        camera.update();

        //vinculamos la camara con el batch
        //y le decimos que dibuje desde su perspectiva
        batch.setProjectionMatrix(camera.combined);
        // se empieza a dibujar
        batch.begin();
        batch.draw(mapa,0,0,5000,4600);
        //obtenemos la textura del jugador
        //se este moviendo o no
        Texture frameActual= showPlayer.getFrame(isMoving);

        //dibujamos
        if(direccion==1) {
            //viendo hacia la derecha
            batch.draw(frameActual, posX, posY, 50, 50);
        }else{
            //viendo hacia la izquierda
            batch.draw(frameActual, posX + 50, posY, -50, 50);
        }
        //termina de dibujar
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
    }
}
