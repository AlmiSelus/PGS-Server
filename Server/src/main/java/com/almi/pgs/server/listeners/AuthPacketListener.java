package com.almi.pgs.server.listeners;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.AuthPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.server.PlayerThread;
import com.almi.pgs.server.authentication.AuthenticationListener;
import com.almi.pgs.server.authentication.AuthenticationLocalDatabase;
import com.almi.pgs.server.authentication.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Created by Almi on 2017-01-04.
 */
public class AuthPacketListener implements PacketListener {
    private final static Logger log = LoggerFactory.getLogger(AuthPacketListener.class);
    private final List<PlayerThread> clients;
    private AuthenticationListener authListener;
    private PlayerThread playerThread;
    private PacketManager packetManager;

    private AuthenticationLocalDatabase localDatabase = new AuthenticationLocalDatabase();

    public AuthPacketListener(AuthenticationListener authenticationListener,
                              PlayerThread playerThread, PacketManager packetManager, List<PlayerThread> userSockets) {
        this.authListener = authenticationListener;
        this.playerThread = playerThread;
        this.packetManager = packetManager;
        this.clients = userSockets;
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

            playerThread.setPlayerID(player.getPlayerID());
            packetManager.addPacketListener(new GamePacketListener(playerThread.getSocket(), packetManager, clients, player));
            authListener.authenticationPassed(packetManager, player.getPlayerID(), playerThread.getTeamID());


        } catch (Exception e) {
            authListener.authenticationFailed(packetManager, e.getMessage());
        }
    }

    @Override
    public Class<? extends Packet> packetClass() {
        return AuthPacket.class;
    }
}