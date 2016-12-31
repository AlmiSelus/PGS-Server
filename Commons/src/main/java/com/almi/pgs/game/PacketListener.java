package com.almi.pgs.game;

/**
 * Created by Almi on 2016-12-31.
 */
public interface PacketListener {
    void handlePacket(Packet gamePacket);
    Class<? extends Packet> packetClass();
}
