package com.almi.pgs.rudp.timer;

import com.almi.pgs.rudp.sockets.ReliableUDPSocket;

/**
 * Created by Almi on 2016-12-28.
 */
public class CumulativeACKTimeout implements Runnable {

    private ReliableUDPSocket socket;

    public CumulativeACKTimeout(ReliableUDPSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        socket.sendAck();
    }
}
