package com.amongus.core.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server extends Thread {
    // CopyOnWriteArrayList es a prueba de bloqueos (thread-safe)
    private final List<PlayerHandler> clients = new CopyOnWriteArrayList<>();
    private ServerSocket serverSocket;

    public Server() {
        serverSocket = Gdx.net.newServerSocket(Net.Protocol.TCP, 5000, null);
        System.out.println("[SERVIDOR] Servidor se ha iniciado en el puerto 5000");
    }

    @Override
    public void run() {
        try {
            while(true) {
                Socket socketClient = serverSocket.accept(null);
                PlayerHandler newPlayer = new PlayerHandler(socketClient, this);
                clients.add(newPlayer);
                newPlayer.start();
                System.out.println("[SERVIDOR] Nuevo jugador conectado. Total: " + clients.size());
            }
        } catch (Exception e) {
            System.out.println("Servidor detenido o error en puerto: " + e.getMessage());
        }
    }

    // Recibe el mensaje del jugador y le avisa a los demas
    public void enviarATodos(String mensaje, PlayerHandler remitente) {
        for(PlayerHandler p : clients) {
            if(p != remitente) {
                p.enviarMensaje(mensaje);
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

    // Se remueve el jugador que se salga de la partida
    public void DeleteClient(PlayerHandler p) {
        clients.remove(p);
        System.out.println("Jugador removido. Total: " + clients.size());
    }
}
