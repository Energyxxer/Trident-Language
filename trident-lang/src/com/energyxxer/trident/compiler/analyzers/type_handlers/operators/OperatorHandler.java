package com.energyxxer.trident.compiler.analyzers.type_handlers.operators;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;
import java.util.Objects;

public interface OperatorHandler<A, B> {

    Object perform(A a, B b, TokenPattern<?> pattern, ISymbolContext ctx);

    class Static {
        private static HashMap<String, OperatorHandler<?, ?>> handlers = new HashMap<>();

        @SuppressWarnings("unchecked")
        public static Object perform(Object a, Operator operator, Object b, TokenPattern<?> pattern, ISymbolContext ctx) {

            String idA = a != null ? VariableTypeHandler.Static.getIdentifierForClass(a.getClass()) : "*";
            String idB = b != null ? VariableTypeHandler.Static.getIdentifierForClass(b.getClass()) : "*";
            OperatorHandler handler = handlers.get(idA + " " + operator.getSymbol() + " " + idB);

            if(handler == null) handler = handlers.get(idA + " " + operator.getSymbol() + " *");

            if(handler == null) handler = handlers.get("* " + operator.getSymbol() + " " + idB);

            if(handler == null) handler = handlers.get("* " + operator.getSymbol() + " *");

            if(handler == null) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "The operator " + operator.getSymbol() + " is not defined for types " + idA.replace("*", "null") + " and " + idB.replace("*", "null"), pattern, ctx);
            }
            return handler.perform(a, b, pattern, ctx);
        }

        static {
            handlers.put("java.lang.Integer + java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);
            handlers.put("java.lang.Double + java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);
            handlers.put("java.lang.Integer + java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);
            handlers.put("java.lang.Double + java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);

            handlers.put("java.lang.Integer - java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);
            handlers.put("java.lang.Double - java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);
            handlers.put("java.lang.Integer - java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);
            handlers.put("java.lang.Double - java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);

            handlers.put("java.lang.Integer * java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);
            handlers.put("java.lang.Double * java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);
            handlers.put("java.lang.Integer * java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);
            handlers.put("java.lang.Double * java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);

            handlers.put("java.lang.Integer / java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a / b);
            handlers.put("java.lang.Double / java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a / b);
            handlers.put("java.lang.Integer / java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a / b);
            handlers.put("java.lang.Double / java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a / b);

            handlers.put("java.lang.Integer % java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a % b);
            handlers.put("java.lang.Double % java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a % b);
            handlers.put("java.lang.Integer % java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a % b);
            handlers.put("java.lang.Double % java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a % b);

            handlers.put("java.lang.Integer > java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);
            handlers.put("java.lang.Double > java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);
            handlers.put("java.lang.Integer > java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);
            handlers.put("java.lang.Double > java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);

            handlers.put("java.lang.Integer >= java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);
            handlers.put("java.lang.Double >= java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);
            handlers.put("java.lang.Integer >= java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);
            handlers.put("java.lang.Double >= java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);

            handlers.put("java.lang.Integer < java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);
            handlers.put("java.lang.Double < java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);
            handlers.put("java.lang.Integer < java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);
            handlers.put("java.lang.Double < java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);

            handlers.put("java.lang.Integer <= java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);
            handlers.put("java.lang.Double <= java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);
            handlers.put("java.lang.Integer <= java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);
            handlers.put("java.lang.Double <= java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);

            handlers.put("java.lang.Integer & java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a & b);
            handlers.put("java.lang.Integer | java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a | b);
            handlers.put("java.lang.Integer ^ java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a ^ b);

            handlers.put("java.lang.Boolean && java.lang.Boolean", (Boolean a, Boolean b, TokenPattern<?> pattern, ISymbolContext ctx) -> a && b);
            handlers.put("java.lang.Boolean || java.lang.Boolean", (Boolean a, Boolean b, TokenPattern<?> pattern, ISymbolContext ctx) -> a || b);

            handlers.put("java.lang.String + java.lang.String", (OperatorHandler<String, String>) (s, str, pattern, compiler) -> s.concat(str));
            handlers.put("java.lang.String + *", (String a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                String converted = InterpolationManager.cast(b, String.class, pattern, ctx);
                return a + converted;
            });

            handlers.put("* == *", (Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> Objects.equals(a,b));
            handlers.put("* != *", (Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> !Objects.equals(a,b));


            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol = *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                a.setValue(b);
                return b;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol += *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.ADD, b, pattern, ctx);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol -= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.SUBTRACT, b, pattern, ctx);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol *= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.MULTIPLY, b, pattern, ctx);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol /= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.DIVIDE, b, pattern, ctx);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol %= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.MODULO, b, pattern, ctx);
                a.setValue(result);
                return result;
            });
        }
    }
}
