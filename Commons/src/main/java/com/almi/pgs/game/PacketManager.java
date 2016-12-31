package com.almi.pgs.game;

import com.almi.pgs.commons.Utils;
import com.almi.pgs.game.packets.*;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.almi.pgs.commons.Constants.SILENT_MODE;

/**
 * Created by Almi on 2016-12-31.
 */
public class PacketManager {
    private final static Logger log = LoggerFactory.getLogger(PacketManager.class);
    private Gson gson;
    private List<PacketListener> packetListeners = new ArrayList<>();
    private final TypeToken<Packet> packetTypeToken = new TypeToken<Packet>() {};
    private final List<Class<? extends Packet>> packetTypes = new ArrayList<>();

    public PacketManager() {
        this(new Class[] {
                AuthPacket.class,
                AuthResponsePacket.class,
                GamePacket.class,
                GenericResponse.class
        });
    }

    public PacketManager(Class<? extends Packet>[] packetTypes) {


        for(Class<? extends Packet> packetType : packetTypes) {
            this.packetTypes.add(packetType);
        }

        createPacketTypeAdapter();
    }

    public void handlePacket(Packet packet) {
        for (PacketListener packetListener : packetListeners) {
            if(packetListener.packetClass() == packet.getClass()) {
                packetListener.handlePacket(packet);
                break;
            }
        }
    }

    public void registerNewPacketType(Class<? extends Packet> packetClass) {
        if(!packetTypes.contains(packetClass)) {
            packetTypes.add(packetClass);
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

    public Packet getGeneralGamePacket(String stringified) {
        try {
            String receivedPacketString = Utils.stripJson(stringified);
            if(!SILENT_MODE) {
                log.info("Received Packet = " + receivedPacketString);
            }
            return gson.fromJson(receivedPacketString, packetTypeToken.getType());
        } catch (Exception e) {
//            log.info(ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public void sendPacket(ReliableSocket clientSocket, Packet packet) {
        try {
            OutputStream os = clientSocket.getOutputStream();
            String packetString = gson.toJson(packet, packetTypeToken.getType());
            os.write(packetString.getBytes());
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

    private void createPacketTypeAdapter() {
        final RuntimeTypeAdapterFactory<Packet> packetTypeFactory = RuntimeTypeAdapterFactory
                .of(Packet.class, "type");
        for (Class<? extends Packet> packetClass : packetTypes) {
//            this.packetTypes.add(packetClass);
            packetTypeFactory.registerSubtype(packetClass);
        }
        gson = new GsonBuilder().registerTypeAdapterFactory(packetTypeFactory).create();

    }
}
