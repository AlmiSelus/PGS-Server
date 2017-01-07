package com.almi.pgs.game.packets;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Almi on 2017-01-03.
 */
public class GameState implements Packet {
    private int remainingTime;
    private byte pointsRed;
    private byte pointsBlue;
    private byte isRunning;
    private byte winner;
    private Map<Byte, Vector3f> positionMap = new HashMap<>();
    private Map<Byte, Quaternion> rotationMap = new HashMap<>();

    public GameState() {
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
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

    public void setIsRunning(byte isRunning) {
        this.isRunning = isRunning;
    }

    public byte getIsRunning() {
        return isRunning;
    }

    public void setWinner(byte winner) {
        this.winner = winner;
    }

    public byte getWinner() {
        return winner;
    }


    public Map<Byte, Vector3f> getPositionMap() {
        return positionMap;
    }

    public void setPositionMap(Map<Byte, Vector3f> positionMap) {
        this.positionMap = positionMap;
    }

    public Map<Byte, Quaternion> getRotationMap() {
        return rotationMap;
    }

    public void setRotationMap(Map<Byte, Quaternion> rotationMap) {
        this.rotationMap = rotationMap;
    }
}
