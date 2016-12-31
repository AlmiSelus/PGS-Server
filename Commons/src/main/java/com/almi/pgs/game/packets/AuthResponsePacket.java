package com.almi.pgs.game.packets;

import java.math.BigInteger;

/**
 * Created by Almi on 2016-12-31.
 */
public class AuthResponsePacket implements Packet {

	private byte playerID;
	private byte teamID;
	private int hash;

	private short code;
	private String reason;

	public AuthResponsePacket(short code) {
		this.code = code;
	}

	public byte getPlayerID() {
		return playerID;
	}

	public void setPlayerID(byte pid) {
		this.playerID = pid;
	}

	public byte getTeamID() {
		return teamID;
	}

	public void setTeamID(byte tid) {
		this.teamID = tid;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public short getCode() {
		return code;
	}

	public void setCode(short code) {
		this.code = code;
	}

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	@Override
	public String toString() {
		return "playerID: " + playerID + " teamID: " + teamID + " hash: " + hash;
	}
}
