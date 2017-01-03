package com.almi.pgs.game.packets;

/**
 * Created by Almi on 2017-01-03.
 */
public class LogoutPacket implements Packet {
    private long timestamp;
    private byte playerID;

    public LogoutPacket(long timestamp, byte playerID) {
        this.timestamp = timestamp;
        this.playerID = playerID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte getPlayerID() {
        return playerID;
    }

    public void setPlayerID(byte playerID) {
        this.playerID = playerID;
    }
}
