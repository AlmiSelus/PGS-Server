package com.almi.pgs.rudp.sockets;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.rudp.messages.*;
import com.almi.pgs.rudp.timer.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.almi.pgs.rudp.sockets.ReliableUDPState.*;

/**
 * Created by Almi on 2016-12-28.
 */
public class ReliableUDPSocket extends Socket {

    private final static Logger log = LoggerFactory.getLogger(ReliableUDPSocket.class);
    private final static int MAX_SEQUENCE_NUMBER = 255;

    private DatagramSocket socket;
    private ReliableUDPSettings settings;
    private ShutdownHook shutdownHook;
    private Thread socketThread = new ReliableSocketThread();
    private int sendBufferSize;
    private int recvBufferSize;
    protected SocketAddress endpoint;
    private ReliableUDPState state = CLOSED;
    public ReliableUDPSessionVars counters = new ReliableUDPSessionVars();

    public final Queue<Message> unackedSentQueue = new ConcurrentLinkedQueue<>();
    public final Queue<Message> outSeqRecvQueue = new ConcurrentLinkedQueue<>();
    public final Queue<Message> inSeqRecvQueue = new ConcurrentLinkedQueue<>();

    private final Queue<ReliableSocketListener> listeners = new ConcurrentLinkedQueue<>();
    private final Queue<ReliableSocketStateListener> stateListeners = new ConcurrentLinkedQueue<>();


    protected ReliableSocketInputStream  _in;
    protected ReliableSocketOutputStream _out;

    private int _sendQueueSize = 32; /* Maximum number of received segments */
    private int _recvQueueSize = 32; /* Maximum number of sent segments */

    /**
     * Timeout timers
     */
    private final Timeout retransmissionTimer =
            new Timeout("ReliableSocket-RetransmissionTimer", new RetransmissionTimeout(this, unackedSentQueue));

    /*
     * This timer is started when the connection is opened and is reset
     * every time a data segment is sent. If the client's null segment
     * timer expires, the client sends a null segment to the server.
     */
    private Timeout nullMessageTimer =
            new Timeout("ReliableSocket-NullSegmentTimerTask", new NullSegmentTimerTask(this));

    /*
     * When this timer expires, if there are segments on the out-of-sequence
     * queue, an extended acknowledgment is sent. Otherwise, if there are
     * any segments currently unacknowledged, a stand-alone acknowledgment
     * is sent.
     * The cumulative acknowledge timer is restarted whenever an acknowledgment
     * is sent in a data, null, or reset segment, provided that there are no
     * segments currently on the out-of-sequence queue. If there are segments
     * on the out-of-sequence queue, the timer is not restarted, so that another
     * extended acknowledgment will be sent when it expires again.
     */
    private final Timeout _cumulativeAckTimer =
            new Timeout("ReliableSocket-CumulativeAckTimer", new CumulativeACKTimeout(this));

    /*
     * When this timer expires, the connection is considered broken.
     */
    private Timeout keepAliveTimeout =
            new Timeout("ReliableSocket-KeepAliveTimer", new KeepAliveTimeout(this));
    private final Object closeLock = new Object();
    private boolean closed = false;
    private boolean connected = false;
    private final Object resetLock = new Object();
    private final Object recvQueueLock = new Object();
    private boolean reset = false;
    private boolean _closed    = false;
    private byte[] receiverBuffer = new byte[Constants.BUFFER_SIZE];
    private long timeout = 0;

    public ReliableUDPSocket() throws IOException {
        this(new ReliableUDPSettings());
    }

    public ReliableUDPSocket(ReliableUDPSettings profile) throws IOException {
        this(new DatagramSocket(), profile);
    }

    public ReliableUDPSocket(String host, int port) throws UnknownHostException, IOException {
        this(new InetSocketAddress(host, port), null);
    }

