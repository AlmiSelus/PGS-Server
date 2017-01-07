package com.almi.pgs.server;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.GameState;
import com.almi.pgs.game.packets.LogoutPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.game.packets.PlayerTakeFlagPacket;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.authentication.AuthenticationListener;
import com.almi.pgs.server.authentication.SimpleAuthenticationListener;
import com.almi.pgs.server.listeners.AuthPacketListener;
import com.almi.pgs.server.listeners.LogoutPacketListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    private final List<PlayerThread> playerThreads;
    private final PacketManager packetManager;
	private final Game gameState;

    private final static Object lock = new Object();


    public PlayerThread(PacketManager pm, ReliableSocket socket, List<PlayerThread> playerSockets, int teamID, Game gameState) {
        this.socket = socket;
        this.playerThreads = playerSockets;
        packetManager = pm;
        this.teamID = (byte) teamID;
		this.gameState = gameState;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            new PlayerSocketReaderThread(socket, playerThreads, this).start();
            log.info("Threads started");
        } catch (Exception e) {
            interrupt();
            try {
                join();
            } catch (InterruptedException e1) {
                log.info(ExceptionUtils.getStackTrace(e1));
            }
        }
    }

    public List<PlayerThread> getPlayerThreads() {
        return playerThreads;
    }

    public ReliableSocket getSocket() {
        return socket;
    }

    public byte getTeamID() {
        return teamID;
    }

    public byte getPlayerID() {
        return playerID;
    }

    public void setPlayerID(byte playerID) {
        this.playerID = playerID;
    }

    private class PlayerSocketReaderThread extends Thread {

        private final PlayerThread playerThread;
        private AuthenticationListener authListener;

        private ReliableSocket socket;

        public PlayerSocketReaderThread(ReliableSocket socket, List<PlayerThread> sockets, PlayerThread playerThread) {
            this.socket = socket;
            this.authListener = new SimpleAuthenticationListener(socket, sockets);
            this.playerThread = playerThread;
            this.setDaemon(true);
            packetManager.addPacketListener(new AuthPacketListener(authListener, playerThread, packetManager, playerThreads, gameState));
            packetManager.addPacketListener(new LogoutPacketListener(packetManager, playerThread));
			packetManager.addPacketListener(new PlayerTakeFlagListener());
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
                        if(packetString.contains("}**")) {
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
                playerThreads.remove(playerThread);
                log.info("Sending logout info to all players");
                for (PlayerThread client : playerThreads) {
                    packetManager.sendPacket(client.getSocket(), new LogoutPacket(new Date().getTime(), playerID));
                }
                interrupt();
            }
        }
    }

	private class PlayerTakeFlagListener implements PacketListener {

		@Override
		public void handlePacket(Packet gamePacket) {
			PlayerTakeFlagPacket packet = (PlayerTakeFlagPacket) gamePacket;
		}

		@Override
		public Class<? extends Packet> packetClass() {
			 return PlayerTakeFlagPacket.class;
		}
	}
}
