package com.almi.pgs.server;

import com.almi.pgs.game.*;
import com.almi.pgs.game.packets.AuthPacket;
import com.almi.pgs.game.packets.GamePacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.authentication.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Created by Almi on 2016-12-30.
 */
public class PlayerThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(PlayerThread.class);

    private ReliableSocket socket;
    private AuthenticationLocalDatabase localDatabase = new AuthenticationLocalDatabase();
    private int playerID;
    private List<ReliableSocket> userSockets;
    private PacketManager packetManager = new PacketManager();


    public PlayerThread(ReliableSocket socket, int playerID, List<ReliableSocket> playerSockets) {
        this.socket = socket;
        this.playerID = playerID;
        this.userSockets = playerSockets;
    }

    @Override
    public void run() {
        try {
            /**
             * Prepare max connections
             */
            new AuthenticationThread(socket, userSockets, playerID).start();
            log.info("Threads started");
        } catch (Exception e) {
            log.error("Server error");
        }
    }

    private class AuthenticationThread extends Thread {

        private AuthenticationListener authListener;

        private ReliableSocket socket;

        public AuthenticationThread(ReliableSocket socket, List<ReliableSocket> sockets, int playerID) {
            this.socket = socket;
            this.authListener = new SimpleAuthenticationListener(socket, sockets, playerID);
            this.setDaemon(true);
            packetManager.addPacketListener(new AuthPacketListener(authListener));
            packetManager.addPacketListener(new GamePacketListener(socket, packetManager));
        }

        @Override
        public void run() {
            try {
                InetSocketAddress clientAddress = (InetSocketAddress) socket.getRemoteSocketAddress();

                log.info("New Connection from " + clientAddress.getHostName() + ":" + clientAddress.getPort() + " Processing...");
                byte[] buffer = new byte[1024];
                InputStream is = socket.getInputStream();

                while ((is.read(buffer)) > 0) {
                    String val = new String(buffer);
                    Packet packet = packetManager.getGeneralGamePacket(val);
//                    log.info("Object is null ? " + Objects.isNull(packet));
                    if(packet != null) {
                        packetManager.handlePacket(packet);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AuthPacketListener implements PacketListener {

        private AuthenticationListener authListener;

        public AuthPacketListener(AuthenticationListener authenticationListener) {
            this.authListener = authenticationListener;
        }

        @Override
        public void handlePacket(Packet p) {
            log.info("In authentication");
            AuthPacket packet = (AuthPacket)p;
            try {
                Optional<Player> optionalPlayer = localDatabase.getPlayer(packet.getLogin());
                if (!optionalPlayer.isPresent()) {
                    throw new IllegalArgumentException("User does not exist");
                }
                Player player = optionalPlayer.get();
                if (!player.getPassword().equals(packet.getPassword())) {
                    throw new IllegalArgumentException("Password incorrect!");
                }

                authListener.authenticationPassed(packetManager);

            } catch (Exception e) {
                authListener.authenticationFailed(packetManager, e.getMessage());
            }
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return AuthPacket.class;
        }
    }

    private class GamePacketListener implements PacketListener {

        private ReliableSocket clientSocket;
        private PacketManager packetManager;

        private GamePacketListener(ReliableSocket socket, PacketManager packetManager) {
            this.clientSocket = socket;
            this.packetManager = packetManager;
        }

        @Override
        public void handlePacket(Packet gamePacket) {
            /**
             * Write to socket - send received packet back :)
             */
            log.info("Send back packet");
            packetManager.sendPacket(clientSocket, gamePacket);
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return GamePacket.class;
        }
    }
}
