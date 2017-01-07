package com.almi.pgs.commons;

/**
 * Created by Almi on 2016-12-09.
 *
 * Constant values for entire application.
 */
public class Constants {
    public final static int PORT = 30090;
    public final static int BUFFER_SIZE = (int) Math.pow(2, 16) - 1; //65535
    public final static int RECEIVE_BUFFER_SIZE = 1024 * 64; //64kB buffer size
    public final static int SEND_BUFFER_SIZE    = 1024 * 64; //64kB buffer size
    public final static String LOGIN_PWD_SALT = "dJaF<9!=m3]o,7a&h3YE`uxdVIN(P[9:lkN37q3>fM2~844t#e8EM?{.$NE1#P5";
    public final static boolean SILENT_MODE = true;
	public final static double MAX_PLAYER_SPEED = 0.01;
    public final static int ROUND_TIME = 120 * 100; //seconds
    public final static long TICK_TIME = 1000/100;
}
