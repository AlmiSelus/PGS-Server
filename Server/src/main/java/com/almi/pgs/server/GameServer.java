package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.germancoding.rudp.ReliableServerSocket;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.almi.pgs.germancoding.rudp.ReliableSocketListener;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Almi on 2016-12-09.
 *
 * Game server logic.
 * Checking if server can be started on given port,
 * starting server,
 * incoming messages
 */
public class GameServer implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(GameServer.class);
    private final static int BUFFER_SIZE = 512;

    /**
     * Max number of players
     */
    private int maxPlayersNum = 4;
    private AtomicInteger playersCount = new AtomicInteger(0);
    private List<PlayerOnServerThread> playersList = new ArrayList<>();
    private ReliableSocket clientSocket;

    public GameServer() {

    }

    public GameServer(int playersCount) {
        this.maxPlayersNum = playersCount;
    }

    @Override
    public void run() {
        log.info("Starting server...");
        try {
            checkServerRunning();
            ReliableServerSocket socket = new ReliableServerSocket(Constants.PORT);
            for(int i = 0; i < maxPlayersNum; ++i) {
                new PlayerThread(socket, i).start();
            }
            while(true){}
        } catch (Exception e) {
            log.error(e.getMessage()+". Closing...");
        }

    }

    private void checkServerRunning() throws Exception {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(Constants.PORT);
        } catch(Exception e) {
            throw new Exception("Server already running on port " + Constants.PORT);
        } finally {
            if(socket != null) {
                socket.close();
            }
        }
    }

    private class SocketListener implements ReliableSocketListener {

        @Override
        public void packetSent() {
            System.out.println("Lolz?");
        }

        @Override
        public void packetRetransmitted() {

            log.info("retransmitted");
        }

        @Override
        public void packetReceivedInOrder() {
            log.info("In order");
        }

        @Override
        public void packetReceivedOutOfOrder() {
            log.info("Out of order");
        }

    }

    class PlayerThread extends Thread {
        private ReliableServerSocket socket;
        private final Object lock = new Object();
        private int pid;

        public PlayerThread(ReliableServerSocket socket, int playerID) {
            this.socket = socket;
            this.pid = playerID;
            setDaemon(true);
        }

        @Override
        public void run() {

            try {
                clientSocket = (ReliableSocket) socket.accept();
                InetSocketAddress clientAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
                clientSocket.addListener(new SocketListener());

                log.info("New Connection from " + clientAddress.getHostName() + ":" + clientAddress.getPort() + " Processing...");

                InputStream is = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];

                synchronized (lock) {
                    while ((is.read(buffer)) > 0) {
                        String val = new String(buffer);
                        String[] arr = val.split("}");
                        System.out.println(val);
                        System.out.println(arr[0]);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
