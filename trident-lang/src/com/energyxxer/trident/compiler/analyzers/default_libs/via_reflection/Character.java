package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

public class Character {

    public static String fromCodePoint(int c) {
        return java.lang.Character.toString((char)c);
    }

    public static int toCodePoint(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return s.charAt(0);
    }

    public static String getName(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return java.lang.Character.getName(s.charAt(0));
    }

    public static boolean isLetter(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return java.lang.Character.isLetter(s.charAt(0));
    }

    public static boolean isDigit(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return java.lang.Character.isDigit(s.charAt(0));
    }

    public static boolean isWhitespace(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return java.lang.Character.isWhitespace(s.charAt(0));
    }

    public static boolean isUpperCase(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return java.lang.Character.isUpperCase(s.charAt(0));
    }

    public static boolean isLowerCase(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return java.lang.Character.isLowerCase(s.charAt(0));
    }
}
