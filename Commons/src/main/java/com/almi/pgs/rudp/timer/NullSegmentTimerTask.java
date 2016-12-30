package com.almi.pgs.rudp.timer;

import com.almi.pgs.rudp.messages.NULMessage;
import com.almi.pgs.rudp.sockets.ReliableUDPSocket;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by Almi on 2016-12-28.
 */
public class NullSegmentTimerTask implements Runnable {

    private ReliableUDPSocket socket;
    private final static Logger log = LoggerFactory.getLogger(NullSegmentTimerTask.class);

    public NullSegmentTimerTask(ReliableUDPSocket socket) {
        this.socket = socket;
    }

    public void run() {

        synchronized (socket.unackedSentQueue) {
            if (socket.unackedSentQueue.isEmpty()) {
                try {
                    socket.sendAndQueueMessage(new NULMessage(socket.counters.nextSequenceNumber()));
                } catch (IOException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }
        }

    }
}
