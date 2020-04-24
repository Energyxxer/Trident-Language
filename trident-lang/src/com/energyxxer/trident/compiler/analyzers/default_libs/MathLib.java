package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "Math")
public class MathLib implements DefaultLibraryProvider {
    private static final double LN_2 = Math.log(2);

    @SuppressWarnings("unchecked")
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        DictionaryObject math = new DictionaryObject();
        math.put("pow", (TridentMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'pow' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            return Math.pow(base.doubleValue(), exponent.doubleValue());
        });
        math.put("min", (TridentMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'min' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            double result = Math.min(base.doubleValue(), exponent.doubleValue());

            if(params[0] instanceof Double || params[1] instanceof Double) return result;
            else return (int) result;
        });
        math.put("max", (TridentMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'max' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            double result = Math.max(base.doubleValue(), exponent.doubleValue());

            if(params[0] instanceof Double || params[1] instanceof Double) return result;
            else return (int) result;
        });
        math.put("abs", (TridentMethod) (params, patterns, pattern, file) -> {
            if(params.length < 1) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'max' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number num = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);

            double result = Math.abs(num.doubleValue());

            if(params[0] instanceof Double) return result;
            else return (int) result;
        });
        try {
            math.put("floor", new MethodWrapper<>(Math.class.getMethod("floor", double.class)).createForInstance(null));
            math.put("ceil", new MethodWrapper<>(Math.class.getMethod("ceil", double.class)).createForInstance(null));
            math.put("round", new MethodWrapper<>("round", (instance, params) -> (double)Math.round((double)params[0]), double.class).createForInstance(null));
            math.put("floorMod", new MethodWrapper<>(Math.class.getMethod("floorMod", int.class, int.class)).createForInstance(null));
            math.put("floorDiv", new MethodWrapper<>(Math.class.getMethod("floorDiv", int.class, int.class)).createForInstance(null));
            math.put("signum", new MethodWrapper<>(Math.class.getMethod("signum", double.class)).createForInstance(null));
            math.put("sin", new MethodWrapper<>(Math.class.getMethod("sin", double.class)).createForInstance(null));
            math.put("cos", new MethodWrapper<>(Math.class.getMethod("cos", double.class)).createForInstance(null));
            math.put("tan", new MethodWrapper<>(Math.class.getMethod("tan", double.class)).createForInstance(null));
            math.put("sinh", new MethodWrapper<>(Math.class.getMethod("sinh", double.class)).createForInstance(null));
            math.put("cosh", new MethodWrapper<>(Math.class.getMethod("cosh", double.class)).createForInstance(null));
            math.put("tanh", new MethodWrapper<>(Math.class.getMethod("tanh", double.class)).createForInstance(null));
            math.put("asin", new MethodWrapper<>(Math.class.getMethod("asin", double.class)).createForInstance(null));
            math.put("acos", new MethodWrapper<>(Math.class.getMethod("acos", double.class)).createForInstance(null));
            math.put("atan", new MethodWrapper<>(Math.class.getMethod("atan", double.class)).createForInstance(null));
            math.put("atan2", new MethodWrapper<>(Math.class.getMethod("atan2", double.class, double.class)).createForInstance(null));
            math.put("log", new MethodWrapper<>(Math.class.getMethod("log", double.class)).createForInstance(null));
            math.put("log10", new MethodWrapper<>(Math.class.getMethod("log10", double.class)).createForInstance(null));
            math.put("log2", new MethodWrapper<>("log2", (instance, params) -> Math.log((double)params[0]) / LN_2, double.class).createForInstance(null));
            math.put("toRadians", new MethodWrapper<>(Math.class.getMethod("toRadians", double.class)).createForInstance(null));
            math.put("toDegrees", new MethodWrapper<>(Math.class.getMethod("toDegrees", double.class)).createForInstance(null));
            math.put("sqrt", new MethodWrapper<>(Math.class.getMethod("sqrt", double.class)).createForInstance(null));
            math.put("cbrt", new MethodWrapper<>(Math.class.getMethod("cbrt", double.class)).createForInstance(null));
            math.put("exp", new MethodWrapper<>(Math.class.getMethod("exp", double.class)).createForInstance(null));
            math.put("random", new MethodWrapper<>(Math.class.getMethod("random")).createForInstance(null));

            math.put("PI", Math.PI);
            math.put("E", Math.E);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        globalCtx.put(new Symbol("Math", Symbol.SymbolVisibility.GLOBAL, math));

        DictionaryObject integer = new DictionaryObject();
        integer.put("MIN_VALUE", Integer.MIN_VALUE);
        integer.put("MAX_VALUE", Integer.MAX_VALUE);
        integer.put("parseInt", new MethodWrapper<>("parseInt", (instance, params) -> {
            if (params[1] == null) {
                return Integer.parseInt(((String) params[0]));
            } else {
                return Integer.parseInt(((String) params[0]), ((int) params[1]));
            }
        }, String.class, int.class).setNullable(1).createForInstance(null));
        integer.put("parseUnsignedInt", new MethodWrapper<>("parseUnsignedInt", (instance, params) -> {
            if (params[1] == null) {
                return Integer.parseUnsignedInt(((String) params[0]));
            } else {
                return Integer.parseUnsignedInt(((String) params[0]), ((int) params[1]));
            }
        }, String.class, int.class).setNullable(1).createForInstance(null));
        integer.put("toString", new MethodWrapper<>("toString", (instance, params) -> {
            if (params[1] == null) {
                return Integer.toString(((int) params[0]));
            } else {
                return Integer.toString(((int) params[0]), ((int) params[1]));
            }
        }, int.class, int.class).setNullable(1).createForInstance(null));
        integer.put("toUnsignedString", new MethodWrapper<>("toUnsignedString", (instance, params) -> {
            if (params[1] == null) {
                return Integer.toUnsignedString(((int) params[0]));
            } else {
                return Integer.toUnsignedString(((int) params[0]), ((int) params[1]));
            }
        }, int.class, int.class).setNullable(1).createForInstance(null));
        globalCtx.put(new Symbol("Integer", Symbol.SymbolVisibility.GLOBAL, integer));

        DictionaryObject real = new DictionaryObject();
        real.put("MIN_VALUE", -Double.MAX_VALUE);
        real.put("MAX_VALUE", Double.MAX_VALUE);
        real.put("MIN_POSITIVE_VALUE", Double.MIN_VALUE);
        real.put("Infinity", Double.POSITIVE_INFINITY);
        real.put("NaN", Double.NaN);
        try {
            real.put("parseReal", new MethodWrapper<>(Double.class.getMethod("parseDouble", String.class)).createForInstance(null));
            real.put("isFinite", new MethodWrapper<>(Double.class.getMethod("isFinite", double.class)).createForInstance(null));
            real.put("isInfinite", new MethodWrapper<>(Double.class.getMethod("isInfinite", double.class)).createForInstance(null));
            real.put("isNaN", new MethodWrapper<>(Double.class.getMethod("isNaN", double.class)).createForInstance(null));
            real.put("toString", new MethodWrapper<>(Double.class.getMethod("toString", double.class)).createForInstance(null));
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        }
        globalCtx.put(new Symbol("Real", Symbol.SymbolVisibility.GLOBAL, real));

    }
}
