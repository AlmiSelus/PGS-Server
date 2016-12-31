package com.almi.pgs.client;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

/**
 * Created by Almi on 2016-12-09.
 *
 */
public class GameClientRunner {

    private final static Logger log = LoggerFactory.getLogger(GameClientRunner.class);

    public static void main(String[] args) {
        GameClientRunner runner = new GameClientRunner();
        runner.launch(args);
    }

    public void launch(String[] args) {
        try {
            GameClient gameClient = new GameClient(args);
            gameClient.start();
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }



}
