package com.almi.pgs.server.listeners;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.GamePacket;
import com.almi.pgs.game.packets.LogoutPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.Game;
import com.almi.pgs.server.PlayerThread;
import com.almi.pgs.server.authentication.Player;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Created by Almi on 2017-01-04.
 */
public class GamePacketListener implements PacketListener {
    private static final Logger log = LoggerFactory.getLogger(GamePacketListener.class);

    private ReliableSocket clientSocket;
    private PacketManager packetManager;
    private List<PlayerThread> userSockets;
    private Player player;
    private Game gameState;

    public GamePacketListener(ReliableSocket socket, PacketManager packetManager, List<PlayerThread> sockets, Player player, Game gameState) {
        this.clientSocket = socket;
        this.packetManager = packetManager;
        this.userSockets = sockets;
        this.player = player;
        this.gameState = gameState;
    }

    @Override
    public void handlePacket(Packet gamePacket) {
        try {
            player.setNewGamePacket((GamePacket) gamePacket);
//            log.info("Resending to all " + gamePacket);
//            for (PlayerThread client : userSockets) {
//                if(client.getSocket() != clientSocket) {
//                    packetManager.sendPacket(client.getSocket(), gamePacket);
//                }
//            }

            gameState.getPositionMap().put(player.getPlayerID(), player.getPosition());
            gameState.getRotationMap().put(player.getPlayerID(), player.getRotation());

        } catch (Exception ex) {
            try {
                log.info(ExceptionUtils.getStackTrace(ex));
                packetManager.sendPacket(clientSocket, new LogoutPacket(new Date().getTime(), player.getPlayerID()));
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }
    }
    @Override
    public Class<? extends Packet> packetClass() {
        return GamePacket.class;
    }
}
