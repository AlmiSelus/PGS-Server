package com.almi.pgs.server.authentication;

import com.almi.pgs.game.GamePacket;
import com.almi.pgs.game.GenericResponse;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.PlayerThread;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Almi on 2016-12-30.
 */
public class SimpleAuthenticationListener implements AuthenticationListener {

    private final static Logger log = LoggerFactory.getLogger(SimpleAuthenticationListener.class);
    private ReliableSocket clientSocket;
    private int playerID;
    private List<ReliableSocket> clients = new ArrayList<>();

    public SimpleAuthenticationListener(ReliableSocket socket, List<ReliableSocket> sockets, int id) {
        clientSocket = socket;
        this.playerID = id;
        clients = sockets;
    }

    @Override
    public void authenticationDataObtained(PacketManager packetManager) {

    }

    @Override
    public void authenticationPassed(PacketManager packetManager) {
        log.info("Authentication passed");
        GamePacket gamePacket = new GamePacket(0,0,0,0,0,0,0);
        gamePacket.setPlayerID((byte) playerID);
        gamePacket.setTeam((byte) 1); //assign all players to same team for now
        packetManager.sendPacket(clientSocket, new GenericResponse("Ok", 200));
        packetManager.sendPacket(clientSocket, gamePacket);
//        new PlayerThread(clientSocket, playerID, packetManager).start();
        /**
         * Notify all connected players that new user has joined.
         */
        for(ReliableSocket socket : clients) {
            if(socket != clientSocket) {
                packetManager.sendPacket(socket, gamePacket);
            }
        }
    }

    @Override
    public void authenticationFailed(PacketManager packetManager, String reason) {
        log.info("Authentication for user failed. Reason: " + reason);
        packetManager.sendPacket(clientSocket, new GenericResponse(reason, 403));
    }

}
