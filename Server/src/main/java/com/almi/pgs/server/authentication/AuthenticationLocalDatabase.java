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
        userPasswordDB.add(new Player("user1", "password1"));
    }

    public Optional<Player> getPlayer(String login) {
        return userPasswordDB.stream().filter(player -> player.getLogin().equals(login)).findFirst();
    }
}
