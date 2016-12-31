package com.almi.pgs.game;

import com.almi.pgs.game.packets.Packet;

/**
 * Created by Almi on 2016-12-31.
 */
public interface PacketListener {
    void handlePacket(Packet gamePacket);
    Class<? extends Packet> packetClass();
}
