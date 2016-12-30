package com.almi.pgs.rudp.sockets;

/**
 * Created by Almi on 2016-12-28.
 */
public class ReliableUDPSettings {
    public final static int MAXSENDQUEUESIZE    = 32;
    public final static int MAXRECVQUEUESIZE    = 32;

    public final static int MAXSEGMENTSIZE       = 128;
    public final static int MAXOUTSTANDINGSEGS   = 3;
    public final static int MAXRETRANS            = 3;
    public final static int MAXCUMULATIVEACKS    = 3;
    public final static int MAXOUTOFSEQUENCE    = 3;
    public final static int MAXAUTORESET         = 3;
    public final static int NULLSEGMENTTIMEOUT   = 2000;
    public final static int RETRANSMISSIONTIMEOUT = 600;
    public final static int CUMULATIVEACKTIMEOUT = 300;

    private int maxSendQueueSize;
    private int maxRecvQueueSize;
    private int maxSegmentSize;
    private int maxOutstandingSegs;
    private int maxRetrans;
    private int maxCumulativeAcks;
    private int maxOutOfSequence;
    private int maxAutoReset;
    private int nullSegmentTimeout;
    private int retransmissionTimeout;
    private int cumulativeAckTimeout;

    /**
     * Creates a profile with the default RUDP parameter values.
     *
     * Note: According to the RUDP protocol's draft, the default
     * maximum number of retransmissions is 3. However, if packet
     * drops are too high, the connection may get stall unless
     * the sender continues to retransmit packets that have not been
     * unacknowledged. We will use 0 instead, which means unlimited.
     *
     */
    public ReliableUDPSettings() {
        this(MAXSENDQUEUESIZE, MAXRECVQUEUESIZE, MAXSEGMENTSIZE, MAXOUTSTANDINGSEGS, 0/*MAXRETRANS*/,
            MAXCUMULATIVEACKS, MAXOUTOFSEQUENCE, MAXAUTORESET, NULLSEGMENTTIMEOUT, RETRANSMISSIONTIMEOUT,
            CUMULATIVEACKTIMEOUT);
    }

    public ReliableUDPSettings(int maxSendQueueSize, int maxRecvQueueSize, int maxSegmentSize, int maxOutstandingSegs,
                                 int maxRetrans, int maxCumulativeAcks, int maxOutOfSequence, int maxAutoReset,
                                 int nullSegmentTimeout, int retransmissionTimeout, int cumulativeAckTimeout) {
        this.maxSendQueueSize      = maxSendQueueSize;
        this.maxRecvQueueSize      = maxRecvQueueSize;
        this.maxSegmentSize        = maxSegmentSize;
        this.maxOutstandingSegs    = maxOutstandingSegs;
        this.maxRetrans            = maxRetrans;
        this.maxCumulativeAcks     = maxCumulativeAcks;
        this.maxOutOfSequence      = maxOutOfSequence;
        this.maxAutoReset          = maxAutoReset;
        this.nullSegmentTimeout    = nullSegmentTimeout;
        this.retransmissionTimeout = retransmissionTimeout;
        this.cumulativeAckTimeout  = cumulativeAckTimeout;
    }

    /**
     * Returns the maximum send queue size (packets).
     */
    public int maxSendQueueSize() {
        return maxSendQueueSize;
    }

    /**
     * Returns the maximum receive queue size (packets).
     */
    public int maxRecvQueueSize() {
        return maxRecvQueueSize;
    }

    /**
     * Returns the maximum segment size (octets).
     */
    public int maxMessageSize() {
        return maxSegmentSize;
    }

    /**
     * Returns the maximum number of outstanding segments.
     */
    public int maxOutstandingSegs() {
        return maxOutstandingSegs;
    }

    /**
     * Returns the maximum number of consecutive retransmissions (0 means unlimited).
     */
    public int maxRetrans() {
        return maxRetrans;
    }

    /**
     * Returns the maximum number of unacknowledged received segments.
     */
    public int maxCumulativeAcks() {
        return maxCumulativeAcks;
    }

    /**
     * Returns the maximum number of out-of-sequence received segments.
     */
    public int maxOutOfSequence() {
        return maxOutOfSequence;
    }

    /**
     * Returns the maximum number of consecutive auto resets.
     */
    public int maxAutoReset() {
        return maxAutoReset;
    }

    /**
     * Returns the null segment timeout (ms).
     */
    public int nullMessageTimeout() {
        return nullSegmentTimeout;
    }

    /**
     * Returns the retransmission timeout (ms).
     */
    public int retransmissionTimeout() {
        return retransmissionTimeout;
    }

    /**
     * Returns the cumulative acknowledge timeout (ms).
     */
    public int cumulativeAckTimeout() {
        return cumulativeAckTimeout;
    }

    public String toString() {
        return "[" +
                maxSendQueueSize + ", " +
                maxRecvQueueSize + ", " +
                maxSegmentSize + ", " +
                maxOutstandingSegs + ", " +
                maxRetrans + ", " +
                maxCumulativeAcks + ", " +
                maxOutOfSequence + ", " +
                maxAutoReset + ", " +
                nullSegmentTimeout + ", " +
                retransmissionTimeout + ", " +
                cumulativeAckTimeout +
                "]";
    }

}
