package com.almi.pgs.server;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.PacketManager;
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
            List<ReliableSocket> socketsList = new CopyOnWriteArrayList<>();
            GameState gameState = new GameState(socketsList, packetManager);
            ExecutorService executorService = Executors.newCachedThreadPool();
            ReliableServerSocket socket = new ReliableServerSocket(Constants.PORT);

            for (int i = 0; i < maxPlayersNum; ++i) {
                executorService.execute(() -> {
                    try {
                        ReliableSocket clientSocket = (ReliableSocket) socket.accept();
                        clientSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                        clientSocket.setSendBufferSize(SEND_BUFFER_SIZE);
                        socketsList.add(clientSocket);
                        log.info("Player log");
                        new PlayerThread(new PacketManager(), clientSocket, socketsList).start();
                    } catch (Exception e) {
                        log.info(ExceptionUtils.getStackTrace(e));
                    }
                });
            }
            log.info("Test!!");
            while(true){
                try{
                    gameState.tick();
                    Thread.sleep(1000);
                }catch(Exception e) {
                    log.info(ExceptionUtils.getStackTrace(e));
                }
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

    private class GameState {
        private final List<ReliableSocket>  clients;
        private final PacketManager         packetManager;
        private com.almi.pgs.game.packets.GameState packet;
        private byte remainingTime = 120;
        private byte pointsBlue = 0;
        private byte pointsRed = 0;

        private GameState(List<ReliableSocket> clientSockets, PacketManager pm) {
            clients = clientSockets;
            packetManager = pm;
        }

        private synchronized void tick() {
            packet = new com.almi.pgs.game.packets.GameState();
            toGameStatePacket(packet, this);
            for(ReliableSocket client : clients) {
                packetManager.sendPacket(client, packet);
            }
            remainingTime--;
        }

        private synchronized void toGameStatePacket(com.almi.pgs.game.packets.GameState packet, GameState gameState) {
            /**
             * Serialize here
             */
            packet.setRemainingTime(gameState.remainingTime);
            packet.setPointsBlue(gameState.pointsBlue);
            packet.setPointsRed(gameState.pointsRed);
        }
    }



}
