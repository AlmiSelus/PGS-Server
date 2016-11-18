package com.almi.pgs.server;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by Almi on 2016-10-10.
 */
public class FPSServer implements Runnable {

    private final static int PORT = 20039;
    private final static Logger log = LoggerFactory.getLogger(FPSServer.class);
    private final static int PACKET_LENGTH = 1024;

    public static void main(String[] args) {
        FPSServer server = new FPSServer();
        server.run();
    }

    public void run() {
        try {
            System.out.println("Server listening on port " + PORT);
            DatagramSocket serverSocket = new DatagramSocket(PORT);
            byte[] receiveBuffer = new byte[PACKET_LENGTH];
            while(true) {
                DatagramPacket receivedPacket = new DatagramPacket(receiveBuffer, PACKET_LENGTH);
                serverSocket.receive(receivedPacket);
                String sentence = new String(receiveBuffer);
                System.out.println(sentence);
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }

    }
}
