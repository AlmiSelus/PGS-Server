package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.germancoding.rudp.ReliableServerSocket;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Almi on 2016-12-09.
 *
 * Game server logic.
 * Checking if server can be started on given port,
 * starting server
 */
public class GameServer implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(GameServer.class);

    /**
     * Max number of players
     */
    private int maxPlayersNum = 4;

    public GameServer() {

    }

    public GameServer(int playersCount) {
        this.maxPlayersNum = playersCount;
    }

    @Override
    public void run() {
        log.info("Starting server...");
        log.info("Server listening on port " + Constants.PORT);
        log.info("Max players set to " + maxPlayersNum);
        try {
            checkServerRunning();

            ReliableServerSocket socket = new ReliableServerSocket(Constants.PORT);
            for(int i = 0; i < maxPlayersNum; ++i) {
                ReliableSocket clientSocket = (ReliableSocket) socket.accept();
                new PlayerThread(clientSocket, i, Collections.synchronizedList(new ArrayList<>())).start();
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

}
