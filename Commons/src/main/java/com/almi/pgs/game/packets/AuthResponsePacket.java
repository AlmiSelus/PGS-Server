package com.almi.pgs.game.packets;

/**
 * Created by Almi on 2016-12-31.
 */
public class AuthResponsePacket implements Packet {
    private byte pid;
    private byte tid;
    private short code;
    private String reason;

    public AuthResponsePacket(short code) {
        this.code = code;
    }


    public byte getPid() {
        return pid;
    }

    public void setPid(byte pid) {
        this.pid = pid;
    }

    public byte getTid() {
        return tid;
    }

    public void setTid(byte tid) {
        this.tid = tid;
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
}
