package com.almi.pgs.client;

import com.almi.pgs.game.packets.GamePacket;
import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;

/**
 * @author Konrad
 */
public class Player {

	private byte playerId;
	private byte team;
	private int hash;
	private long packetTime;

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

	public Geometry getGeometry() {
		return geometry;
	}

	void setNewGamePacket(GamePacket gamePacket) {
		this.geometry.setLocalTranslation(gamePacket.getX(), gamePacket.getY(), gamePacket.getZ());
		this.geometry.setLocalRotation(
				new Quaternion(gamePacket.getxAngle(),
				gamePacket.getyAngle(),
				gamePacket.getzAngle(),
				gamePacket.getW()));
	}

	public long getPacketTime() {
		return packetTime;
	}

	public void setPacketTime(long packetTime) {
		this.packetTime = packetTime;
	}
}
