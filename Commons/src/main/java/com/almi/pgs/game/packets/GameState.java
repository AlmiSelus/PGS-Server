package com.almi.pgs.game.packets;

/**
 * Created by Almi on 2017-01-03.
 */
public class GameState implements Packet {
    private byte remainingTime;
    private byte pointsRed;
    private byte pointsBlue;

    public GameState() {
    }

    public byte getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(byte remainingTime) {
        this.remainingTime = remainingTime;
    }

    public byte getPointsRed() {
        return pointsRed;
    }

    public void setPointsRed(byte pointsRed) {
        this.pointsRed = pointsRed;
    }

    public byte getPointsBlue() {
        return pointsBlue;
    }

    public void setPointsBlue(byte pointsBlue) {
        this.pointsBlue = pointsBlue;
    }
}
