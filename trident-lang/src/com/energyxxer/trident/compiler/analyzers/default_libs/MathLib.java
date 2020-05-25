package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Math")
public class MathLib implements DefaultLibraryProvider {
    private static final double LN_2 = Math.log(2);

    @SuppressWarnings("unchecked")
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass math = new CustomClass("Math", "trident-util:native", globalCtx);
        math.setConstructor(Symbol.SymbolVisibility.PRIVATE, null);
        globalCtx.put(new Symbol("Math", Symbol.SymbolVisibility.GLOBAL, math));

        try {
            math.putStaticFunction(nativeMethodsToFunction(globalCtx, Math.class.getMethod("pow", double.class, double.class)));
            math.putStaticFunction(nativeMethodsToFunction(globalCtx,
                    Math.class.getMethod("min", int.class, int.class),
                    Math.class.getMethod("min", double.class, double.class)
            ));
            math.putStaticFunction(nativeMethodsToFunction(globalCtx,
                    Math.class.getMethod("max", int.class, int.class),
                    Math.class.getMethod("max", double.class, double.class)
            ));
            math.putStaticFunction(nativeMethodsToFunction(globalCtx,
                    Math.class.getMethod("abs", int.class),
                    Math.class.getMethod("abs", double.class)
            ));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("floor", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("ceil", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("round", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("floorDiv", int.class, int.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("floorMod", int.class, int.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("signum", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("sin", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("cos", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("tan", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("sinh", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("cosh", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("tanh", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("asin", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("acos", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("atan", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("atan2", double.class, double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("log", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("log10", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), MathLib.class.getMethod("log2", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("toRadians", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("toDegrees", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("sqrt", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("cbrt", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(), Math.class.getMethod("exp", double.class)));
            math.putStaticFunction(nativeMethodsToFunction(math.getInnerStaticContext(),
                    Math.class.getMethod("random"),
                    MathLib.class.getMethod("random", double.class, double.class)
            ));

            math.putStaticFinalMember("PI", Math.PI);
            math.putStaticFinalMember("E", Math.E);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        CustomClass integer = new CustomClass("Integer", "trident-util:native", globalCtx);
        integer.setConstructor(Symbol.SymbolVisibility.PRIVATE, null);
        globalCtx.put(new Symbol("Integer", Symbol.SymbolVisibility.GLOBAL, integer));

        try {
            integer.putStaticFinalMember("MIN_VALUE", Integer.MIN_VALUE);
            integer.putStaticFinalMember("MAX_VALUE", Integer.MAX_VALUE);
            integer.putStaticFunction(nativeMethodsToFunction(integer.getInnerStaticContext(),
                    Integer.class.getMethod("parseInt", String.class),
                    Integer.class.getMethod("parseInt", String.class, int.class)
            ));
            integer.putStaticFunction(nativeMethodsToFunction(integer.getInnerStaticContext(),
                    Integer.class.getMethod("parseUnsignedInt", String.class),
                    Integer.class.getMethod("parseUnsignedInt", String.class, int.class)
            ));
            integer.putStaticFunction(nativeMethodsToFunction(integer.getInnerStaticContext(),
                    Integer.class.getMethod("toString", int.class),
                    Integer.class.getMethod("toString", int.class, int.class)
            ));
            integer.putStaticFunction(nativeMethodsToFunction(integer.getInnerStaticContext(),
                    Integer.class.getMethod("toUnsignedString", int.class),
                    Integer.class.getMethod("toUnsignedString", int.class, int.class)
            ));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }

        CustomClass real = new CustomClass("Real", "trident-util:native", globalCtx);
        real.setConstructor(Symbol.SymbolVisibility.PRIVATE, null);
        globalCtx.put(new Symbol("Real", Symbol.SymbolVisibility.GLOBAL, real));

        try {
            real.putStaticFinalMember("MIN_VALUE", -Double.MAX_VALUE);
            real.putStaticFinalMember("MAX_VALUE", Double.MAX_VALUE);
            real.putStaticFinalMember("MIN_POSITIVE_VALUE", Double.MIN_VALUE);
            real.putStaticFinalMember("Infinity", Double.POSITIVE_INFINITY);
            real.putStaticFinalMember("NaN", Double.NaN);
            real.putStaticFunction(nativeMethodsToFunction(real.getInnerStaticContext(), "parseReal", Double.class.getMethod("parseDouble", String.class)));
            real.putStaticFunction(nativeMethodsToFunction(real.getInnerStaticContext(), Double.class.getMethod("isFinite", double.class)));
            real.putStaticFunction(nativeMethodsToFunction(real.getInnerStaticContext(), Double.class.getMethod("isInfinite", double.class)));
            real.putStaticFunction(nativeMethodsToFunction(real.getInnerStaticContext(), Double.class.getMethod("isNaN", double.class)));
            real.putStaticFunction(nativeMethodsToFunction(real.getInnerStaticContext(), Double.class.getMethod("toString", double.class)));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static double log2(double a) {
        return Math.log(a) / LN_2;
    }

    public static double random(double min, double max) {
        return min + Math.random() * (max-min);
    }
}
