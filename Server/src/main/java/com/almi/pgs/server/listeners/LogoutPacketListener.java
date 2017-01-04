package com.almi.pgs.server.listeners;

import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.LogoutPacket;
import com.almi.pgs.game.packets.Packet;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.server.PlayerThread;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/**
 * Created by Almi on 2017-01-04.
 */
public class LogoutPacketListener implements PacketListener {
    private final static Logger log = LoggerFactory.getLogger(LogoutPacketListener.class);
    private final PlayerThread playerThread;
    private final PacketManager packetManager;

    public LogoutPacketListener(PacketManager packetManager, PlayerThread playerThread) {
        this.playerThread = playerThread;
        this.packetManager = packetManager;
    }

    @Override
    public void handlePacket(Packet gamePacket) {
        playerThread.getUserSockets().remove(playerThread.getSocket());
        log.info("Sending logout info to all players");
        for (ReliableSocket socket : playerThread.getUserSockets()) {
            packetManager.sendPacket(socket, new LogoutPacket(new Date().getTime(), playerThread.getPlayerID()));
        }
        try {
            playerThread.getSocket().close();
        } catch (IOException e) {
            log.info(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public Class<? extends Packet> packetClass() {
        return LogoutPacket.class;
    }
}