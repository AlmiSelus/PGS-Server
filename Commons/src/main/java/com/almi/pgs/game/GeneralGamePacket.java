package com.almi.pgs.game;

/**
 * Created by Almi on 2016-12-31.
 *
 * Only one at a time can be set
 */
public class GeneralGamePacket implements Packet {
    private AuthPacket authPacket;
    private GamePacket gamePacket;
    private GenericResponse genericPacket;

    public AuthPacket getAuthPacket() {
        return authPacket;
    }

    public void setAuthPacket(AuthPacket authPacket) {
        this.authPacket = authPacket;
    }

    public GamePacket getGamePacket() {
        return gamePacket;
    }

    public void setGamePacket(GamePacket gamePacket) {
        this.gamePacket = gamePacket;
    }

    public GenericResponse getGenericPacket() {
        return genericPacket;
    }

    public void setGenericPacket(GenericResponse genericPacket) {
        this.genericPacket = genericPacket;
    }
}
