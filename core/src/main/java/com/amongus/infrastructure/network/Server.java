package com.amongus.infrastructure.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server extends Thread{
    //Lista sincronizada para evitar errores si alguien entra y sale
    //mientras se envian los mensajes
    private final List<PlayerHandler> clients = Collections.synchronizedList(new ArrayList<>());

    //prueba de inicializacion del servidor
    public static void main(String args[]) {
        Server s=new Server();
        s.start();
    }

    @Override
    public void run() {
        //inicializa el servidor
        ServerSocket server=null;
        server = Gdx.net.newServerSocket(Net.Protocol.TCP,5000,null);
        System.out.println("Servidor iniciado ");

        System.out.println("Servidor se ha iniciado");
        while(true) {
            //Espera hasta que se conecte un cliente
            Socket socketClient = server.accept(null);
            //Se inicializa el cliente y se añade a la lista de clientes
            PlayerHandler newPlayer = new PlayerHandler(socketClient, this);
            clients.add(newPlayer);
            newPlayer.start();

            System.out.println("Nuevo jugador conectado. Total: " + clients.size());
            if (clients.size() == 5) {
                System.out.println("Mínimo de jugadores alcanzado. Listos para iniciar.");
            }

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
