package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *    0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | |A| | | | | | |               |
 *  |0|C|0|0|0|0|1|0|        6      |
 *  | |K| | | | | | |               |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |         Header Checksum       |
 *  +---------------+---------------+
 *
 */
public class TCSMessage extends Message{

    public TCSMessage() {
        super();
    }

    public TCSMessage(int seqn) {
        super(TCS_FLAG, seqn, HEADER_LENGTH);
    }

}
