package com.beachmonitor.simulator.utils;

public class SensorBehaviorUtils {

    /**
     * Genera el siguiente valor de un sensor basado en su valor previo.
     * La lectura fluctua dentro de una variacion permitida y respeta un rango.
     */
    public static double nextSensorValue(double previousValue,
                                         double variationMin, double variationMax,
                                         double allowedMin, double allowedMax) {

        double variation = RandomUtils.randomInRange(variationMin, variationMax);
        boolean increase = RandomUtils.randomBoolean();

        double newValue = increase
                ? previousValue + variation
                : previousValue - variation;

        if (newValue < allowedMin) newValue = allowedMin;
        if (newValue > allowedMax) newValue = allowedMax;

        return NumberUtils.roundToTwoDecimals(newValue);
    }

    /**
     * Calcula un valor correlacionado basado en un valor de referencia,
     * aplicando atenuacion por distancia y ruido gaussiano.
     */
    public static double correlatedValue(double baseValue,
                                         double distanceMeters,
                                         double noiseSigma,
                                         double min,
                                         double max) {

        double attenuation = Math.exp(-distanceMeters / 100000.0);
        double noise = RandomUtils.randomGaussian(0, noiseSigma);
        double newValue = baseValue * attenuation + noise;

        if (newValue < min) newValue = min;
        if (newValue > max) newValue = max;

        return NumberUtils.roundToTwoDecimals(newValue);
    }

    /**
     * Genera un valor inicial dentro del rango permitido.
     */
    public static double initialValue(double min, double max) {
        return NumberUtils.roundToTwoDecimals(RandomUtils.randomInRange(min, max));
    }
}
