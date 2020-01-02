package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "Character")
public class CharacterLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        DictionaryObject clib = new DictionaryObject();
        try {
            clib.put("fromCodePoint", new MethodWrapper<>("fromCodePoint", CharacterLib.class.getMethod("fromCodePoint", int.class)).createForInstance(null));
            clib.put("toCodePoint", new MethodWrapper<>("toCodePoint", CharacterLib.class.getMethod("toCodePoint", String.class)).createForInstance(null));
            clib.put("getName", new MethodWrapper<>("getName", CharacterLib.class.getMethod("getName", String.class)).createForInstance(null));
            clib.put("isLetter", new MethodWrapper<>("isLetter", CharacterLib.class.getMethod("isLetter", String.class)).createForInstance(null));
            clib.put("isDigit", new MethodWrapper<>("isDigit", CharacterLib.class.getMethod("isDigit", String.class)).createForInstance(null));
            clib.put("isWhitespace", new MethodWrapper<>("isWhitespace", CharacterLib.class.getMethod("isWhitespace", String.class)).createForInstance(null));
            clib.put("isUpperCase", new MethodWrapper<>("isUpperCase", CharacterLib.class.getMethod("isUpperCase", String.class)).createForInstance(null));
            clib.put("isLowerCase", new MethodWrapper<>("isLowerCase", CharacterLib.class.getMethod("isLowerCase", String.class)).createForInstance(null));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        globalCtx.put(new Symbol("Character", Symbol.SymbolVisibility.GLOBAL, clib));
    }

    public static String fromCodePoint(int c) {
        return Character.toString((char)c);
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
        return Character.getName(s.charAt(0));
    }

    public static boolean isLetter(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return Character.isLetter(s.charAt(0));
    }

    public static boolean isDigit(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return Character.isDigit(s.charAt(0));
    }

    public static boolean isWhitespace(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return Character.isWhitespace(s.charAt(0));
    }

    public static boolean isUpperCase(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return Character.isUpperCase(s.charAt(0));
    }

    public static boolean isLowerCase(String s) {
        if(s.isEmpty()) {
            throw new IllegalArgumentException("Empty string");
        }
        return Character.isLowerCase(s.charAt(0));
    }
}
