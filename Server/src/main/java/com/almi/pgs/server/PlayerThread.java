package com.almi.pgs.server;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.AuthPacket;
import com.almi.pgs.game.packets.GamePacket;
import com.almi.pgs.game.packets.LogoutPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.authentication.AuthenticationListener;
import com.almi.pgs.server.authentication.AuthenticationLocalDatabase;
import com.almi.pgs.server.authentication.Player;
import com.almi.pgs.server.authentication.SimpleAuthenticationListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Almi on 2016-12-30.
 */
public class PlayerThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(PlayerThread.class);

    private ReliableSocket socket;
    private AuthenticationLocalDatabase localDatabase = new AuthenticationLocalDatabase();
    private byte playerID;
    private final List<ReliableSocket> userSockets;
    private final PacketManager packetManager;

    private final static Object lock = new Object();


    public PlayerThread(PacketManager pm, ReliableSocket socket, List<ReliableSocket> playerSockets) {
        this.socket = socket;
        this.userSockets = playerSockets;
        packetManager = pm;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            new AuthenticationThread(socket, userSockets).start();
            log.info("Threads started");
        } catch (Exception e) {
            interrupt();
            log.error("Server error");
        }
    }

    private class AuthenticationThread extends Thread {

        private AuthenticationListener authListener;

        private ReliableSocket socket;

        public AuthenticationThread(ReliableSocket socket, List<ReliableSocket> sockets) {
            this.socket = socket;
            this.authListener = new SimpleAuthenticationListener(socket, sockets);
            this.setDaemon(true);
            packetManager.addPacketListener(new AuthPacketListener(authListener));
            packetManager.addPacketListener(new GamePacketListener(socket, packetManager));
            packetManager.addPacketListener(new LogoutPacketListner());
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
//                    log.info("Received from client " + packetString);
                        Packet packet = packetManager.getGeneralGamePacket(packetString);
                        if (packet != null) {
                            log.info("Handling packet of class " + packet.getClass().getSimpleName());
                            if (packet instanceof GamePacket) {
                                log.info(((GamePacket) packet).getPlayerID() + " <--- uid");
                            }
                            packetManager.handlePacket(packet);
                        }
                        log.info("Testinnnnnggg!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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

    private class ServerLogoutPacketListener implements PacketListener {

        @Override
        public void handlePacket(Packet gamePacket) {

        }

        @Override
        public Class<? extends Packet> packetClass() {
            return LogoutPacket.class;
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

                playerID = player.getPlayerID();
                authListener.authenticationPassed(packetManager, player.getPlayerID());


            } catch (Exception e) {
                authListener.authenticationFailed(packetManager, e.getMessage());
            }
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return AuthPacket.class;
        }
    }

    private class LogoutPacketListner implements PacketListener {

        @Override
        public void handlePacket(Packet gamePacket) {
            userSockets.remove(socket);
            log.info("Sending logout info to all players");
            for (ReliableSocket socket : userSockets) {
                packetManager.sendPacket(socket, new LogoutPacket(new Date().getTime(), playerID));
            }
            try {
                socket.close();
            } catch (IOException e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return LogoutPacket.class;
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
            log.info("Resending to all " + gamePacket);
			synchronized(userSockets) {
                log.info("Connected players: " + userSockets.size());
				for (ReliableSocket socket : userSockets) {
					packetManager.sendPacket(socket, gamePacket);
				}
			}
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return GamePacket.class;
        }
    }
}
