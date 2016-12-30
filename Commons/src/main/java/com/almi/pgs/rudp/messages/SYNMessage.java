package com.almi.pgs.rudp.messages;

/**
 * Created by Almi on 2016-12-22.
 *
 *  0             7 8              15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | |A| | | | | | |               |
 *  |1|C|0|0|0|0|0|0|       22      |
 *  | |K| | | | | | |               |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  +  Sequence #   +   Ack Number  |
 *  +---------------+---------------+
 *  | Vers  | Spare | Max # of Out  |
 *  |       |       | standing Segs |
 *  +---------------+---------------+
 *  | Option Flags  |     Spare     |
 *  +---------------+---------------+
 *  |     Maximum Segment Size      |
 *  +---------------+---------------+
 *  | Retransmission Timeout Value  |
 *  +---------------+---------------+
 *  | Cumulative Ack Timeout Value  |
 *  +---------------+---------------+
 *  |   Null Segment Timeout Value  |
 *  +---------------+---------------+
 *  |  Max Retrans  | Max Cum Ack   |
 *  +---------------+---------------+
 *  | Max Out of Seq| Max Auto Reset|
 *  +---------------+---------------+
 *  |           Checksum            |
 *  +---------------+---------------+
 *
 */
public class SYNMessage extends Message {

    public int getVersion() {
        return version;
    }

    public int getMaxNumberOfOutstandingSegments() {
        return maxNumberOfOutstandingSegments;
    }

    public int getOptionFlags() {
        return optionFlags;
    }

    public int getMaxSegmentSize() {
        return maxSegmentSize;
    }

    public int getRetransmissionTimeout() {
        return retransmissionTimeout;
    }

    public int getCumulativeACKTimeout() {
        return cumulativeACKTimeout;
    }

    public int getNullSegmentTimeout() {
        return nullSegmentTimeout;
    }

    public int getMaxRetransmissions() {
        return maxRetransmissions;
    }

    public int getMaxCumulativeAck() {
        return maxCumulativeAck;
    }

    public int getMaxOutOfSequence() {
        return maxOutOfSequence;
    }

    public int getMaxAutoReset() {
        return maxAutoReset;
    }

    private int version;
    private int maxNumberOfOutstandingSegments;
    private int optionFlags;
    private int maxSegmentSize;
    private int retransmissionTimeout;
    private int cumulativeACKTimeout;
    private int nullSegmentTimeout;
    private int maxRetransmissions;
    private int maxCumulativeAck;
    private int maxOutOfSequence;
    private int maxAutoReset;

    public SYNMessage() {
    }

    public SYNMessage(int seqn, int maxseg, int maxsegsize, int rettoval,
                      int cumacktoval, int niltoval, int maxret,
                      int maxcumack, int maxoutseq, int maxautorst) {
        super(SYN_FLAG, seqn, HEADER_LENGTH + 17);

        version = 1;
        maxNumberOfOutstandingSegments = maxseg;
        maxSegmentSize = maxsegsize;
        retransmissionTimeout = rettoval;
        maxCumulativeAck = maxcumack;
        nullSegmentTimeout = niltoval;
        maxRetransmissions = maxret;
        maxOutOfSequence = maxoutseq;
        maxAutoReset = maxautorst;
        cumulativeACKTimeout = cumacktoval;
        optionFlags = 0;
    }

    @Override
    public byte[] getBytes() {
        byte[] byteArr = super.getBytes();

        byteArr[4] = (byte) ((version << 4)& 0xFF);
        byteArr[5] = 0;
        byteArr[6] = (byte) (maxNumberOfOutstandingSegments & 0xFF);
        byteArr[7] = (byte) (optionFlags & 0xFF);
        byteArr[8] = 0;
        byteArr[9] = (byte) ((maxSegmentSize >>> 8 ) & 0xFF);
        byteArr[10] = (byte) (maxSegmentSize & 0xFF);
        byteArr[11] = (byte) ((retransmissionTimeout >>> 8) & 0xFF);
        byteArr[12] = (byte) (retransmissionTimeout & 0xFF);
        byteArr[13] = (byte) ((cumulativeACKTimeout >>> 8) & 0xFF);
        byteArr[14] = (byte) (cumulativeACKTimeout & 0xFF);
        byteArr[15] = (byte) ((nullSegmentTimeout >>> 8) & 0xFF);
        byteArr[16] = (byte) (nullSegmentTimeout & 0xFF);
        byteArr[17] = (byte) (maxRetransmissions & 0xFF);
        byteArr[18] = (byte) (maxCumulativeAck & 0xFF);
        byteArr[19] = (byte) (maxOutOfSequence & 0xFF);
        byteArr[20] = (byte) (maxAutoReset & 0xFF);

        return byteArr;
    }

    @Override
    public void parseBytes(byte[] arr) {
        super.parseBytes(arr);

        if(arr.length < HEADER_LENGTH + 17) {
            throw new IllegalStateException("Invalid SYN Message");
        }

        version = (arr[4] & 0xFF) >>> 4;
        if(version != 1) {
            throw new IllegalStateException("Invalid version");
        }

        maxNumberOfOutstandingSegments = (arr[6] & 0xFF);
        optionFlags = (arr[7] & 0xFF);
        maxSegmentSize = ((arr[9] << 8) & 0xFF) | (arr[10] & 0xFF);
        retransmissionTimeout = ((arr[11] << 8) & 0xFF) | (arr[12] & 0xFF);
        cumulativeACKTimeout = ((arr[13] << 8) & 0xFF) | (arr[14] & 0xFF);
        nullSegmentTimeout = ((arr[15] << 8) & 0xFF) | (arr[16] & 0xFF);
        maxRetransmissions = arr[17] & 0xFF;
        maxCumulativeAck = arr[18] & 0xFF;
        maxOutOfSequence = arr[19] & 0xFF;
        maxAutoReset = arr[20] & 0xFF;
    }
}
