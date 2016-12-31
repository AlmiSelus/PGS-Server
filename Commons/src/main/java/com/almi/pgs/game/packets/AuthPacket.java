package com.almi.pgs.game.packets;

/**
 * Created by Almi on 2016-12-30.
 */
public class AuthPacket implements Packet {
    private String login;
    private String password;

    public AuthPacket(String login, String password) {
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
}
