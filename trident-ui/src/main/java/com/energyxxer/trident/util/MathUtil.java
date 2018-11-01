package com.energyxxer.trident.util;

/**
 * Created by User on 2/22/2017.
 */
public class MathUtil {
    public static double truncateDecimals(double n, int precission) {
        return Math.floor(n * Math.pow(10, precission)) / (Math.pow(10, precission));
    }
}
