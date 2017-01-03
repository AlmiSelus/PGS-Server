package com.almi.pgs.game.packets;

/**
 * Created by Almi on 2016-12-31.
 * TLV packet structure:
 *  Type, Length, Value
 *
 *  Type is represented by packet concrete class,
 *  Length is common for all packets, hence set up here,
 *  Value depends on concrete packet class
 *
 */
public interface Packet {
}
