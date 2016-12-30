package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *  RUDP Header
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |S|A|E|R|N|C|T| |    Header     |
 *  |Y|C|A|S|U|H|C|0|    Length     |
 *  |N|K|K|T|L|K|S| |               |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |  Sequence #   +   Ack Number  |
 *  +---------------+---------------+
 *  |            Checksum           |
 *  +---------------+---------------+
 *
 */
public abstract class Message {

    /**
     * At least 6 octets for each message type
     */
    public final static byte HEADER_LENGTH = 6;

    /**
     * Mask flags for testing/setting bits in first octet
     */
    public final static byte SYN_FLAG   = (byte) 0x80;
    public final static byte DAT_FLAG   = (byte) 0x41;
    public final static byte ACK_FLAG   = (byte) 0x40;
    public final static byte EACK_FLAG  = (byte) 0x20;
    public final static byte RST_FLAG   = (byte) 0x10;
    public final static byte NUL_FLAG   = (byte) 0x08;
    public final static byte CHK_FLAG   = (byte) 0x04;
    public final static byte TCS_FLAG   = (byte) 0x02;

    /**
     * Message settings
     */
    private int flags;
    private int headerLength;
    private int ackNumber;
    private int sequenceNumber;

    /**
     * Additional
     */
    private int retransmissionCounter;

    public Message() {
        retransmissionCounter = 0;
        ackNumber = -1; //not known
    }

    public Message(int flags, int seqn, int len) {
        this();
        this.flags = flags;
        this.sequenceNumber = seqn;
        this.headerLength = len;
    }

    public int getFlags() {
        return flags;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public void setAck(int ack) {
        flags = flags | ACK_FLAG;
        ackNumber = ack;
    }

    public int getAckNumber() {
        return (flags & ACK_FLAG) == ACK_FLAG ? ackNumber :  -1;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setAckNumber(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    public int getRetransmissionCounter() {
        return retransmissionCounter;
    }

    public void setRetransmissionCounter(int retransmissionCounter) {
        this.retransmissionCounter = retransmissionCounter;
    }

    public String typeToString() {
        String className = getClass().getSimpleName();
        return className.substring(0, className.indexOf("Message"));
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[headerLength];
        bytes[0] = (byte) (flags & 0xFF);
        bytes[1] = (byte) (headerLength & 0xFF);
        bytes[2] = (byte) (sequenceNumber & 0xFF);
        bytes[3] = (byte) (ackNumber & 0xFF);
        return bytes;
    }

    public static Message parseMessage(byte[] arr) {
        if(arr.length < HEADER_LENGTH) {
            throw new IllegalStateException("Length lower than default length.");
        }

        Message msg = null;
        int flags = arr[0];
        if ((flags & SYN_FLAG) != 0) {
            msg = new SYNMessage();
        } else if ((flags & NUL_FLAG) != 0) {
            msg = new NULMessage();
        } else if ((flags & EACK_FLAG) != 0) {
            msg = new EACKMessage();
        } else if ((flags & RST_FLAG) != 0) {
            msg = new RSTMessage();
        } else if ((flags & TCS_FLAG) != 0) {
            msg = new TCSMessage();
        } else if ((flags & ACK_FLAG) != 0) {
            msg = new ACKMessage();
        } else if ((flags & DAT_FLAG) != 0) {
            msg = new DATMessage();
        }

        if(msg == null) {
            throw new IllegalStateException("Message is of invalid type");
        }

        msg.parseBytes(arr);

        return msg;
    }

    public void parseBytes(byte[] arr) {
        flags = arr[0];
        headerLength = arr[1] & 0xFF;
        sequenceNumber = arr[2] & 0xFF;
        ackNumber = arr[3] & 0xFF;

    }

    public int length() {
        return headerLength;
    }
}
