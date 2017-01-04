package com.almi.pgs.server;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.LogoutPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.authentication.AuthenticationListener;
import com.almi.pgs.server.authentication.SimpleAuthenticationListener;
import com.almi.pgs.server.listeners.AuthPacketListener;
import com.almi.pgs.server.listeners.LogoutPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;

/**
 * Created by Almi on 2016-12-30.
 */
public class PlayerThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(PlayerThread.class);

    private ReliableSocket socket;
    private byte playerID;
    private byte teamID;
    private final List<ReliableSocket> userSockets;
    private final PacketManager packetManager;

    private final static Object lock = new Object();


    public PlayerThread(PacketManager pm, ReliableSocket socket, List<ReliableSocket> playerSockets, int teamID) {
        this.socket = socket;
        this.userSockets = playerSockets;
        packetManager = pm;
        this.teamID = (byte) teamID;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            new PlayerSocketReaderThread(socket, userSockets, this).start();
            log.info("Threads started");
        } catch (Exception e) {
            interrupt();
            log.error("Server error");
        }
    }

    public List<ReliableSocket> getUserSockets() {
        return userSockets;
    }

    public ReliableSocket getSocket() {
        return socket;
    }

    private class PlayerSocketReaderThread extends Thread {

        private AuthenticationListener authListener;

        private ReliableSocket socket;

        public PlayerSocketReaderThread(ReliableSocket socket, List<ReliableSocket> sockets, PlayerThread playerThread) {
            this.socket = socket;
            this.authListener = new SimpleAuthenticationListener(socket, sockets);
            this.setDaemon(true);
            packetManager.addPacketListener(new AuthPacketListener(authListener, playerThread, packetManager, teamID, socket, userSockets));
            packetManager.addPacketListener(new LogoutPacketListener(packetManager, playerThread));
        }

        @Override
        public void run() {
            try {
                InetSocketAddress clientAddress = (InetSocketAddress) socket.getRemoteSocketAddress();

                log.info("New Connection from " + clientAddress.getHostName() + ":" + clientAddress.getPort() + " Processing...");

                BufferedInputStream is = new BufferedInputStream(socket.getInputStream());

                while(true) {
                    String packetString = "";
                    boolean end = false;

                    while(!end) {
                        byte[] buffer = new byte[1024];
                        int c = is.read(buffer);
                        packetString += new String(buffer, 0, c);
                        if(packetString.contains("}")) {
                            end = true;
                        }
                    }

                    synchronized (lock) {
                        Packet packet = packetManager.getGeneralGamePacket(packetString);
                        if (packet != null) {
                            packetManager.handlePacket(packet);
                        }
                    }
                }
            } catch (IOException e) {

                if(!socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        log.info("Socket has been closed. Why Am I even here?");
                    }
                }
                userSockets.remove(socket);
                log.info("Sending logout info to all players");
                for (ReliableSocket socket : userSockets) {
                    packetManager.sendPacket(socket, new LogoutPacket(new Date().getTime(), playerID));
                }
                interrupt();
            }
        }
    }

    public byte getPlayerID() {
        return playerID;
    }

    public void setPlayerID(byte playerID) {
        this.playerID = playerID;
    }
}
