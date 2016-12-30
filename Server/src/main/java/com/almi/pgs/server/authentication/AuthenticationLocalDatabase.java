package com.almi.pgs.server.authentication;

import sun.security.rsa.RSASignature;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Almi on 2016-12-30.
 */
public class AuthenticationLocalDatabase {

    private Map<String, String> userPasswordDB = new HashMap<>();

    public AuthenticationLocalDatabase() {
        userPasswordDB.put("user1", "");
    }

}
