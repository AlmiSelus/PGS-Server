package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.GameState;
import com.almi.pgs.game.packets.PlayerTeleportPacket;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Almi on 2017-01-04.
 */
public class Game {
    private final static Logger log = LoggerFactory.getLogger(Game.class);
    private final List<PlayerThread> clients;
    private final PacketManager packetManager;
    private byte remainingTime = Constants.ROUND_TIME;
    private byte pointsBlue = 0;
    private byte pointsRed = 0;
    private byte isRunning = 1;
    private byte winnerTeam = -1;

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
			packetManager.sendPacket(client.getSocket(), pt);
		}
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
    }
}