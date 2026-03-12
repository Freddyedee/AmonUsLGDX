package com.amongus.core.impl.network;

import com.amongus.core.api.events.TaskCompletedEvent;
import com.amongus.core.api.map.MapType;
import com.amongus.core.api.player.PlayerId;
import com.amongus.core.api.player.Role;
import com.amongus.core.api.player.SkinColor;
import com.amongus.core.api.state.GameState;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.impl.engine.GameEngine;
import com.amongus.core.impl.player.PlayerImpl;
import com.amongus.core.impl.sabotage.SabotageManager;
import com.amongus.core.impl.voting.VoteImpl;
import com.amongus.core.model.Position;
import com.amongus.debug.DebugConfig;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class GameClient extends Thread{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean conectado = false;

    private final GameEngine engine;
    private final boolean isHost;
    private final PlayerId myPlayerId;
    private final String myName;
    private final SkinColor myColor;

    public GameClient(GameEngine engine, boolean isHost, PlayerId myPlayerId, String myName, SkinColor myColor){
        this.engine = engine;
        this.isHost = isHost;
        this.myPlayerId = myPlayerId;
        this.myName = myName;
        this.myColor = myColor;

        engine.getEventBus().subscribe(TaskCompletedEvent.class, event -> {
            // Solo enviamos a la red si la tarea la hicimos nosotros
            if (event.playerId().equals(this.myPlayerId)) {
                String idStr = event.taskId().value().toString();

                // Si nosotros completamos un sabotaje, avisamos que lo apaguen
                if (idStr.equals("11111111-1111-1111-1111-111111111111") ||
                    idStr.equals("22222222-2222-2222-2222-222222222222")) {
                    enviarMensaje("RESOLVE_SABOTAGE");
                } else {
                    // Si completamos tarea normal, avisamos que suban la barra
                    enviarMensaje("PROGRESS_INC:" + event.playerId().value());
                }
            }
        });
    }

    public void conectar(String ip, int puerto) {
        try {
            SocketHints hints = new SocketHints();
            hints.tcpNoDelay = true;

            this.socket = Gdx.net.newClientSocket(Net.Protocol.TCP, ip, puerto, hints);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            conectado = true;
            this.setDaemon(true); // Muere cuando se cierra el juego
            this.start();
            System.out.println("Conectado al servidor!");

            // APENAS NOS CONECTAMOS, LE AVISAMOS A TODOS QUE LLEGAMOS
            String myRole = isHost ? "IMPOSTOR" : "CREWMATE";
            enviarMensaje("JOIN:" + myPlayerId.value() + ":" + myName + ":" + myRole + ":" + myColor.name());

        } catch (Exception e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
    }

    public void disconnect() {
        // 1. Avisamos a todos antes de cerrar la conexión
        if (conectado) {
            enviarMensaje("QUIT:" + myPlayerId.value());
        }
        conectado = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.dispose();
            System.out.println("Desconectado correctamente del servidor.");
        } catch (Exception e) {
            System.out.println("Error al desconectar: " + e.getMessage());
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
        Gdx.app.postRunnable(() -> {
            try {
                String[] partes = mensaje.split(":");
                String protocolo = partes[0];
                switch (protocolo) {
                    case "JOIN":
                        // Bloqueo de entrada si la partida ya empezó
                        if (engine.getSnapshot().getState() != GameState.LOBBY) {
                            System.out.println("[RED] Conexión rechazada: La partida ya está en curso.");
                            break; // Ignoramos completamente a este jugador
                        }
                        SkinColor newColor = SkinColor.valueOf(partes[4]); // Leemos el color
                        PlayerId newId = engine.spawnPlayerWithId(partes[1], partes[2], newColor);
                        // Solo continuamos si el engine aceptó al jugador
                        if (newId != null) {
                            engine.assignRole(newId, Role.valueOf(partes[3]));

                            String myRole = isHost ? "IMPOSTOR" : "CREWMATE";
                            engine.getSnapshot().getPlayers().stream().filter(p -> p.getId().equals(myPlayerId)).findFirst().ifPresent(me -> {
                                // Añadimos el color al final del paquete HERE también
                                enviarMensaje("HERE:" + myPlayerId.value() + ":" + myName + ":" + myRole + ":" + me.getPosition().x() + ":" + me.getPosition().y() + ":" + myColor.name());
                            });
                        }
                        break;
                    case "HERE":
                        SkinColor existColor = SkinColor.valueOf(partes[6]); // Leemos el color (índice 6)
                        PlayerId existId = engine.spawnPlayerWithId(partes[1], partes[2], existColor);

                        // Solo continuamos si el engine aceptó al jugador
                        if (existId != null) {
                            engine.assignRole(existId, Role.valueOf(partes[3]));
                            engine.movePlayer(existId, new Position(Float.parseFloat(partes[4]), Float.parseFloat(partes[5])));
                        }
                        break;
                    // Inicio oficial de partida
                    case "START_GAME":
                        if (!isHost) {
                            engine.startGameClient(partes[1]);
                        }
                        break;
                    case "MOVE":
                        PlayerId moveId = new PlayerId(java.util.UUID.fromString(partes[1]));
                        float x = Float.parseFloat(partes[2]);
                        float y = Float.parseFloat(partes[3]);
                        int dir = Integer.parseInt(partes[4]);
                        // Prevenir teletransportes fantasma
                        engine.getSnapshot().getPlayers().stream()
                            .filter(p -> p.getId().equals(moveId))
                            .findFirst()
                            .ifPresent(p -> {
                                // Calculamos la distancia entre donde está el jugador y donde el paquete dice que debería estar
                                float dist = Vector2.dst(p.getPosition().x(), p.getPosition().y(), x, y);
                                // Si la distancia es gigante (> 200px), es un paquete viejo de antes de iniciar partida. Lo ignoramos.
                                if (dist > 200f) {
                                    // Descomenta esto si quieres ver cuando el juego te salva de un crash:
                                    // System.out.println("[RED] Paquete fantasma ignorado.");
                                    return;
                                }
                                // Si la distancia es normal, aplicamos el movimiento
                                engine.forceMovePlayer(moveId, new Position(x, y));
                                engine.updatePlayerDirection(moveId, dir);
                                engine.setPlayerMoving(moveId, true, dir);
                            });
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
                            engine.castVote(new VoteImpl(voterId, targetId));
                        }
                        break;
                    case "VENT":
                        PlayerId vId = new PlayerId(java.util.UUID.fromString(partes[1]));
                        if (playerExists(vId)) {
                            boolean exiting = Boolean.parseBoolean(partes[2]);
                            Position target = exiting ? null : new Position(Float.parseFloat(partes[3]), Float.parseFloat(partes[4]));
                            engine.processVentAction(vId, target, exiting);
                        }
                        break;
                    case "COLOR":
                        PlayerId colorId = new PlayerId(java.util.UUID.fromString(partes[1]));
                        newColor = SkinColor.valueOf(partes[2]);
                        if (playerExists(colorId)) {
                            engine.changePlayerColor(colorId, newColor);
                        }
                        break;
                    case "CHAT":
                        PlayerId chatSenderId = new PlayerId(java.util.UUID.fromString(partes[1]));
                        // Reconstruir el mensaje en caso de que contenga ':'
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < partes.length; i++) {
                            sb.append(partes[i]);
                            if (i < partes.length - 1) sb.append(":");
                        }
                        String chatMessage = sb.toString();
                        if (playerExists(chatSenderId)) {
                            engine.addChatMessage(chatSenderId, chatMessage);
                        }
                        break;
                    case "CHANGE_MAP":
                        MapType nuevoMapa = MapType.valueOf(partes[1]);
                        engine.setMapType(nuevoMapa);
                        System.out.println("[RED] El Host cambió el mapa a: " + nuevoMapa.name());
                        break;
                    case "RESTART":
                        engine.restartToLobby();
                        break;
                    case "QUIT":
                        PlayerId quitId = new PlayerId(java.util.UUID.fromString(partes[1]));
                        engine.removePlayer(quitId);
                        break;
                    // --- SINCRONIZACIÓN DE SABOTAJES ---
                    case "SABOTAGE":
                        SabotageManager.SabotageType sabotageType = SabotageManager.SabotageType.valueOf(partes[2]);
                        engine.getSabotageManager().forceActivateSabotage(sabotageType);
                        engine.activateSabotageTask(sabotageType);
                        System.out.println("[RED] Sabotaje recibido y activado: " + sabotageType.name());
                        break;
                    // --- SINCRONIZACIÓN DE DEBUG (Victorias) ---
                    case "DEBUG_WIN_COND":
                        // Formato esperado: DEBUG_WIN_COND:true/false
                        boolean ignoreWins = Boolean.parseBoolean(partes[1]);
                        DebugConfig.IGNORE_WIN_CONDITIONS = ignoreWins;
                        System.out.println("[RED] Condición de victoria ignorada sincronizada a: " + ignoreWins);
                        break;
                    // --- SINCRONIZACIÓN DE TAREAS COMPLETADAS ---
                    case "RESOLVE_SABOTAGE":
                        // Obligamos al juego a apagar la alarma
                        engine.getSabotageManager().resolveSabotage();
                        System.out.println("[RED] Sabotaje apagado por el servidor.");
                        break;

                    case "PROGRESS_INC":
                        // Recibimos que alguien avanzó una tarea.
                        PlayerId pId = new PlayerId(java.util.UUID.fromString(partes[1]));
                        // Creamos un UUID falso e inofensivo solo para engañar al sistema y que dispare el evento
                        TaskId fakeId = new TaskId(java.util.UUID.randomUUID());

                        if (playerExists(pId)) {
                            engine.getEventBus().publish(new TaskCompletedEvent(pId, fakeId));
                            System.out.println("[RED] Un tripulante avanzó la barra verde.");
                        }
                        break;
                }
            }   catch (Exception e) {
                // Si llega un paquete corrupto o hay un error de mapa, lo ignoramos y seguimos conectados
                System.out.println("[RED ERROR] Paquete ignorado para evitar desconexión: " + mensaje + " | Causa: " + e.getMessage());
            }
        });
    }

    // Metodo para verificar si el jugador ya existe en nuestro mapa local
    private boolean playerExists(PlayerId id) {
        return engine.getSnapshot().getPlayers().stream()
            .anyMatch(p -> p.getId().equals(id));
    }
}
