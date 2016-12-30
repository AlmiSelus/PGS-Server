package com.almi.pgs.game;

import java.io.Serializable;

/**
 * Created by Almi on 2016-12-29.
 *
 * Simple game packet representing user movement
 */
public class GamePacket implements Serializable {

    private float x;
    private float y;
    private float z;
    private byte team;
    private byte playerID;

    public GamePacket(float x, float y, float z, byte team, byte playerID) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.team = team;
        this.playerID = playerID;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public byte getTeam() {
        return team;
    }

    public void setTeam(byte team) {
        this.team = team;
    }

    public byte getPlayerID() {
        return playerID;
    }

    public void setPlayerID(byte playerID) {
        this.playerID = playerID;
    }
}
