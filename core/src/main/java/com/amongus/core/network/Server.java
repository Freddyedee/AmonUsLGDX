package com.amongus.core.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server extends Thread{
    //Lista sincronizada para evitar errores si alguien entra y sale
    //mientras se envían los mensajes
    private final List<PlayerHandler> clients = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void run() {
        ServerSocket server = null;
        try {
            // Intentamos abrir el puerto 5000
            server = Gdx.net.newServerSocket(Net.Protocol.TCP, 5000, null);
            System.out.println("[SERVIDOR] Servidor se ha iniciado en el puerto 5000");

            while(true) {
                // Espera hasta que se conecte un cliente
                Socket socketClient = server.accept(null);

                // Se inicializa el cliente y se añade a la lista
                PlayerHandler newPlayer = new PlayerHandler(socketClient, this);
                clients.add(newPlayer);
                newPlayer.start();

                System.out.println("[SERVIDOR] Nuevo jugador conectado. Total: " + clients.size());
            }
        } catch (Exception e) {
            // ¡SI EL PUERTO ESTÁ OCUPADO, CAEMOS AQUÍ EN LUGAR DE CERRAR EL JUEGO!
            System.err.println("==================================================");
            System.err.println(" ERROR CRÍTICO DEL SERVIDOR: No se pudo abrir el puerto 5000.");
            System.err.println(" Es probable que otra instancia del juego siga corriendo de fondo.");
            System.err.println(" Detalle técnico: " + e.getMessage());
            System.err.println("==================================================");
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

    //se remueve el jugador que se salga de la partida
    public void DeleteClient(PlayerHandler p) {
        clients.remove(p);
        System.out.println("Jugador removido. Total: " + clients.size());
    }
}
