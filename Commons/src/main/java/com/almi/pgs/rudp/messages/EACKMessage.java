package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *  0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|1|0|0|0|0|0|     N + 6     |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |1st out of seq |2nd out of seq |
 *  |  ack number   |   ack number  |
 *  +---------------+---------------+
 *  |  . . .        |Nth out of seq |
 *  |               |   ack number  |
 *  +---------------+---------------+
 *  |            Checksum           |
 *  +---------------+---------------+
 *
 */
public class EACKMessage extends Message {

    private int[] acks;

    public EACKMessage() {
        super();
    }

    public EACKMessage(int seqn, int ack, int[] acks) {
        super(ACK_FLAG | EACK_FLAG, seqn, acks.length + HEADER_LENGTH);
        setAck(ack);
        this.acks = acks;
    }

    public int[] getAcks() {
        return acks;
    }

    public byte[] getBytes() {
        byte[] buffer = super.getBytes();

        for (int i = 0; i < acks.length; i++) {
            buffer[4+i] = (byte) (acks[i] & 0xFF);
        }

        return buffer;
    }

}
