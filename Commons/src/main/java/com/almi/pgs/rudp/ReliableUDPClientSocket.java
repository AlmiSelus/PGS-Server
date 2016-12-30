package com.almi.pgs.rudp;

import com.almi.pgs.rudp.messages.Message;
import com.almi.pgs.rudp.sockets.ReliableUDPSettings;
import com.almi.pgs.rudp.sockets.ReliableUDPSocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * Created by Almi on 2016-12-22.
 */
public class ReliableUDPClientSocket extends ReliableUDPSocket {
    public ReliableUDPClientSocket(DatagramSocket sock, SocketAddress endpoint) throws IOException {
        super(sock);
        this.endpoint = endpoint;
    }

    protected void init(DatagramSocket sock, ReliableUDPSettings profile)
    {
        _queue = new ArrayList<>();
        super.init(sock, profile);
    }

    protected Message receiveMessageImpl()
    {
        synchronized (_queue) {
            while (_queue.isEmpty()) {
                try {
                    _queue.wait();
                }
                catch (InterruptedException xcp) {
                    xcp.printStackTrace();
                }
            }

            return _queue.remove(0);
        }
    }

    protected void messageReceived(Message s)
    {
        synchronized (_queue) {
            _queue.add(s);
            _queue.notify();
        }
    }

    protected void closeSocket()
    {
        synchronized (_queue) {
            _queue.clear();
            _queue.add(null);
            _queue.notify();
        }
    }

    protected void log(String msg)
    {
        System.out.println(getPort() + ": " + msg);
    }

    private ArrayList<Message> _queue;
}
