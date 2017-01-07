package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.GameState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Almi on 2017-01-04.
 */
public class Game {
    private final static Logger log = LoggerFactory.getLogger(Game.class);
    private final List<PlayerThread> clients;
    private final PacketManager packetManager;
    private int remainingTime = Constants.ROUND_TIME;
    private byte pointsBlue = 0;
    private byte pointsRed = 0;
    private byte isRunning = 1;
    private byte winnerTeam = -1;
    private Map<Byte, Vector3f> positionMap = new HashMap<>();
    private Map<Byte, Quaternion> rotationMap = new HashMap<>();

    public Game(List<PlayerThread> clientSockets, PacketManager pm) {
        clients = clientSockets;
        packetManager = pm;
    }

    public synchronized void tick() {
        GameState packet = new GameState();

        if(remainingTime == 0) {
            try {
                winnerTeam = (byte) (pointsBlue > pointsRed ? 0 : pointsRed > pointsBlue ? 1 : 2);
                isRunning = 0;
                toGameStatePacket(packet, this);
                for(PlayerThread client : clients) {
                    packetManager.sendPacket(client.getSocket(), packet);
                }
                resetGameState();
                Thread.sleep(10 * 1000); // wait 10 seconds before starting new game
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        } else {
            toGameStatePacket(packet, this);
            for(PlayerThread client : clients) {
                packetManager.sendPacket(client.getSocket(), packet);
            }
            remainingTime--;
        }
    }

    public List<PlayerThread> getClients() {
        return clients;
    }

    public PacketManager getPacketManager() {
        return packetManager;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public byte getPointsBlue() {
        return pointsBlue;
    }

    public void setPointsBlue(byte pointsBlue) {
        this.pointsBlue = pointsBlue;
    }

    public byte getPointsRed() {
        return pointsRed;
    }

    public void setPointsRed(byte pointsRed) {
        this.pointsRed = pointsRed;
    }

    public byte getIsRunning() {
        return isRunning;
    }

    public void setIsRunning(byte isRunning) {
        this.isRunning = isRunning;
    }

    public byte getWinnerTeam() {
        return winnerTeam;
    }

    public void setWinnerTeam(byte winnerTeam) {
        this.winnerTeam = winnerTeam;
    }

    private void resetGameState() {
        remainingTime = Constants.ROUND_TIME;
        isRunning = 1;
        winnerTeam = -1;
        pointsBlue = 0;
        pointsRed = 0;
    }

    private synchronized void toGameStatePacket(GameState packet, Game gameState) {
        packet.setRemainingTime(gameState.remainingTime);
        packet.setPointsBlue(gameState.pointsBlue);
        packet.setPointsRed(gameState.pointsRed);
        packet.setIsRunning(isRunning);
        packet.setWinner(winnerTeam);
        packet.setPositionMap(getPositionMap());
        packet.setRotationMap(getRotationMap());
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