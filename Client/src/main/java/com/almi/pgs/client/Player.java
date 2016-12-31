package com.almi.pgs.client;

import com.almi.pgs.game.packets.GamePacket;
import com.jme3.scene.Geometry;
import java.math.BigInteger;

/**
 *
 * @author Konrad
 */
public class Player {

	private byte playerId;
	private byte team;
	private int hash;

	private Geometry geometry;

	public Player(byte playerId, byte team, int hash) {
		this.playerId = playerId;
		this.team = team;
		this.hash = hash;
	}

	Player(GamePacket gamePacket) {
		this.playerId = gamePacket.getPlayerID();
		this.team = gamePacket.getTeam();
	}

	public byte getPlayerId() {
		return playerId;
	}

	public byte getTeam() {
		return team;
	}

	public int getHash() {
		return hash;
	}

	void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	void SetNewGamePacket(GamePacket gamePacket) {
		this.geometry.setLocalTranslation(gamePacket.getX(), gamePacket.getY(), gamePacket.getZ());
	}

}
