package com.almi.pgs.server.authentication;

import com.almi.pgs.game.packets.AuthResponsePacket;
import com.almi.pgs.game.packets.GamePacket;
import com.almi.pgs.game.packets.GenericResponse;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.almi.pgs.server.PlayerThread;
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
	private int hash;
    private List<PlayerThread> clients = new ArrayList<>();

    public SimpleAuthenticationListener(ReliableSocket socket, List<PlayerThread> sockets) {
        clientSocket = socket;
		this.hash = new BigInteger(30, new SecureRandom()).intValue();
        clients = sockets;
    }

    @Override
    public void authenticationDataObtained(PacketManager packetManager) {

    }

    @Override
    public void authenticationPassed(PacketManager packetManager, int playerID, byte teamID) {
        log.info("Authentication passed");
        GamePacket gamePacket = new GamePacket(0,0,0,0,0,0,0);
        gamePacket.setPlayerID((byte) playerID);
        gamePacket.setTeam(teamID); //assign all players to same team for now
        AuthResponsePacket authCorrect = new AuthResponsePacket((short) 200);
        authCorrect.setPlayerID(gamePacket.getPlayerID());
        authCorrect.setTeamID(gamePacket.getTeam());
		authCorrect.setHash(this.hash);
        packetManager.sendPacket(clientSocket, authCorrect);
        packetManager.sendPacket(clientSocket, gamePacket);
//        new PlayerThread(clientSocket, playerID, packetManager).start();
        packetManager.removePacketListener(0);
        /**
         * Notify all connected players that new user has joined.
         */
        clients.stream().filter(client -> client.getSocket() != clientSocket).forEach(client ->
                packetManager.sendPacket(client.getSocket(), gamePacket));
    }

    @Override
    public void authenticationFailed(PacketManager packetManager, String reason) {
        log.info("Authentication for user failed. Reason: " + reason);
        AuthResponsePacket failedPacket = new AuthResponsePacket((short) 403);
        failedPacket.setReason(reason);
        packetManager.sendPacket(clientSocket, failedPacket);
    }

}
