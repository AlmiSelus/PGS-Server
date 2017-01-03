package com.almi.pgs.server.authentication;

import com.almi.pgs.game.PacketManager;

/**
 * Created by Almi on 2016-12-30.
 */
public interface AuthenticationListener {
    void authenticationDataObtained(PacketManager packetManager);
    void authenticationPassed(PacketManager packetManager, int playerID, byte teamID);
    void authenticationFailed(PacketManager packetManager, String reason);
}
