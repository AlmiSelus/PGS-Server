package com.almi.pgs.rudp.sockets;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Almi on 2016-12-28.
 */
public class ReliableSocketInputStream extends InputStream {

    private ReliableUDPSocket _sock;
    private byte[] _buf;
    private int _pos;
    private int _count;

	ReliableSocketInputStream(ReliableUDPSocket sock) throws IOException {
            if (sock == null) {
                throw new NullPointerException("sock");
            }

            _sock = sock;
            _buf = new byte[_sock.getReceiveBufferSize()];
            _pos = _count = 0;
        }

    public synchronized int read() throws IOException {
        if (readImpl() < 0) {
            return -1;
        }

        return (_buf[_pos++] & 0xFF);
    }

    public synchronized int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }

        if (off < 0 || len < 0 || (off + len) > b.length) {
            throw new IndexOutOfBoundsException();
        }

        if (readImpl() < 0) {
            return -1;
        }

        int readBytes = Math.min(available(), len);
        System.arraycopy(_buf, _pos, b, off, readBytes);
        _pos += readBytes;

        return readBytes;
    }

    public synchronized int available() {
        return (_count - _pos);
    }

    public boolean markSupported() {
        return false;
    }

    public void close() throws IOException {
        _sock.shutdownInput();
    }

    private int readImpl() throws IOException {
        if (available() == 0) {
            _count = _sock.read(_buf, 0, _buf.length);
            _pos = 0;
        }

        return _count;
    }

}

