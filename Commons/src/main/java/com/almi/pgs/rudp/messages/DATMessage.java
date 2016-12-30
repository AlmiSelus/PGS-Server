package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *  0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|0|0|0|0|0|1|       6       |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |           Checksum            |
 *  +---------------+---------------+
 *  | ...                           |
 *  +-------------------------------+
 *
 */
public class DATMessage extends Message {

    private byte[] data;

    public DATMessage() {
        super();
    }

    public DATMessage(int seqn, int ack, byte[] data) {
        super(DAT_FLAG, seqn, HEADER_LENGTH);
        setAck(ack);
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public byte[] getData() {
        return data;
    }

}
