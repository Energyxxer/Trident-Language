package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

public class Integer {
    public static final int MIN_VALUE = java.lang.Integer.MIN_VALUE;
    public static final int MAX_VALUE = java.lang.Integer.MAX_VALUE;

    public static int parseInt(String s) {
        return java.lang.Integer.parseInt(s);
    }
    public static int parseInt(String s, int radix) {
        return java.lang.Integer.parseInt(s, radix);
    }
    public static int parseUnsignedInt(String s) {
        return java.lang.Integer.parseUnsignedInt(s);
    }
    public static int parseUnsignedInt(String s, int radix) {
        return java.lang.Integer.parseUnsignedInt(s, radix);
    }
    public static String toString(int i) {
        return java.lang.Integer.toString(i);
    }
    public static String toString(int i, int radix) {
        return java.lang.Integer.toString(i, radix);
    }
    public static String toUnsignedString(int i) {
        return java.lang.Integer.toUnsignedString(i);
    }
    public static String toUnsignedString(int i, int radix) {
        return java.lang.Integer.toUnsignedString(i, radix);
    }
}
