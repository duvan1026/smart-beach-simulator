package com.beachmonitor.simulator.utils;

public class NumberUtils {

    public static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
