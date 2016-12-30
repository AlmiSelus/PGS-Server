package com.almi.pgs.rudp.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Almi on 2016-12-27.
 *
 * Timer for timeout
 */
public class Timeout extends Thread {

    private final static Logger log = LoggerFactory.getLogger(Timeout.class);

    private Runnable _task;
    private long     _delay;
    private long     _period;
    private boolean  _canceled;
    private boolean  _scheduled;
    private boolean  _reset;
    private boolean  _stopped;
    private final Object   _lock = new Object();

    public Timeout(String name, Runnable task) {
        super(name);
        setDaemon(true);

        _task = task;
        _delay = 0;
        _period = 0;
        start();
    }

    public void run() {
        while (!_stopped) {

            synchronized (this) {

                while (!_scheduled && !_stopped) {
                    try {
                        wait();
                    }
                    catch (InterruptedException xcp) {
                        xcp.printStackTrace();
                    }
                }

                if (_stopped) {
                    break;
                }
            }

            synchronized (_lock) {

                _reset = false;
                _canceled = false;

                if (_delay > 0) {
                    try {
                        _lock.wait(_delay);
                    }
                    catch (InterruptedException xcp) {
                        xcp.printStackTrace();
                    }
                }

                if (_canceled) {
                    continue;
                }
            }

            if (!_reset) {
                _task.run();
            }

            if (_period > 0) {

                while (true) {

                    synchronized (_lock) {

                        _reset = false;

                        try {
                            _lock.wait(_period);
                        }
                        catch (InterruptedException xcp) {
                            xcp.printStackTrace();
                        }

                        if (_canceled) {
                            break;
                        }

                        if (_reset) {
                            continue;
                        }
                    }

                    _task.run();

                }
            }
        }
    }

    public synchronized void schedule(long delay) {
        schedule(delay, 0);
    }

    public synchronized void schedule(long delay, long period) {
        if (_scheduled) {
//            throw new IllegalStateException("already scheduled");
            log.info("Has already been scheduled! Ignoring");
            return;
        }
        _delay = delay;
        _period = period;




        _scheduled = true;
        notify();

        synchronized (_lock) {
            _lock.notify();
        }
    }

    public synchronized boolean isScheduled() {
        return _scheduled;
    }

    public synchronized boolean isIdle() {
        return !isScheduled();
    }

    public synchronized void reset() {
        synchronized (_lock) {
            _reset = true;
            _lock.notify();
        }
    }

    public synchronized void cancel() {
        _scheduled = false;
        synchronized (_lock) {
            _canceled = true;
            _lock.notify();
        }
    }

    public synchronized void destroy() {
        cancel();
        _stopped = true;
        notify();
    }


}
