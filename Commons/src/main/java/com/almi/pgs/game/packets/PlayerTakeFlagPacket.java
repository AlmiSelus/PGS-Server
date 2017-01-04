package com.almi.pgs.game.packets;

/**
 *
 * @author Konrad
 */
public class PlayerTakeFlagPacket implements Packet {

	private byte playerId;

	public byte getPlayerId() {
		return playerId;
	}

	public void setPlayerId(byte playerId) {
		this.playerId = playerId;
	}

	@Override
	public String toString() {
		return "[ playerId = " + playerId + " ]";
	}

}
