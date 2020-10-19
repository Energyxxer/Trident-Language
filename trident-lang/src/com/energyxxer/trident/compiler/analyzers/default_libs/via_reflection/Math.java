package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

public class Math {
    private static final double LN_2 = java.lang.Math.log(2);
    public static final double PI = java.lang.Math.PI;
    public static final double E = java.lang.Math.E;

    public static double pow(double a, double b) {
        return java.lang.Math.pow(a, b);
    }
    public static int min(int a, int b) {
        return java.lang.Math.min(a, b);
    }
    public static double min(double a, double b) {
        return java.lang.Math.min(a, b);
    }
    public static int max(int a, int b) {
        return java.lang.Math.max(a, b);
    }
    public static double max(double a, double b) {
        return java.lang.Math.max(a, b);
    }
    public static int abs(int x) {
        return java.lang.Math.abs(x);
    }
    public static double abs(double x) {
        return java.lang.Math.abs(x);
    }
    public static double floor(double x) {
        return java.lang.Math.floor(x);
    }
    public static double ceil(double x) {
        return java.lang.Math.ceil(x);
    }
    public static double round(double x) {
        return java.lang.Math.round(x);
    }
    public static int floorDiv(int x, int y) {
        return java.lang.Math.floorDiv(x, y);
    }
    public static int floorMod(int x, int y) {
        return java.lang.Math.floorMod(x, y);
    }
    public static double signum(double x) {
        return java.lang.Math.signum(x);
    }

    public static double sin(double angrad) {
        return java.lang.Math.sin(angrad);
    }
    public static double cos(double angrad) {
        return java.lang.Math.cos(angrad);
    }
    public static double tan(double angrad) {
        return java.lang.Math.tan(angrad);
    }
    public static double sinh(double angrad) {
        return java.lang.Math.sinh(angrad);
    }
    public static double cosh(double angrad) {
        return java.lang.Math.cosh(angrad);
    }
    public static double tanh(double angrad) {
        return java.lang.Math.tanh(angrad);
    }
    public static double asin(double angrad) {
        return java.lang.Math.asin(angrad);
    }
    public static double acos(double angrad) {
        return java.lang.Math.acos(angrad);
    }
    public static double atan(double angrad) {
        return java.lang.Math.atan(angrad);
    }
    public static double atan2(double y, double x) {
        return java.lang.Math.atan2(y, x);
    }

    public static double log(double a) {
        return java.lang.Math.log(a);
    }
    public static double log10(double a) {
        return java.lang.Math.log10(a);
    }
    public static double log2(double a) {
        return java.lang.Math.log(a) / LN_2;
    }

    public static double toRadians(double angdeg) {
        return java.lang.Math.toRadians(angdeg);
    }
    public static double toDegrees(double angrad) {
        return java.lang.Math.toDegrees(angrad);
    }

    public static double sqrt(double a) {
        return java.lang.Math.sqrt(a);
    }
    public static double cbrt(double a) {
        return java.lang.Math.cbrt(a);
    }

    public static double exp(double a) {
        return java.lang.Math.exp(a);
    }

    public static double random() {
        return java.lang.Math.random();
    }
    public static double random(double min, double max) {
        return min + java.lang.Math.random() * (max-min);
    }

}
