package com.amongus.core.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server extends Thread {
    private final List<PlayerHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private ServerSocket serverSocket; // Atributo de clase

    @Override
    public void run() {
        try {
            // Asignamos a la variable de clase
            serverSocket = Gdx.net.newServerSocket(Net.Protocol.TCP, 5000, null);
            System.out.println("[SERVIDOR] Servidor se ha iniciado en el puerto 5000");

            while(true) {
                Socket socketClient = serverSocket.accept(null);
                PlayerHandler newPlayer = new PlayerHandler(socketClient, this);
                clients.add(newPlayer);
                newPlayer.start();
                System.out.println("[SERVIDOR] Nuevo jugador conectado. Total: " + clients.size());
            }
        } catch (Exception e) {
            // Este catch atrapará el error cuando cerremos el socket desde fuera
            System.out.println("Servidor detenido o error en puerto: " + e.getMessage());
        }
    }

    //Recibe el mensaje del jugador y le avisa a los demas
    public void enviarATodos(String mensaje, PlayerHandler remitente) {
        // Hacemos una copia de la lista para iterar de forma segura
        synchronized(clients) {
            for(PlayerHandler p : clients) {
                if(p != remitente) {
                    p.enviarMensaje(mensaje);
                }
            }
        }
    }

    public void detener() {
        try {
            if (serverSocket != null) {
                serverSocket.dispose();
                System.out.println("Recursos del servidor liberados.");
            }
        } catch (Exception e) {
            System.out.println("Error al detener servidor: " + e.getMessage());
        }
    }

    //se remueve el jugador que se salga de la partida
    public void DeleteClient(PlayerHandler p) {
        clients.remove(p);
        System.out.println("Jugador removido. Total: " + clients.size());
    }
}
