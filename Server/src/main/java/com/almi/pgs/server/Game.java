package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.GameState;
import com.almi.pgs.game.packets.PlayerTeleportPacket;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
                toGameStatePacket(packet, this, new ArrayList<>());
                for(PlayerThread client : clients) {
                    packetManager.sendPacket(client.getSocket(), packet);
                }
                resetGameState();
                Thread.sleep(10 * 1000); // wait 10 seconds before starting new game
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        } else {
            for(PlayerThread client : clients) {
                try {
                    toGameStatePacket(packet, this, getSeenPlayers(client));
                    packetManager.sendPacket(client.getSocket(), packet);
                } catch (Exception e) {
                    log.info(ExceptionUtils.getStackTrace(e));
                }
            }
            remainingTime--;
        }
    }

    private List<Byte> getSeenPlayers(PlayerThread client) {
        List<Byte> seenPlayers = new ArrayList<>();
        byte currentID = client.getPlayerID();
        Map<Byte, Quaternion> userRotations = getRotationMap();
        if(userRotations.containsKey(currentID)) {
            Vector3f pos = getPositionMap().get(currentID);
            Quaternion rotation = getRotationMap().get(currentID);
            Vector3f look = rotation.mult(Vector3f.UNIT_Z);
            // Decide how fat the cone should be, this can be a constant.  Note:
            // the angle for cosine is relative to the side and not straight ahead
            // so I will show a 30 degree example so it's clearer.  90 degrees is
            // straight ahead.
            float threshold = FastMath.cos((90 - 30) * FastMath.DEG_TO_RAD);
            for (Map.Entry<Byte, Vector3f> userRot : getPositionMap().entrySet()) {
                if (currentID != userRot.getKey()) {
                    Vector3f enemyPos = userRot.getValue().normalize();
                    Vector3f relative = enemyPos.subtract(pos).normalizeLocal();
                    float dot = relative.dot(look);
//                    if(dot > threshold) {
                        seenPlayers.add(userRot.getKey());
//                    }
                }
            }
        }

        log.info("Sending to player " + currentID + " data = " +Arrays.toString(seenPlayers.stream().toArray()));

        return seenPlayers;
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

	public synchronized void addPoints(byte teamId) {
		switch (teamId) {
			case 1:
				pointsBlue++;
				break;
			case 2:
				pointsRed++;
				break;
		}
		PlayerTeleportPacket pt = new PlayerTeleportPacket();
		for (PlayerThread client : clients) {
            try {
                packetManager.sendPacket(client.getSocket(), pt);
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }
	}

    private void resetGameState() {
        remainingTime = Constants.ROUND_TIME;
        isRunning = 1;
        winnerTeam = -1;
        pointsBlue = 0;
        pointsRed = 0;
    }

    private synchronized void toGameStatePacket(GameState packet, Game gameState, List<Byte> maskPlayerIds) {
        packet.setRemainingTime(gameState.remainingTime);
        packet.setPointsBlue(gameState.pointsBlue);
        packet.setPointsRed(gameState.pointsRed);
        packet.setIsRunning(isRunning);
        packet.setWinner(winnerTeam);
        packet.setPositionMap(getPositionMap().entrySet().stream().filter(posEntry -> maskPlayerIds.contains(posEntry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        packet.setRotationMap(getRotationMap().entrySet().stream().filter(posEntry -> maskPlayerIds.contains(posEntry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
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