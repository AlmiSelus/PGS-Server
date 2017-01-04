package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.GameState;
import com.almi.pgs.germancoding.rudp.ReliableServerSocket;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almi.pgs.commons.Constants.RECEIVE_BUFFER_SIZE;
import static com.almi.pgs.commons.Constants.SEND_BUFFER_SIZE;
import static java.lang.Thread.sleep;

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

    /**
     * Packet manager
     */
    private PacketManager packetManager = new PacketManager();

    private AtomicInteger playerCountr = new AtomicInteger(0);

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
            List<PlayerThread> socketsList = new CopyOnWriteArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool(maxPlayersNum);
            ReliableServerSocket socket = new ReliableServerSocket(Constants.PORT);
            new Thread(() -> {
                Game gameState = new Game(socketsList, packetManager);
                while(true) {
                    try {
                        gameState.tick();
                        Thread.sleep(Constants.TICK_TIME);
                    } catch (Exception e) {
                        log.info(ExceptionUtils.getStackTrace(e));
                    }
                }
            }).start();

            while(true){
                ReliableSocket clientSocket = (ReliableSocket) socket.accept();
                executorService.execute(() -> {
                    try {
                        playerCountr.incrementAndGet();
                        clientSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                        clientSocket.setSendBufferSize(SEND_BUFFER_SIZE);
                        PlayerThread playerThread = new PlayerThread(new PacketManager(), clientSocket, socketsList, playerCountr.get() %2);
                        socketsList.add(playerThread);
                        playerThread.start();
                    } catch (Exception e) {
                        log.info(ExceptionUtils.getStackTrace(e));
                    }
                });
            }
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
