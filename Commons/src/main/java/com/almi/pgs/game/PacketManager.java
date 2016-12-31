package com.almi.pgs.game;

import com.almi.pgs.commons.Utils;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.google.gson.Gson;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Almi on 2016-12-31.
 */
public class PacketManager {
    private final static Logger log = LoggerFactory.getLogger(PacketManager.class);
    private Gson gson = new Gson();
    private List<PacketListener> packetListeners = new ArrayList<>();

    public void handlePacket(GeneralGamePacket generalPacket) {
        Optional<Packet> optionalPacket = Optional.empty();

        if(generalPacket.getAuthPacket() != null) {
            optionalPacket = Optional.of(generalPacket.getAuthPacket());
        } else if(generalPacket.getGamePacket() != null) {
            optionalPacket = Optional.of(generalPacket.getGamePacket());
        } else if(generalPacket.getGenericPacket() != null) {
            optionalPacket = Optional.of(generalPacket.getGenericPacket());
        }

        if(optionalPacket.isPresent()) {
            Packet packet = optionalPacket.get();
            for (PacketListener packetListener : packetListeners) {
                if(packetListener.packetClass() == packet.getClass()) {
                    log.info("Selected for class " + packetListener.getClass().getSimpleName());
                    packetListener.handlePacket(packet);
                    break;
                }
            }
        }
    }

    /**
     * Adds packet listener for given type of packet if it is not already in manager
     * @param packetListener
     */
    public void addPacketListener(PacketListener packetListener) {
        if(packetListeners.stream().filter(listener->listener.packetClass() == packetListener.packetClass()).count() == 0) {
            log.info("Added listener for " + packetListener.packetClass().getSimpleName());
            this.packetListeners.add(packetListener);
        }
    }

    public GeneralGamePacket getGeneralGamePacket(String stringified) {
        try {
            String receivedPacketString = Utils.stripJson(stringified);
            log.info("Received Packet = " + receivedPacketString);
            return gson.fromJson(receivedPacketString, GeneralGamePacket.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void sendPacket(ReliableSocket clientSocket, Packet packet) {
        try {
            GeneralGamePacket generalGamePacket = new GeneralGamePacket();
            if(packet.getClass() == AuthPacket.class) {
                generalGamePacket.setAuthPacket((AuthPacket) packet);
            } else if(packet.getClass() == GamePacket.class) {
                generalGamePacket.setGamePacket((GamePacket) packet);
            } else if(packet.getClass() == GenericResponse.class) {
                generalGamePacket.setGenericPacket((GenericResponse) packet);
            }

            OutputStream os = clientSocket.getOutputStream();
            os.write(gson.toJson(generalGamePacket).getBytes());
            os.flush();
        } catch(Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public void removePacketListener(int i) {
        if(i < packetListeners.size()) {
            packetListeners.remove(i);
        }
    }
}
