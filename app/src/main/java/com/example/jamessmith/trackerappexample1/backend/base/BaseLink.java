package com.example.jamessmith.trackerappexample1.backend.base;

import org.jetbrains.annotations.Contract;

/**
 * Created by James Smith on 10/02/2018.
 */

public class BaseLink {

    private static final String googleBaseLink = "http://maps.googleapis.com";

    @Contract(pure = true)
    public static String getGoogleBaseLink() {
        return googleBaseLink;
    }
}
