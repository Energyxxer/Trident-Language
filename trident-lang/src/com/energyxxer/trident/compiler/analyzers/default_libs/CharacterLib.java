package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Character")
public class CharacterLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass clib = new CustomClass("Character", "trident-util:native", globalCtx);
        clib.seal();
        try {
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("fromCodePoint", int.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("toCodePoint", String.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("getName", String.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("isLetter", String.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("isDigit", String.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("isWhitespace", String.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("isUpperCase", String.class)));
            clib.putStaticFunction(nativeMethodsToFunction(clib.getInnerStaticContext(), CharacterLib.class.getMethod("isLowerCase", String.class)));
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
