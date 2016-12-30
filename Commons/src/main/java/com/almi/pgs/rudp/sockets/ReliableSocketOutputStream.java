package com.almi.pgs.rudp.sockets;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Almi on 2016-12-28.
 */
class ReliableSocketOutputStream extends OutputStream {

    private ReliableUDPSocket _sock;
    private byte[]         _buf;
    private int            _count;

    ReliableSocketOutputStream(ReliableUDPSocket sock) throws IOException {
        if (sock == null) {
            throw new NullPointerException("sock");
        }

        _sock = sock;
        _buf = new byte[_sock.getSendBufferSize()];
        _count = 0;
    }

    public synchronized void write(int b) throws IOException {
        if (_count >= _buf.length) {
            flush();
        }

        _buf[_count++] = (byte) (b & 0xFF);
    }

    public synchronized void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }

        if (off < 0 || len < 0 || (off+len) > b.length) {
            throw new IndexOutOfBoundsException();
        }

        int buflen;
        int writtenBytes = 0;

        while (writtenBytes < len) {
            buflen = Math.min(_buf.length, len-writtenBytes);
            if (buflen > (_buf.length - _count)) {
                flush();
            }
            System.arraycopy(b, off+writtenBytes, _buf, _count, buflen);
            _count += buflen;
            writtenBytes += buflen;
        }
    }

    public synchronized void flush() throws IOException {
        if (_count > 0) {
            _sock.write(_buf, 0, _count);
            _count = 0;
        }
    }

    public synchronized void close() throws IOException {
        flush();
        _sock.shutdownOutput();
    }

}
