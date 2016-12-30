package com.almi.pgs.server;

import com.almi.pgs.rudp.ReliableUDPServerSocket;
import com.almi.pgs.rudp.messages.DATMessage;
import com.almi.pgs.rudp.sockets.ReliableSocketListener;
import com.almi.pgs.rudp.sockets.ReliableUDPSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by Almi on 2016-12-09.
 */
public class PlayerOnServerThread implements ReliableSocketListener {
    private final static Logger log = LoggerFactory.getLogger(PlayerOnServerThread.class);
    private InetAddress address;
    private int port;
    private ReliableUDPServerSocket socket;

    /**
     * Counters
     */

    public PlayerOnServerThread(InetAddress address, int port, ReliableUDPServerSocket serverSocket) throws SocketException {
        this.address = address;
        this.port = port;
        socket = serverSocket;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }


    @Override
    public void packetSent() {

    }

    @Override
    public void packetRetransmitted() {

    }

    @Override
    public void packetReceivedInOrder(DATMessage msg) {

    }

    @Override
    public void packetReceivedOutOfOrder(DATMessage msg) {

    }

}
