package com.almi.pgs.server;

import java.util.Optional;

import static com.almi.pgs.commons.Utils.getArgFor;

/**
 * Created by Almi on 2016-12-09.
 */
public class GameServerRunner {

    public static void main(String[] args) {
        GameServerRunner runner = new GameServerRunner();
        runner.launch(args);
    }

    public void launch(String[] args) {
        GameServer gameServer = new GameServer();
        if(args.length > 0) {
            Optional<String> maxPlayersArgValue = getArgFor(args, "-maxplayers");
            if(maxPlayersArgValue.isPresent()) {
                gameServer = new GameServer(Integer.parseInt(maxPlayersArgValue.get()));
            }
        }
        gameServer.run();
    }

}
