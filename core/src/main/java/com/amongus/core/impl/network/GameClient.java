package com.amongus.core.impl.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GameClient extends Thread{
    private DataInputStream in;
    private DataOutputStream out;
    private boolean conectado = false;

    //Metodo para conectar el jugador el servidor
    public void conectar(String ip, int puerto) {
        try {
            SocketHints hints = new SocketHints();
            // Evita retrasos en el envío de paquetes pequeños
            hints.tcpNoDelay = true;

            Socket socket = Gdx.net.newClientSocket(Net.Protocol.TCP, ip, puerto, hints);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            conectado = true;
            this.start(); // Iniciamos el hilo para escuchar al servidor
            System.out.println("Conectado al servidor!");
        } catch (Exception e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }

    //Se envia el mensaje solamente al servidor
    public void enviarMensaje(String msg) {
        if (!conectado) return;
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error enviando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (conectado) {
                // Aquí el cliente se queda esperando mensajes del Server
                String mensaje = in.readUTF();
                procesarMensaje(mensaje);
            }
        } catch (IOException e) {
            System.out.println("Desconectado del servidor.");
            conectado = false;
        }
    }

    //fase de prueba
    private void procesarMensaje(String mensaje) {
        // Se divide el mensaje por el separador ":"
        String[] partes = mensaje.split(":");
        String protocolo = partes[0];

        switch (protocolo) {
            case "MOVE":
                // Se divide de la forma MOVE:playerID:x:y
                String id = partes[1];
                float x = Float.parseFloat(partes[2]);
                float y = Float.parseFloat(partes[3]);

                break;

            case "ROL":
                // ROL:Impostor o ROL:Tripulante
                String miRol = partes[1];

                break;

            case "CHAT":
                // CHAT:Jugador:Mensaje
                System.out.println(partes[1] + " dice: " + partes[2]);
                break;

            case "START":
                System.out.println("¡La partida ha comenzado!");
                break;
        }
    }
}
