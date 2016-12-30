package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *
 *  ACK Segment
 *
 *  0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|0|0|0|0|0|0|       6       |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |           Checksum            |
 *  +---------------+---------------+
 *
 */
public class ACKMessage extends Message {

    public ACKMessage() {
        super();
    }

    public ACKMessage(int seqn, int ackn) {
        super(ACK_FLAG, seqn, HEADER_LENGTH);
        setAck(ackn);
    }

}
