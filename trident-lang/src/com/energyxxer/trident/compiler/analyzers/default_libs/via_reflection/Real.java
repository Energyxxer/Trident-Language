package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

public class Real {
    public static final double MIN_VALUE = -Double.MAX_VALUE;
    public static final double MAX_VALUE = Double.MAX_VALUE;
    public static final double MIN_POSITIVE_VALUE = Double.MIN_VALUE;
    public static final double Infinity = Double.POSITIVE_INFINITY;
    public static final double NaN = Double.NaN;

    public static double parseReal(String s) {
        return Double.parseDouble(s);
    }
    public static boolean isFinite(double r) {
        return Double.isFinite(r);
    }
    public static boolean isInfinite(double r) {
        return Double.isInfinite(r);
    }
    public static boolean isNaN(double r) {
        return Double.isNaN(r);
    }
    public static String toString(double r) {
        return Double.toString(r);
    }
    public static String toHexString(double r) {
        return Double.toHexString(r);
    }
}
