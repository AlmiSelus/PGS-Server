package com.almi.pgs.rudp;

import com.almi.pgs.rudp.messages.Message;
import com.almi.pgs.rudp.messages.SYNMessage;
import com.almi.pgs.rudp.sockets.ReliableSocketStateListener;
import com.almi.pgs.rudp.sockets.ReliableUDPSocket;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Almi on 2016-12-22.
 */
public class ReliableUDPServerSocket extends ServerSocket {

    public ReliableUDPServerSocket() throws IOException {
        this(0, 0, null);
    }

    public ReliableUDPServerSocket(int port) throws IOException {
        this(port, 0, null);
    }

    public ReliableUDPServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    public ReliableUDPServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        this(new DatagramSocket(new InetSocketAddress(bindAddr, port)), backlog);
    }

    public ReliableUDPServerSocket(DatagramSocket sock, int backlog) throws IOException {
        if (sock == null) {
            throw new NullPointerException("sock");
        }

        _serverSock = sock;
        _backlogSize = (backlog <= 0) ? DEFAULT_BACKLOG_SIZE : backlog;
        _backlog = new ArrayList<>(_backlogSize);
        _clientSockTable = new HashMap<>();
        _stateListener = new StateListener();
        _timeout = 0;
        _closed = false;

        new ReceiverThread().start();
    }

    public ReliableUDPSocket accept() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        synchronized (_backlog) {
            while (_backlog.isEmpty()) {
                try {
                    if (_timeout == 0) {
                        _backlog.wait();
                    } else {
                        long startTime = System.currentTimeMillis();
                        _backlog.wait(_timeout);
                        if (System.currentTimeMillis() - startTime >= _timeout) {
                            throw new SocketTimeoutException();
                        }
                    }

                } catch (InterruptedException xcp) {
                    xcp.printStackTrace();
                }

                if (isClosed()) {
                    throw new IOException();
                }
            }

            return _backlog.remove(0);
        }
    }

    public synchronized void bind(SocketAddress endpoint) throws IOException {
        bind(endpoint, 0);
    }

    public synchronized void bind(SocketAddress endpoint, int backlog) throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        _serverSock.bind(endpoint);
    }

    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        _closed = true;
        synchronized (_backlog) {
            _backlog.clear();
            _backlog.notify();
        }

        if (_clientSockTable.isEmpty()) {
            _serverSock.close();
        }
    }

    public InetAddress getInetAddress() {
        return _serverSock.getInetAddress();
    }

    public int getLocalPort() {
        return _serverSock.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return _serverSock.getLocalSocketAddress();
    }

    public boolean isBound() {
        return _serverSock.isBound();
    }

    public boolean isClosed() {
        return _closed;
    }

    public void setSoTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }

        _timeout = timeout;
    }

    public int getSoTimeout() {
        return _timeout;
    }

    /**
     * Registers a new client socket with the specified endpoint address.
     *
     * @param endpoint    the new socket.
     * @return the registered socket.
     */
    private ReliableUDPClientSocket addClientSocket(SocketAddress endpoint) {
        synchronized (_clientSockTable) {
            ReliableUDPClientSocket sock = _clientSockTable.get(endpoint);

            if (sock == null) {
                try {
                    sock = new ReliableUDPClientSocket(_serverSock, endpoint);
                    sock.addStateListener(_stateListener);
                    _clientSockTable.put(endpoint, sock);
                }
                catch (IOException xcp) {
                    xcp.printStackTrace();
                }
            }

            return sock;
        }
    }

    /**
     * Deregisters a client socket with the specified endpoint address.
     *
     * @param endpoint     the socket.
     * @return the deregistered socket.
     */
    private ReliableUDPClientSocket removeClientSocket(SocketAddress endpoint) {
        synchronized (_clientSockTable) {
            ReliableUDPClientSocket sock = _clientSockTable.remove(endpoint);

            if (_clientSockTable.isEmpty()) {
                if (isClosed()) {
                    _serverSock.close();
                }
            }

            return sock;
        }
    }

    private DatagramSocket _serverSock;
    private int            _timeout;
    private int            _backlogSize;
    private boolean        _closed;

    /*
     * The listen backlog queue.
     */
    private final ArrayList<ReliableUDPSocket>      _backlog;

    /*
     * A table of active opened client sockets.
     */
    private final HashMap<SocketAddress, ReliableUDPClientSocket>   _clientSockTable;

    private ReliableSocketStateListener _stateListener;

    private static final int DEFAULT_BACKLOG_SIZE = 50;

    private class ReceiverThread extends Thread {
        ReceiverThread() {
            super("ReliableServerSocket");
            setDaemon(true);
        }

        public void run() {
            byte[] buffer = new byte[65535];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                ReliableUDPClientSocket sock;

                try {
                    _serverSock.receive(packet);
                    SocketAddress endpoint = packet.getSocketAddress();
                    Message s = Message.parseMessage(packet.getData());

                    synchronized (_clientSockTable) {

                        if (!isClosed()) {
                            if (s instanceof SYNMessage) {
                                if (!_clientSockTable.containsKey(endpoint)) {
                                    sock = addClientSocket(endpoint);
                                }
                            }
                        }

                        sock = _clientSockTable.get(endpoint);
                    }

                    if (sock != null) {
                        sock.messageReceived(s);
                    }
                } catch (IOException xcp) {
                    if (isClosed()) {
                        break;
                    }
                    xcp.printStackTrace();
                }
            }
        }
    }

    private class StateListener implements ReliableSocketStateListener {

        public void connectionOpened(ReliableUDPSocket sock) {
            if (sock != null) {
                synchronized (_backlog) {
                    while (_backlog.size() > DEFAULT_BACKLOG_SIZE) {
                        try {
                            _backlog.wait();
                        } catch (InterruptedException xcp) {
                            xcp.printStackTrace();
                        }
                    }

                    _backlog.add(sock);
                    _backlog.notify();
                }
            }
        }

        public void connectionRefused(ReliableUDPSocket sock) {
            // do nothing.
        }

        public void connectionClosed(ReliableUDPSocket sock) {
            // Remove client socket from the table of active connections.
            if (sock != null) {
                removeClientSocket(sock.getRemoteSocketAddress());
            }
        }

        public void connectionFailure(ReliableUDPSocket sock) {
            // Remove client socket from the table of active connections.
            if (sock != null) {
                removeClientSocket(sock.getRemoteSocketAddress());
            }
        }

        public void connectionReset(ReliableUDPSocket sock) {
            // do nothing.
        }
    }
}
