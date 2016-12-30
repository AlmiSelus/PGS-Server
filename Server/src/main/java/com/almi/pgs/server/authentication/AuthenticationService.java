package com.almi.pgs.server.authentication;

/**
 * Created by Almi on 2016-12-30.
 */
public class AuthenticationService extends Thread {

    private AuthenticationListener authListener;

    public void setListener(AuthenticationListener listener) {
        this.authListener = listener;
    }

    @Override
    public void run() {

    }
}
