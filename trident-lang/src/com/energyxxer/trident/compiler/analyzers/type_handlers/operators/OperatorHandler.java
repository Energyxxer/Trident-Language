package com.energyxxer.trident.compiler.analyzers.type_handlers.operators;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.ILazyValue;
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
            OperatorType operatorType = operator.getOperatorType();
            
            if(operatorType == OperatorType.BINARY) {
                String idA = a != null ? VariableTypeHandler.Static.getIdentifierForClass(a.getClass()) : "*";
                String idB = b != null ? VariableTypeHandler.Static.getIdentifierForClass(b.getClass()) : "*";
                OperatorHandler handler = handlers.get(idA + " " + operator.getSymbol() + " " + idB);

                if (handler == null) handler = handlers.get(idA + " " + operator.getSymbol() + " *");

                if (handler == null) handler = handlers.get("* " + operator.getSymbol() + " " + idB);

                if (handler == null) handler = handlers.get("* " + operator.getSymbol() + " *");

                if (handler == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, operator.getUndefinedMessage(idA.replace("*", "null"), idB.replace("*", "null")), pattern, ctx);
                }
                return handler.perform(a, b, pattern, ctx);
            } else if(operatorType == OperatorType.UNARY_ANY || operatorType == OperatorType.UNARY_LEFT || operatorType == OperatorType.UNARY_RIGHT) {
                OperatorHandler handler = null;
                String idA = a != null ? VariableTypeHandler.Static.getIdentifierForClass(a.getClass()) : "*";
                String idB = b != null ? VariableTypeHandler.Static.getIdentifierForClass(b.getClass()) : "*";

                if(operatorType == OperatorType.UNARY_LEFT) {
                    handler = handlers.get(operator.getSymbol() + " " + idB);
                    if(handler == null) handler = handlers.get(operator.getSymbol() + " *");
                } else if(operatorType == OperatorType.UNARY_RIGHT) {
                    handler = handlers.get(idA + " " + operator.getSymbol());
                    if(handler == null) handler = handlers.get("* " + operator.getSymbol());
                } else {
                    if(a == null && b == null) {
                        handler = handlers.get("* " + operator.getSymbol());
                        if(handler == null) handler = handlers.get(operator.getSymbol() + " *");
                    } else {
                        if(a == null) {
                            handler = handlers.get(operator.getSymbol() + " " + idB);
                            if(handler == null) handler = handlers.get(operator.getSymbol() + " *");
                        } else if(b == null) {
                            handler = handlers.get(idA + " " + operator.getSymbol());
                            if(handler == null) handler = handlers.get("* " + operator.getSymbol());
                        }
                    }
                }

                String errorType = idA.replace("*",idB).replace("*", "null");

                if (handler == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, operator.getUndefinedMessage(errorType, ""), pattern, ctx);
                }
                return handler.perform(a, b, pattern, ctx);
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "The operator " + operator.getSymbol() + " is not defined", pattern, ctx);
            }
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

            handlers.put("java.lang.Integer / java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("java.lang.Double / java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("java.lang.Integer / java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("java.lang.Double / java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });

            handlers.put("java.lang.Integer % java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("java.lang.Double % java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("java.lang.Integer % java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("java.lang.Double % java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });

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

            //handlers.put("java.lang.Boolean && java.lang.Boolean", (Boolean a, Boolean b, TokenPattern<?> pattern, ISymbolContext ctx) -> a && b);
            //handlers.put("java.lang.Boolean || java.lang.Boolean", (Boolean a, Boolean b, TokenPattern<?> pattern, ISymbolContext ctx) -> a || b);

            handlers.put("com.energyxxer.trident.compiler.semantics.ILazyValue && com.energyxxer.trident.compiler.semantics.ILazyValue", (ILazyValue a, ILazyValue b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Boolean realA = a.getValue(Boolean.class);
                if(!realA) return false;
                return b.getValue(Boolean.class);
            });
            handlers.put("com.energyxxer.trident.compiler.semantics.ILazyValue || com.energyxxer.trident.compiler.semantics.ILazyValue", (ILazyValue a, ILazyValue b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Boolean realA = a.getValue(Boolean.class);
                if(realA) return true;
                return b.getValue(Boolean.class);
            });

            handlers.put("java.lang.String + java.lang.String", (OperatorHandler<String, String>) (s, str, pattern, compiler) -> s.concat(str));
            handlers.put("java.lang.String + *", (String a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                String converted = InterpolationManager.cast(b, String.class, pattern, ctx);
                return a + converted;
            });

            handlers.put("* == *", (Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> Objects.equals(a,b));
            handlers.put("* != *", (Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> !Objects.equals(a,b));

            handlers.put("- java.lang.Integer", (Object nl, Integer a, TokenPattern<?> pattern, ISymbolContext ctx) -> -a);
            handlers.put("- java.lang.Double", (Object nl, Double a, TokenPattern<?> pattern, ISymbolContext ctx) -> -a);
            handlers.put("+ java.lang.Integer", (Object nl, Integer a, TokenPattern<?> pattern, ISymbolContext ctx) -> a);
            handlers.put("+ java.lang.Double", (Object nl, Double a, TokenPattern<?> pattern, ISymbolContext ctx) -> a);
            handlers.put("! java.lang.Boolean", (Object nl, Boolean a, TokenPattern<?> pattern, ISymbolContext ctx) -> !a);
            handlers.put("~ java.lang.Integer", (Object nl, Integer a, TokenPattern<?> pattern, ISymbolContext ctx) -> ~a);

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol ++", (Symbol a, Object nl, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object oldValue = a.getValue();
                Object result = perform(a.getValue(), Operator.ADD, 1, pattern, ctx);
                a.setValue(result);
                return oldValue;
            });
            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol --", (Symbol a, Object nl, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object oldValue = a.getValue();
                Object result = perform(a.getValue(), Operator.SUBTRACT, 1, pattern, ctx);
                a.setValue(result);
                return oldValue;
            });

            handlers.put("++ com.energyxxer.trident.compiler.semantics.Symbol", (Object nl, Symbol a, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.ADD, 1, pattern, ctx);
                a.setValue(result);
                return a.getValue();
            });
            handlers.put("-- com.energyxxer.trident.compiler.semantics.Symbol", (Object nl, Symbol a, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(), Operator.SUBTRACT, 1, pattern, ctx);
                a.setValue(result);
                return a.getValue();
            });

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
