/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.almi.pgs.game.packets;

/**
 *
 * @author Konrad
 */
public class ShootPacket implements Packet {

	private byte shooterId;
	private byte victimId;

	public ShootPacket(byte shooterId, byte victimId) {
		this.shooterId = shooterId;
		this.victimId = victimId;
	}

	public byte getShooterId() {
		return shooterId;
	}

	public byte getVictimId() {
		return victimId;
	}
}
