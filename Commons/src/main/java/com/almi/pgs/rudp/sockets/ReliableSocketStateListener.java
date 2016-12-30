package com.almi.pgs.rudp.sockets;

/**
 * Created by Almi on 2016-12-28.
 */
public interface ReliableSocketStateListener {
    void connectionOpened(ReliableUDPSocket sock);
    void connectionRefused(ReliableUDPSocket sock);
    void connectionClosed(ReliableUDPSocket sock);
    void connectionFailure(ReliableUDPSocket sock);
    void connectionReset(ReliableUDPSocket sock);
}
