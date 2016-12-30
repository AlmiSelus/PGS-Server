package com.almi.pgs.rudp.sockets;

/**
 * Created by Almi on 2016-12-28.
 */
public class ReliableUDPSessionVars {
    public ReliableUDPSessionVars() {
    }

    public synchronized int nextSequenceNumber() {
        return (_seqn = ReliableUDPSocket.nextSequenceNumber(_seqn));
    }

    public synchronized int setSequenceNumber(int n) {
        _seqn = n;
        return _seqn;
    }

    public synchronized int setLastInSequence(int n) {
        _lastInSequence = n;
        return _lastInSequence;
    }

    public synchronized int getLastInSequence() {
        return _lastInSequence;
    }

    public synchronized void incCumulativeAckCounter() {
        _cumAckCounter++;
    }

    public synchronized int getCumulativeAckCounter() {
        return _cumAckCounter;
    }

    public synchronized int getAndResetCumulativeAckCounter() {
        int tmp = _cumAckCounter;
        _cumAckCounter = 0;
        return tmp;
    }

    public synchronized void incOutOfSequenceCounter() {
        _outOfSeqCounter++;
    }

    public synchronized int getOutOfSequenceCounter() {
        return _outOfSeqCounter;
    }

    public synchronized int getAndResetOutOfSequenceCounter() {
        int tmp = _outOfSeqCounter;
        _outOfSeqCounter = 0;
        return tmp;
    }

    public synchronized void incOutstandingSegsCounter() {
        _outSegsCounter++;
    }

    public synchronized int getOutstandingSegsCounter() {
        return _outSegsCounter;
    }

    public synchronized int getAndResetOutstandingSegsCounter() {
        int tmp = _outSegsCounter;
        _outSegsCounter = 0;
        return tmp;
    }

    public synchronized void reset() {
        _outOfSeqCounter = 0;
        _outSegsCounter  = 0;
        _cumAckCounter   = 0;
    }

    private int _seqn;             /* Segment sequence number */
    private int _lastInSequence;   /* Last in-sequence received segment */

    /*
     * The receiver maintains a counter of unacknowledged segments received
     * without an acknowledgment being sent to the transmitter. The maximum
     * value of this counter is configurable. If this counter's maximum is
     * exceeded, the receiver sends either a stand-alone acknowledgment, or
     * an extended acknowledgment if there are currently any out-of-sequence
     * segments. The recommended value for the cumulative acknowledge counter
     * is 3.
     */
    private int _cumAckCounter; /* Cumulative acknowledge counter */

    /*
     * The receiver maintains a counter of the number of segments that have
     * arrived out-of-sequence. Each time this counter exceeds its configurable
     * maximum, an extended acknowledgment segment containing the sequence
     * numbers of all current out-of-sequence segments that have been received
     * is sent to the transmitter. The counter is then reset to zero. The
     * recommended value for the out-of-sequence acknowledgments counter is 3.
     */
    private int _outOfSeqCounter; /* Out-of-sequence acknowledgments counter */

    /*
     * The transmitter maintains a counter of the number of segments that
     * have been sent without getting an acknowledgment. This is used
     * by the receiver as a mean of flow control.
     */
    private int _outSegsCounter;
}
