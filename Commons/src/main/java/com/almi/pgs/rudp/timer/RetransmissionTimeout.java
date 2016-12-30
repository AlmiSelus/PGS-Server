package com.almi.pgs.rudp.timer;

import com.almi.pgs.rudp.messages.Message;
import com.almi.pgs.rudp.sockets.ReliableUDPSocket;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by Almi on 2016-12-27.
 */
public class RetransmissionTimeout implements Runnable {

    private final Queue<Message> queue;
    private ReliableUDPSocket reliableSocket;
    private final static Logger log = LoggerFactory.getLogger(RetransmissionTimeout.class);

    public RetransmissionTimeout(ReliableUDPSocket serverSocket, Queue<Message> queue) {
        this.queue = queue;
        reliableSocket = serverSocket;
    }

    @Override
    public void run() {
        synchronized (queue) {
            for (Message aQueue : queue) {
                try {
                    reliableSocket.retransmitMessage(aQueue);
                } catch (IOException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

}
