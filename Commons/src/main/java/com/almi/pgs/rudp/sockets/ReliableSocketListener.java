package com.almi.pgs.rudp.sockets;

import com.almi.pgs.rudp.messages.DATMessage;

/**
 * Created by Almi on 2016-12-28.
 */
public interface ReliableSocketListener {
    void packetSent();
    void packetRetransmitted();
    void packetReceivedInOrder(DATMessage msg);
    void packetReceivedOutOfOrder(DATMessage msg);
}
