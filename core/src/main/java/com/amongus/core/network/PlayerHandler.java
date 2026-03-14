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
    private String playerId = null;

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
    public void enviarMensaje(String mensaje) {
        try {
            out.writeUTF(mensaje);
            out.flush();
        } catch (Exception e) {
            // Si falla el envío (porque se desconectó de golpe), lo sacamos de inmediato.
            System.out.println("Error enviando a cliente, forzando desconexión...");
            server.DeleteClient(this);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String mensaje = in.readUTF();

                // Extraer el ID del jugador si es el mensaje inicial
                if (playerId == null) {
                    String[] partes = mensaje.split(":");
                    if (partes.length > 1 && (partes[0].equals("JOIN") || partes[0].equals("HERE"))) {
                        playerId = partes[1];
                    }
                }

                server.enviarATodos(mensaje, this);
            }
        } catch (IOException e) {
            System.out.println("Un jugador se ha desconectado.");
        } finally {
            // Avisar a los demas que se desconecto para evitar fantasmas
            if (playerId != null) {
                server.enviarATodos("QUIT:" + playerId, this);
            }
            // Se borra de la lista del servidor
            server.DeleteClient(this);
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
