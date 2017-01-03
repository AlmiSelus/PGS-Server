package com.almi.pgs.server.authentication;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.packets.GamePacket;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Almi on 2016-12-30.
 */
public class Player {

	private final static Logger log = LoggerFactory.getLogger(com.almi.pgs.server.authentication.Player.class);
	private String login;
	private String password;

	private byte playerId;
	private byte team;
	private int hash;
	private long packetTime;
	private Vector3f position;
	private Quaternion rotation;

	public Player(String login, String password) {
		this.login = login;
		this.password = password;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setNewGamePacket(GamePacket gamePacket) throws Exception {
		Vector3f newPosition = new Vector3f(gamePacket.getX(), gamePacket.getY(), gamePacket.getZ());

		this.checkSpeed(newPosition, gamePacket);

		this.position = newPosition;
		this.rotation = new Quaternion(
				gamePacket.getxAngle(),
				gamePacket.getyAngle(),
				gamePacket.getzAngle(),
				gamePacket.getW()
		);
		this.packetTime = gamePacket.getCurrentTime();
	}

	private void checkSpeed(Vector3f newPosition, GamePacket gamePacket) throws Exception {
		try {
			float distance = newPosition.distance(this.position);
			float deltaTime = (gamePacket.getCurrentTime() - this.packetTime) * 1000;
			float speed = distance / deltaTime;
			log.info("speed: " + speed);
			if (Float.isInfinite(speed)) {
				return;
			}
			if (speed > Constants.MAX_PLAYER_SPEED) {
				log.info("this: " + this.packetTime);
				throw new Exception("Player speed too high. Cheater? Speed:" + speed);
			}
		} catch (java.lang.NullPointerException e) {
		}
	}
}
