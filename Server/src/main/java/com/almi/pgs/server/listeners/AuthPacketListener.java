package com.almi.pgs.server.listeners;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.AuthPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
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
    private final ReliableSocket socket;
    private final List<ReliableSocket> userSockets;
    private AuthenticationListener authListener;
    private PlayerThread playerThread;
    private PacketManager packetManager;
    private byte teamID;

    private AuthenticationLocalDatabase localDatabase = new AuthenticationLocalDatabase();

    public AuthPacketListener(AuthenticationListener authenticationListener,
                              PlayerThread playerThread, PacketManager packetManager, byte teamID, ReliableSocket socket,
                              List<ReliableSocket> userSockets) {
        this.authListener = authenticationListener;
        this.playerThread = playerThread;
        this.packetManager = packetManager;
        this.teamID = teamID;
        this.socket = socket;
        this.userSockets = userSockets;
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
            packetManager.addPacketListener(new GamePacketListener(socket, packetManager, userSockets, player));
            authListener.authenticationPassed(packetManager, player.getPlayerID(), teamID);


        } catch (Exception e) {
            authListener.authenticationFailed(packetManager, e.getMessage());
        }
    }

    @Override
    public Class<? extends Packet> packetClass() {
        return AuthPacket.class;
    }
}