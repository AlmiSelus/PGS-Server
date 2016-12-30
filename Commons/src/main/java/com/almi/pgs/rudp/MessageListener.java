package com.almi.pgs.rudp;

import com.almi.pgs.rudp.messages.Message;

import java.net.InetAddress;

/**
 * Created by Almi on 2016-12-27.
 */
public interface MessageListener {
    void handleMessage(Message message);
    int port();
    InetAddress address();
}
