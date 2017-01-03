package com.almi.pgs.server.authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Almi on 2016-12-30.
 */
public class AuthenticationLocalDatabase {

    private List<Player> userPasswordDB = new ArrayList<>();

    public AuthenticationLocalDatabase() {
        userPasswordDB.add(new Player((byte) 0, "user1", "p1"));
        userPasswordDB.add(new Player((byte) 1, "user2", "p2"));
        userPasswordDB.add(new Player((byte) 2, "user3", "p3"));
        userPasswordDB.add(new Player((byte) 3, "user4", "p4"));
    }

    public Optional<Player> getPlayer(String login) {
        return userPasswordDB.stream().filter(player -> player.getLogin().equals(login)).findFirst();
    }
}
