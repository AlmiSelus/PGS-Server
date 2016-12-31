package com.almi.pgs.game.packets;

/**
 * Created by Almi on 2016-12-30.
 */
public class GenericResponse implements Packet {
    private String response;
    private int code;

    public GenericResponse(String response, int code) {
        this.response = response;
        this.code = code;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
