package com.amongus.core.impl.network;

import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.model.Position;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class GameClient extends Thread{
    private DataInputStream in;
    private DataOutputStream out;
    private boolean conectado = false;

    private final GameEngine engine;
    private final boolean isHost;
    private final PlayerId myPlayerId;
    private final String myName;

    // Nuevo constructor con más contexto
    public GameClient(GameEngine engine, boolean isHost, PlayerId myPlayerId, String myName){
        this.engine = engine;
        this.isHost = isHost;
        this.myPlayerId = myPlayerId;
        this.myName = myName;
    }

    public void conectar(String ip, int puerto) {
        try {
            SocketHints hints = new SocketHints();
            hints.tcpNoDelay = true;

            Socket socket = Gdx.net.newClientSocket(Net.Protocol.TCP, ip, puerto, hints);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            conectado = true;
            this.setDaemon(true); // Muere cuando se cierra el juego
            this.start();
            System.out.println("Conectado al servidor!");

            // APENAS NOS CONECTAMOS, LE AVISAMOS A TODOS QUE LLEGAMOS
            String myRole = isHost ? "IMPOSTOR" : "CREWMATE";
            enviarMensaje("JOIN:" + myPlayerId.value() + ":" + myName + ":" + myRole);

        } catch (Exception e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }

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
                String mensaje = in.readUTF();
                procesarMensaje(mensaje);
            }
        } catch (IOException e) {
            System.out.println("Desconectado del servidor.");
            conectado = false;
        }
    }

    private void procesarMensaje(String mensaje) {
        // System.out.println("[RED - CLIENTE] Mensaje recibido: " + mensaje);
        String[] partes = mensaje.split(":");
        String protocolo = partes[0];

        switch (protocolo) {
            // --- PROTOCOLOS DE SINCRONIZACIÓN DE JUGADORES ---
            case "JOIN":
                PlayerId newId = engine.spawnPlayerWithId(partes[1], partes[2]);
                engine.assignRole(newId, Role.valueOf(partes[3]));

                String myRole = isHost ? "IMPOSTOR" : "CREWMATE";
                engine.getSnapshot().getPlayers().stream()
                    .filter(p -> p.getId().equals(myPlayerId))
                    .findFirst()
                    .ifPresent(me -> {
                        enviarMensaje("HERE:" + myPlayerId.value() + ":" + myName + ":" + myRole + ":" + me.getPosition().x() + ":" + me.getPosition().y());
                    });
                break;

            case "HERE":
                PlayerId existId = engine.spawnPlayerWithId(partes[1], partes[2]);
                engine.assignRole(existId, Role.valueOf(partes[3]));
                engine.movePlayer(existId, new Position((int)Float.parseFloat(partes[4]), (int)Float.parseFloat(partes[5])));
                break;

            // --- PROTOCOLOS DE ACCIÓN (CON ESCUDO PROTECTOR) ---
            case "MOVE":
                PlayerId idMove = new PlayerId(UUID.fromString(partes[1]));
                // Solo lo movemos si ya terminó de hacer spawn
                if (playerExists(idMove)) {
                    engine.setPlayerMoving(idMove, true, 1);
                    engine.movePlayer(idMove, new Position((int)Float.parseFloat(partes[2]), (int)Float.parseFloat(partes[3])));
                }
                break;

            case "STOP":
                PlayerId idStop = new PlayerId(UUID.fromString(partes[1]));
                if (playerExists(idStop)) {
                    engine.setPlayerMoving(idStop, false, Integer.parseInt(partes[2]));
                }
                break;

            case "KILL":
                PlayerId killerId = new PlayerId(UUID.fromString(partes[1]));
                PlayerId victimId = new PlayerId(UUID.fromString(partes[2]));
                if (playerExists(killerId) && playerExists(victimId)) {
                    engine.requestKill(killerId, victimId);
                }
                break;

            case "REPORT":
                PlayerId reporterId = new PlayerId(UUID.fromString(partes[1]));
                PlayerId corpseId = new PlayerId(UUID.fromString(partes[2]));
                if (playerExists(reporterId)) {
                    engine.reportBody(reporterId, corpseId);
                }
                break;

            case "VOTE":
                PlayerId voterId = new PlayerId(UUID.fromString(partes[1]));
                PlayerId targetId = partes[2].equals("SKIP") ? null : new PlayerId(UUID.fromString(partes[2]));
                if (playerExists(voterId)) {
                    engine.castVote(new com.amongus.core.impl.voting.VoteImpl(voterId, targetId));
                }
                break;
        }
    }

    // Metodo para verificar si el jugador ya existe en nuestro mapa local
    private boolean playerExists(PlayerId id) {
        return engine.getSnapshot().getPlayers().stream()
            .anyMatch(p -> p.getId().equals(id));
    }
}
