package com.almi.pgs.server.authentication;

/**
 * Created by Almi on 2016-12-30.
 */
public interface AuthenticationListener {
    void authenticationDataObtained();
    void authenticationPassed();
    void authenticationFailed();
}
