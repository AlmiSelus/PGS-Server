package com.almi.pgs.rudp.timer;

import com.almi.pgs.rudp.sockets.ReliableUDPSocket;

/**
 * Created by Almi on 2016-12-28.
 */
public class KeepAliveTimeout implements Runnable {

    private ReliableUDPSocket socket;

    public KeepAliveTimeout(ReliableUDPSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        socket.connectionFailure();
    }
}
