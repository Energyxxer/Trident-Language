package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;
import com.energyxxer.trident.compiler.semantics.TridentException;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "Math")
public class MathLib implements DefaultLibraryProvider {
    private static final double LN_2 = Math.log(2);

    @SuppressWarnings("unchecked")
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject math = new DictionaryObject();
        math.put("pow", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'pow' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            double result = Math.pow(base.doubleValue(), exponent.doubleValue());

            if(params[0] instanceof Double || params[1] instanceof Double) return result;
            else return (int) result;
        });
        math.put("min", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'min' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            double result = Math.min(base.doubleValue(), exponent.doubleValue());

            if(params[0] instanceof Double || params[1] instanceof Double) return result;
            else return (int) result;
        });
        math.put("max", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'max' requires 2 parameters, instead found " + params.length, pattern, file);
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            double result = Math.max(base.doubleValue(), exponent.doubleValue());

            if(params[0] instanceof Double || params[1] instanceof Double) return result;
            else return (int) result;
        });
        math.put("abs", (VariableMethod) (params, patterns, pattern, file) -> {
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
            math.put("round", new MethodWrapper<>(Math.class.getMethod("round", double.class)).createForInstance(null));
            math.put("floorMod", new MethodWrapper<>(Math.class.getMethod("floorMod", int.class, int.class)).createForInstance(null));
            math.put("floorDiv", new MethodWrapper<>(Math.class.getMethod("floorDiv", int.class, int.class)).createForInstance(null));
            math.put("signum", new MethodWrapper<>("signum", (instance, params) -> (int)Math.signum((double)params[0]), double.class).createForInstance(null));
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
        stack.getGlobal().put(new Symbol("Math", Symbol.SymbolAccess.GLOBAL, math));
    }
}
