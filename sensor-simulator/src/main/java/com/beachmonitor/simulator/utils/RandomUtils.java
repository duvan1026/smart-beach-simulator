package com.beachmonitor.simulator.utils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

    public static double randomInRange(double min, double max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static double randomGaussian(double mean, double sigma) {
        return mean + ThreadLocalRandom.current().nextGaussian() * sigma;
    }
}
