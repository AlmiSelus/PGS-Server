package com.almi.pgs.rudp;

/**
 * Created by Almi on 2016-12-24.
 */
public interface RUDPStateListener {
    void stateConnected(RUDPSocket socket);
    void stateRefused(RUDPSocket socket);
    void stateClosed(RUDPSocket socket);
    void stateFailure(RUDPSocket socket);
    void stateReset(RUDPSocket socket);
}