    public ReliableUDPSocket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        this(new InetSocketAddress(address, port), new InetSocketAddress(localAddr, localPort));
    }

    public ReliableUDPSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        this(new InetSocketAddress(host, port), new InetSocketAddress(localAddr, localPort));
    }

    public ReliableUDPSocket(DatagramSocket sock) {
        this(sock, new ReliableUDPSettings());
    }

    private ReliableUDPSocket(InetSocketAddress inetAddr, InetSocketAddress localAddr) throws IOException {
        this(new DatagramSocket(localAddr), new ReliableUDPSettings());
        connect(inetAddr);
    }

    private ReliableUDPSocket(DatagramSocket sock, ReliableUDPSettings profile) {
        if (sock == null) {
            throw new NullPointerException("sock");
        }

        init(sock, profile);
    }

    protected void init(DatagramSocket sock, ReliableUDPSettings profile) {
        socket = sock;
        settings = profile;
        shutdownHook = new ShutdownHook();

        sendBufferSize    = (settings.maxMessageSize() - Message.HEADER_LENGTH) * 32;
        recvBufferSize = (settings.maxMessageSize() - Message.HEADER_LENGTH) * 32;

        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }

        socketThread.start();

        keepAliveTimeout.schedule(profile.nullMessageTimeout() * 6,
                profile.nullMessageTimeout() * 6);
    }

    public InputStream getInputStream()
            throws IOException
    {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }

        if (isInputShutdown()) {
            throw new SocketException("Socket input is shutdown");
        }

        return _in;
    }

    public OutputStream getOutputStream()
            throws IOException
    {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }

        if (isOutputShutdown()) {
            throw new SocketException("Socket output is shutdown");
        }

        return _out;
    }


    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }

        if (timeout < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }

        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (isConnected()) {
            throw new SocketException("already connected");
        }

        if (!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }

        this.endpoint = endpoint;

        // Synchronize sequence numbers
        state = SYN_SENT;
        Random rand = new Random(System.currentTimeMillis());
        Message syn = new SYNMessage(counters.setSequenceNumber(rand.nextInt(MAX_SEQUENCE_NUMBER)),
                settings.maxOutstandingSegs(),
                settings.maxMessageSize(),
                settings.retransmissionTimeout(),
                settings.cumulativeAckTimeout(),
                settings.nullMessageTimeout(),
                settings.maxRetrans(),
                settings.maxCumulativeAcks(),
                settings.maxOutOfSequence(),
                settings.maxAutoReset());

        sendAndQueueMessage(syn);

        // Wait for connection establishment (or timeout)
        boolean timedout = false;
        synchronized (this) {
            if (!isConnected()) {
                try {
                    if (timeout == 0) {
                        wait();
                    } else {
                        long startTime = System.currentTimeMillis();
                        wait(timeout);
                        if (System.currentTimeMillis() - startTime >= timeout) {
                            timedout = true;
                        }
                    }
                } catch (InterruptedException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }
        }

        if (state == ESTABLISHED) {
            return;
        }

        synchronized (unackedSentQueue) {
            unackedSentQueue.clear();
            unackedSentQueue.notifyAll();
        }

        counters.reset();
        retransmissionTimer.cancel();

        switch (state) {
            case SYN_SENT:
                connectionRefused();
                state = CLOSED;
                if (timedout) {
                    throw new SocketTimeoutException();
                }
                throw new SocketException("Connection refused");
            case CLOSED:
            case CLOSE_WAIT:
                state = CLOSED;
                throw new SocketException("Socket closed");
        }
    }

    public SocketChannel getChannel() {
        return null;
    }

    public InetAddress getInetAddress() {
        if (!isConnected()) {
            return null;
        }

        return ((InetSocketAddress)endpoint).getAddress();
    }

    public int getPort() {
        if (!isConnected()) {
            return 0;
        }

        return ((InetSocketAddress)endpoint).getPort();
    }

    public SocketAddress getRemoteSocketAddress() {
        if (!isConnected()) {
            return null;
        }

        return new InetSocketAddress(getInetAddress(), getPort());
    }

    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    public synchronized void close()
            throws IOException
    {

        synchronized (closeLock) {

            if (isClosed()) {
                return;
            }

            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                log.error(ExceptionUtils.getStackTrace(e));
            }

            switch (state) {
                case SYN_SENT:
                    synchronized (this) {
                        notify();
                    }
                    break;
                case CLOSE_WAIT:
                case SYN_RCVD:
                case ESTABLISHED:
                    sendMessage(new TCSMessage(counters.nextSequenceNumber()));
                    closeImpl();
                    break;
                case CLOSED:
                    retransmissionTimer.destroy();
                    _cumulativeAckTimer.destroy();
                    keepAliveTimeout.destroy();
                    nullMessageTimer.destroy();
                    socket.close();
                    break;
            }

            closed = true;
            state = CLOSED;

            synchronized (unackedSentQueue) {
                unackedSentQueue.notify();
            }

            synchronized (inSeqRecvQueue) {
                inSeqRecvQueue.notify();
            }
        }
    }

    public boolean isBound()
    {
        return socket.isBound();
    }

    public boolean isConnected()
    {
        return connected;
    }

    public boolean isClosed()
    {
        synchronized (closeLock) {
            return closed;
        }
    }

    public void setSoTimeout(int timeout)
            throws SocketException
    {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }

        this.timeout = timeout;
    }

    public synchronized void setSendBufferSize(int size)
            throws SocketException
    {
        if (!(size > 0)) {
            throw new IllegalArgumentException("negative receive size");
        }

        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (isConnected()) {
            return;
        }

        sendBufferSize = size;
    }

    public synchronized int getSendBufferSize()
            throws SocketException
    {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        return sendBufferSize;
    }

    public synchronized void setReceiveBufferSize(int size)
            throws SocketException
    {
        if (!(size > 0)) {
            throw new IllegalArgumentException("negative send size");
        }

        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (isConnected()) {
            return;
        }

        recvBufferSize = size;
    }

    public synchronized int getReceiveBufferSize()
            throws SocketException
    {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        return recvBufferSize;
    }

    public void setTcpNoDelay(boolean on)
            throws SocketException
    {
        throw new SocketException("Socket option not supported");
    }

    public boolean getTcpNoDelay()
    {
        return false;
    }

    public void reset(ReliableUDPSettings profile)
            throws IOException
    {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (!isConnected()) {
            throw new SocketException("Socket is not connected");
        }

        synchronized (resetLock) {
            reset = true;

            sendAndQueueMessage(new RSTMessage(counters.nextSequenceNumber()));

            // Wait to flush all outstanding Messages (including last RST Message).
            synchronized (unackedSentQueue) {
                while (!unackedSentQueue.isEmpty()) {
                    try {
                        unackedSentQueue.wait();
                    }
                    catch (InterruptedException xcp) {
                        xcp.printStackTrace();
                    }
                }
            }
        }

        connectionReset();

        // Set new profile
        if (profile != null) {
            this.settings = profile;
        }

        // Synchronize sequence numbers
        state = SYN_SENT;
        Random rand = new Random(System.currentTimeMillis());
        Message syn = new SYNMessage(counters.setSequenceNumber(rand.nextInt(MAX_SEQUENCE_NUMBER)),
                profile.maxOutstandingSegs(),
                profile.maxMessageSize(),
                profile.retransmissionTimeout(),
                profile.cumulativeAckTimeout(),
                profile.nullMessageTimeout(),
                profile.maxRetrans(),
                profile.maxCumulativeAcks(),
                profile.maxOutOfSequence(),
                profile.maxAutoReset());

        sendAndQueueMessage(syn);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (isOutputShutdown()) {
            throw new IOException("Socket output is shutdown");
        }

        if (!isConnected()) {
            throw new SocketException("Connection reset");
        }

        int totalBytes = 0;
        while (totalBytes < len) {
            synchronized (resetLock) {
                while (reset) {
                    try {
                        resetLock.wait();
                    } catch (InterruptedException xcp) {
                        xcp.printStackTrace();
                    }
                }

                int writeBytes = Math.min(settings.maxMessageSize() - Message.HEADER_LENGTH, len - totalBytes);

                sendAndQueueMessage(new DATMessage(counters.nextSequenceNumber(),
                        counters.getLastInSequence(), b));
                totalBytes += writeBytes;
            }
        }
    }

    public int read(byte[] b, int off, int len)
            throws IOException
    {

        int totalBytes = 0;

        synchronized (recvQueueLock) {

            while (true) {
                while (inSeqRecvQueue.isEmpty()) {

                    if (isClosed()) {
                        throw new SocketException("Socket is closed");
                    }

                    if (isInputShutdown()) {
                        throw new EOFException();
                    }

                    if (!isConnected()) {
                        throw new SocketException("Connection reset");
                    }

                    try {
                        if (timeout == 0) {
                            recvQueueLock.wait();
                        }
                        else {
                            long startTime = System.currentTimeMillis();
                            recvQueueLock.wait(timeout);
                            if ((System.currentTimeMillis() - startTime) >= timeout) {
                                throw new SocketTimeoutException();
                            }
                        }
                    }
                    catch (InterruptedException xcp) {
                        if(!_closed)
                            throw new InterruptedIOException(xcp.getMessage());
                    }
                }

                for (Iterator<Message> it = inSeqRecvQueue.iterator(); it.hasNext(); ) {
                    Message s = it.next();

                    if (s instanceof RSTMessage) {
                        it.remove();
                        break;
                    } else if (s instanceof TCSMessage) {
                        if (totalBytes <= 0) {
                            it.remove();
                            return -1; /* EOF */
                        }
                        break;
                    } else if (s instanceof DATMessage) {
                        byte[] data = ((DATMessage) s).getData();
                        if (data.length + totalBytes > len) {
                            if (totalBytes <= 0) {
                                throw new IOException("insufficient buffer space");
                            }
                            break;
                        }

                        System.arraycopy(data, 0, b, off+totalBytes, data.length);
                        totalBytes += data.length;
                        it.remove();
                    }
                }

                if (totalBytes > 0) {
                    return totalBytes;
                }
            }
        }
    }

    public void addListener(ReliableSocketListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void addStateListener(ReliableSocketStateListener stateListener) {
        if (stateListener == null) {
            throw new NullPointerException("stateListener");
        }

        synchronized (stateListeners) {
            if (!stateListeners.contains(stateListener)) {
                stateListeners.add(stateListener);
            }
        }
    }

    private void sendMessage(Message msg) throws IOException {
        /* Piggyback any pending acknowledgments */
        if (msg instanceof DATMessage || msg instanceof RSTMessage || msg instanceof TCSMessage || msg instanceof NULMessage) {
            checkAndSetAck(msg);
        }

        /* Reset null Message timer */
        if (msg instanceof DATMessage || msg instanceof RSTMessage || msg instanceof TCSMessage) {
            nullMessageTimer.reset();
        }

        log.info("Sent message : " + msg.typeToString());
        if(msg instanceof DATMessage) {
            log.info(new String(((DATMessage)msg).getData()));
        }

        sendMessageImpl(msg);
    }

    private Message receiveMessage() throws IOException {
        Message s;
        if ((s = receiveMessageImpl()) != null) {

            log.info("Received " + s.typeToString());

            if (s instanceof DATMessage || s instanceof NULMessage ||
                    s instanceof RSTMessage || s instanceof TCSMessage ||
                    s instanceof SYNMessage) {
                counters.incCumulativeAckCounter();
            }

            keepAliveTimeout.reset();
        }

        return s;
    }

    public void sendAndQueueMessage(Message msg) throws IOException {
        synchronized (unackedSentQueue) {
            while ((unackedSentQueue.size() >= _sendQueueSize) || (counters.getOutstandingSegsCounter() > settings.maxOutstandingSegs())) {
                try {
                    unackedSentQueue.wait();
                } catch (InterruptedException xcp) {
                    xcp.printStackTrace();
                }
            }

            counters.incOutstandingSegsCounter();
            unackedSentQueue.add(msg);
        }

        if (_closed) {
            throw new SocketException("Socket is closed");
        }

        /* Re-start retransmission timer */
        if (!(msg instanceof EACKMessage) && !(msg instanceof ACKMessage)) {
            synchronized (retransmissionTimer) {
                if (retransmissionTimer.isIdle()) {
                    retransmissionTimer.schedule(settings.retransmissionTimeout(),
                            settings.retransmissionTimeout());
                }
            }
        }

        sendMessage(msg);

        if (msg instanceof DATMessage) {
            synchronized (listeners) {
                for (ReliableSocketListener l : listeners) {
                    l.packetSent();
                }
            }
        }
    }

    public void retransmitMessage(Message msg) throws IOException {
        if (settings.maxRetrans() > 0) {
            msg.setRetransmissionCounter(msg.getRetransmissionCounter()+1);
        }

        if (settings.maxRetrans() != 0 && msg.getRetransmissionCounter() > settings.maxRetrans()) {
            connectionFailure();
            return;
        }

        sendMessage(msg);

        if (msg instanceof DATMessage) {
            synchronized (listeners) {
                for (ReliableSocketListener l : listeners) {
                    l.packetRetransmitted();
                }
            }
        }
    }

    private void connectionOpened() {
        if (isConnected()) {

            nullMessageTimer.cancel();
            keepAliveTimeout.cancel();

            synchronized (resetLock) {
                reset = false;
                resetLock.notify();
            }
        }
        else {
            synchronized (this) {
                try {
                    _in = new ReliableSocketInputStream(this);
                    _out = new ReliableSocketOutputStream(this);
                    connected = true;
                    state = ESTABLISHED;
                } catch (IOException xcp) {
                    xcp.printStackTrace();
                }

                notify();
            }

            synchronized (stateListeners) {
                for (ReliableSocketStateListener l : stateListeners) {
                    l.connectionOpened(this);
                }
            }
        }

        nullMessageTimer.schedule(0, settings.nullMessageTimeout());

        keepAliveTimeout.schedule(settings.nullMessageTimeout() * 6,
                settings.nullMessageTimeout() * 6);
    }

    private void connectionRefused() {
        synchronized (stateListeners) {
            for (ReliableSocketStateListener l : stateListeners) {
                l.connectionRefused(this);
            }
        }
    }

    private void connectionClosed() {
        synchronized (stateListeners) {
            for (ReliableSocketStateListener l : stateListeners) {
                l.connectionClosed(this);
            }
        }
    }

    public void connectionFailure() {
        synchronized (closeLock) {

            if (isClosed()) {
                return;
            }

            switch (state) {
                case SYN_SENT:
                    synchronized (this) {
                        notify();
                    }
                    break;
                case CLOSE_WAIT:
                case SYN_RCVD:
                case ESTABLISHED:
                    connected = false;
                    synchronized (unackedSentQueue) {
                        unackedSentQueue.notifyAll();
                    }

                    synchronized (recvQueueLock) {
                        recvQueueLock.notify();
                    }

                    closeImpl();
                    break;
            }

            state = CLOSED;
            closed = true;
        }

        synchronized (stateListeners) {
            for (ReliableSocketStateListener l : stateListeners) {
                l.connectionFailure(this);
            }
        }
    }

    private void connectionReset() {
        synchronized (stateListeners) {
            for (ReliableSocketStateListener l : stateListeners) {
                l.connectionReset(this);
            }
        }
    }

    private void handleSYNMessage(SYNMessage synMessage)
    {
        try {
            switch (state) {
                case CLOSED:
                    counters.setLastInSequence(synMessage.getSequenceNumber());
                    state = SYN_RCVD;

                    Random rand = new Random(System.currentTimeMillis());
                    settings = new ReliableUDPSettings(
                            _sendQueueSize,
                            _recvQueueSize,
                            synMessage.getMaxSegmentSize(),
                            synMessage.getMaxNumberOfOutstandingSegments(),
                            synMessage.getMaxRetransmissions(),
                            synMessage.getMaxCumulativeAck(),
                            synMessage.getMaxOutOfSequence(),
                            synMessage.getMaxAutoReset(),
                            synMessage.getNullSegmentTimeout(),
                            synMessage.getRetransmissionTimeout(),
                            synMessage.getCumulativeACKTimeout());

                    Message syn = new SYNMessage(counters.setSequenceNumber(rand.nextInt(MAX_SEQUENCE_NUMBER)),
                            settings.maxOutstandingSegs(),
                            settings.maxMessageSize(),
                            settings.retransmissionTimeout(),
                            settings.cumulativeAckTimeout(),
                            settings.nullMessageTimeout(),
                            settings.maxRetrans(),
                            settings.maxCumulativeAcks(),
                            settings.maxOutOfSequence(),
                            settings.maxAutoReset());

                    syn.setAck(syn.getSequenceNumber());
                    sendAndQueueMessage(syn);
                    break;
                case SYN_SENT:
                    counters.setLastInSequence(synMessage.getSequenceNumber());
                    state = ESTABLISHED;
                    /*
                     * Here the client accepts or rejects the parameters sent by the
                     * server. For now we will accept them.
                     */
                    sendAck();
                    connectionOpened();
                    break;
            }
        }
        catch (IOException xcp) {
            xcp.printStackTrace();
        }
    }

    private void handleEACKMessage(EACKMessage msg) {
        Iterator<Message> it;
        int[] acks = msg.getAcks();

        int lastInSequence = msg.getAckNumber();
        int lastOutSequence = acks[acks.length-1];

        synchronized (unackedSentQueue) {

            /* Removed acknowledged Messages from sent queue */
            for (it = unackedSentQueue.iterator(); it.hasNext(); ) {
                Message s = it.next();
                if ((sequenceNumbersComparator.compare(s.getSequenceNumber(), lastInSequence) <= 0)) {
                    it.remove();
                    continue;
                }

                for (int i = 0; i < acks.length; i++) {
                    if ((sequenceNumbersComparator.compare(s.getSequenceNumber(), acks[i]) == 0)) {
                        it.remove();
                        break;
                    }
                }
            }

            /* Retransmit Messages */
            it = unackedSentQueue.iterator();
            while (it.hasNext()) {
                Message s = it.next();
                if ((sequenceNumbersComparator.compare(lastInSequence, s.getSequenceNumber()) < 0) &&
                        (sequenceNumbersComparator.compare(lastOutSequence, s.getSequenceNumber()) > 0)) {

                    try {
                        retransmitMessage(s);
                    }
                    catch (IOException xcp) {
                        xcp.printStackTrace();
                    }
                }
            }

            unackedSentQueue.notifyAll();
        }
    }

    private void handleMessage(Message msg) {
        if (msg instanceof RSTMessage) {
            synchronized (resetLock) {
                reset = true;
            }

            connectionReset();
        }

        if (msg instanceof TCSMessage) {
            switch (state) {
                case SYN_SENT:
                    synchronized (this) {
                        notify();
                    }
                    break;
                case CLOSED:
                    break;
                default:
                    state = CLOSE_WAIT;
            }
        }

        boolean inSequence = false;
        synchronized (recvQueueLock) {

            if (sequenceNumbersComparator.compare(msg.getSequenceNumber(), counters.getLastInSequence()) <= 0) {
                /* Drop packet: duplicate. */
            } else if (sequenceNumbersComparator.compare(msg.getSequenceNumber(), nextSequenceNumber(counters.getLastInSequence())) == 0) {
                inSequence = true;
                if (inSeqRecvQueue.size() == 0 || (inSeqRecvQueue.size() + outSeqRecvQueue.size() < _recvQueueSize)) {
                    /* Insert in-sequence Message */
                    counters.setLastInSequence(msg.getSequenceNumber());
                    if (msg instanceof DATMessage || msg instanceof RSTMessage || msg instanceof TCSMessage) {
                        inSeqRecvQueue.add(msg);
                    }

                    if (msg instanceof DATMessage) {
                        synchronized (listeners) {
                            for (ReliableSocketListener l : listeners) {
                                l.packetReceivedInOrder((DATMessage)msg);
                            }
                        }
                    }

                    checkRecvQueues();
                } else {
                    /* Drop packet: queue is full. */
                }
            }  else if (inSeqRecvQueue.size() + outSeqRecvQueue.size() < _recvQueueSize) {
                /* Insert out-of-sequence Message, in order */
                boolean added = false;
                PriorityQueue<Message> priority = new PriorityQueue<>((o1, o2) ->
                        sequenceNumbersComparator.compare(o1.getSequenceNumber(), o2.getSequenceNumber()));
                for(Message m : outSeqRecvQueue) {
                    priority.add(m);
                }
                priority.add(msg);

                outSeqRecvQueue.clear();
                for (Message m : priority) {
                    outSeqRecvQueue.add(m);
                }

                counters.incOutOfSequenceCounter();

                if (msg instanceof DATMessage) {
                    synchronized (listeners) {
                        for (ReliableSocketListener l : listeners) {
                            l.packetReceivedOutOfOrder((DATMessage)msg);
                        }
                    }
                }
            }

            if (inSequence && (msg instanceof RSTMessage ||
                    msg instanceof NULMessage ||
                    msg instanceof TCSMessage)) {
                sendAck();
            } else if ((counters.getOutOfSequenceCounter() > 0) &&
                    (settings.maxOutOfSequence() == 0 || counters.getOutOfSequenceCounter() > settings.maxOutOfSequence())) {
                sendExtendedAck();
            } else if ((counters.getCumulativeAckCounter() > 0) &&
                    (settings.maxCumulativeAcks() == 0 || counters.getCumulativeAckCounter() > settings.maxCumulativeAcks())) {
                sendSingleAck();
            } else {
                synchronized (_cumulativeAckTimer) {
                    if (_cumulativeAckTimer.isIdle()) {
                        _cumulativeAckTimer.schedule(settings.cumulativeAckTimeout());
                    }
                }
            }
        }
    }

    public void sendAck() {
        synchronized (recvQueueLock) {
            if (!outSeqRecvQueue.isEmpty()) {
                sendExtendedAck();
                return;
            }

            sendSingleAck();
        }
    }

    private void sendExtendedAck() {
        synchronized (recvQueueLock) {

            if (outSeqRecvQueue.isEmpty()) {
                return;
            }

            counters.getAndResetCumulativeAckCounter();
            counters.getAndResetOutOfSequenceCounter();

            /* Compose list of out-of-sequence sequence numbers */
            int[] acks = new int[outSeqRecvQueue.size()];
            int i = 0;
            for(Iterator<Message> outSeqIt = outSeqRecvQueue.iterator(); outSeqIt.hasNext();) {
                Message msg = outSeqIt.next();
                acks[i] = msg.getSequenceNumber();
                i++;
            }

            try {
                int lastInSequence = counters.getLastInSequence();
                sendMessage(new EACKMessage(nextSequenceNumber(lastInSequence),
                        lastInSequence, acks));
            }
            catch (IOException xcp) {
                xcp.printStackTrace();
            }

        }
    }

    private void sendSingleAck() {
        if (counters.getAndResetCumulativeAckCounter() == 0) {
            return;
        }

        try {
            int lastInSequence = counters.getLastInSequence();
            sendMessage(new ACKMessage(nextSequenceNumber(lastInSequence), lastInSequence));
        }
        catch (IOException xcp) {
            xcp.printStackTrace();
        }
    }

    private void checkAndSetAck(Message msg) {
        if (counters.getAndResetCumulativeAckCounter() == 0) {
            return;
        }

        msg.setAck(counters.getLastInSequence());
    }

    private void checkAndGetAck(Message msg) {
        int ackn = msg.getAckNumber();

        if (ackn < 0) {
            return;
        }

        counters.getAndResetOutstandingSegsCounter();

        if (state == SYN_RCVD) {
            state = ESTABLISHED;
            connectionOpened();
        }

        synchronized (unackedSentQueue) {
            Iterator<Message> it = unackedSentQueue.iterator();
            while (it.hasNext()) {
                Message s = it.next();
                if (sequenceNumbersComparator.compare(s.getSequenceNumber(), ackn) <= 0) {
                    it.remove();
                }
            }

            if (unackedSentQueue.isEmpty()) {
                retransmissionTimer.cancel();
            }

            unackedSentQueue.notifyAll();
        }
    }

    private void checkRecvQueues() {
        synchronized (recvQueueLock) {
            Iterator<Message> it = outSeqRecvQueue.iterator();
            while (it.hasNext()) {
                Message s = it.next();
                if (sequenceNumbersComparator.compare(s.getSequenceNumber(), nextSequenceNumber(counters.getLastInSequence())) == 0) {
                    counters.setLastInSequence(s.getSequenceNumber());
                    if (s instanceof DATMessage || s instanceof RSTMessage || s instanceof TCSMessage) {
                        inSeqRecvQueue.add(s);
                    }
                    it.remove();
                }
            }

            recvQueueLock.notify();
        }
    }

    protected void sendMessageImpl(Message s) throws IOException {
        try {
            DatagramPacket packet = new DatagramPacket(
                    s.getBytes(), s.length(), endpoint);
            socket.send(packet);
        } catch (IOException xcp) {
            if (!isClosed()) {
                xcp.printStackTrace();
            }
        }
    }

    protected Message receiveMessageImpl() throws IOException {
        try {
            DatagramPacket packet = new DatagramPacket(receiverBuffer, receiverBuffer.length);
            socket.receive(packet);
            return Message.parseMessage(packet.getData());
        } catch (IOException ioXcp) {
            if (!isClosed()) {
                ioXcp.printStackTrace();
            }
        }

        return null;
    }

    protected void closeSocket() {
        socket.close();
    }

    protected void closeImpl() {
        nullMessageTimer.cancel();
        keepAliveTimeout.cancel();
        state = CLOSE_WAIT;

        Thread t = new Thread() {
            public void run() {
                keepAliveTimeout.destroy();
                nullMessageTimer.destroy();

                try {
                    Thread.sleep(settings.nullMessageTimeout() * 2);
                }
                catch (InterruptedException xcp) {
                    xcp.printStackTrace();
                }

                retransmissionTimer.destroy();
                _cumulativeAckTimer.destroy();

                closeSocket();
                connectionClosed();
            }
        };
        t.setName("ReliableSocket-Closing");
        t.setDaemon(true);
        t.start();
    }

    static int nextSequenceNumber(int seqn) {
        return (seqn + 1) % MAX_SEQUENCE_NUMBER;
    }

    private Comparator<Integer> sequenceNumbersComparator = (seqn, aseqn) -> {
        if (seqn.equals(aseqn)) {
            return 0;
        } else if (((seqn < aseqn) && ((aseqn - seqn) > MAX_SEQUENCE_NUMBER/2)) || ((seqn > aseqn) && ((seqn - aseqn) < MAX_SEQUENCE_NUMBER/2))) {
            return 1;
        } else {
            return -1;
        }
    };

    private class ReliableSocketThread extends Thread {
        public ReliableSocketThread() {
            super("ReliableSocket");
            setDaemon(true);
        }

        public void run() {
            Message message;
            try {
                while ((message = receiveMessage()) != null) {
                    log.info(message.typeToString());
                    if (message instanceof SYNMessage) {
                        handleSYNMessage((SYNMessage) message);
                    } else if (message instanceof EACKMessage) {
                        handleEACKMessage((EACKMessage) message);
                    } else if (message instanceof ACKMessage) {
                        // do nothing.
                    } else {
                        handleMessage(message);
                    }

                    checkAndGetAck(message);
                }
            } catch (IOException xcp) {
                xcp.printStackTrace();
            }
        }
    }

    private class ShutdownHook extends Thread {
        public ShutdownHook() {
            super("ReliableSocket-ShutdownHook");
        }

        public void run() {
            try {
                switch (state) {
                    case CLOSED:
                        return;
                    default:
                        sendMessage(new TCSMessage(counters.nextSequenceNumber()));
                        break;
                }
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }
    }

}
