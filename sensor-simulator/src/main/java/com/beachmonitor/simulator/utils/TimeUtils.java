package com.beachmonitor.simulator.utils;

import java.time.Instant;

public class TimeUtils {

    public static String nowIso() {
        return Instant.now().toString();
    }
}
