package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *
 *    0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|0|0|1|0|0|0|       6       |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |  Ack Number   |
 *  +---------------+---------------+
 *  |            Checksum           |
 *  +---------------+---------------+
 */
public class NULMessage extends Message {

    public NULMessage() {
        super();
    }

    public NULMessage(int seqn) {
        super(NUL_FLAG | ACK_FLAG, seqn, HEADER_LENGTH);
    }

}
