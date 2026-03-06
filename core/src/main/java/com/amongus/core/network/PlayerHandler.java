package com.amongus.core.network;
import com.badlogic.gdx.net.Socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PlayerHandler extends Thread{
    //atributos de control de cliente-servidor
    private final Server server;
    private DataOutputStream out;
    private DataInputStream in;

    public PlayerHandler(Socket socket, Server server) {
        this.server = server;
        try {
            // Inicializamos los flujos de datos
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Envia un mensaje al servidor para que lo envie a todos
    public void enviarMensaje(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error enviando a cliente: " + e.getMessage());
        }
    }


    @Override
    public void run() {

        try {
            while (true) {
                // Escuchar lo que el jugador manda
                String mensaje = in.readUTF();
                // El servidor le reenvía esto a todos los demás
                server.enviarATodos(mensaje,this);
            }
        } catch (IOException e) {
            System.out.println("Un jugador se ha desconectado.");
        } finally {
            try {
                in.close();
                out.close();
                // Le avisa al servidor para quitarlo de la lista de clientes
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
