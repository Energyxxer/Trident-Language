package com.energyxxer.trident.compiler.analyzers.type_handlers.operators;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.ILazyValue;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;
import java.util.Objects;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager.getInternalTypeIdentifierForObject;

public interface OperatorHandler<A, B> {

    Object perform(A a, B b, TokenPattern<?> pattern, ISymbolContext ctx);

    class Static {
        private static HashMap<String, OperatorHandler<?, ?>> handlers = new HashMap<>();

        @SuppressWarnings("unchecked")
        public static Object perform(Object a, Operator operator, Object b, TokenPattern<?> pattern, ISymbolContext ctx) {
            OperatorType operatorType = operator.getOperatorType();
            
            if(operatorType == OperatorType.BINARY) {
                String idA = a != null ? getInternalTypeIdentifierForObject(a) : "*";
                String idB = b != null ? getInternalTypeIdentifierForObject(b) : "*";
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
                String idA = a != null ? getInternalTypeIdentifierForObject(a) : "*";
                String idB = b != null ? getInternalTypeIdentifierForObject(b) : "*";

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
            handlers.put("primitive(int) + primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);
            handlers.put("primitive(real) + primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);
            handlers.put("primitive(int) + primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);
            handlers.put("primitive(real) + primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a + b);

            handlers.put("primitive(int) - primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);
            handlers.put("primitive(real) - primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);
            handlers.put("primitive(int) - primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);
            handlers.put("primitive(real) - primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a - b);

            handlers.put("primitive(int) * primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);
            handlers.put("primitive(real) * primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);
            handlers.put("primitive(int) * primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);
            handlers.put("primitive(real) * primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a * b);

            handlers.put("primitive(int) / primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("primitive(real) / primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("primitive(int) / primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("primitive(real) / primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a / b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });

            handlers.put("primitive(int) % primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("primitive(real) % primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("primitive(int) % primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });
            handlers.put("primitive(real) % primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                try {
                    return a % b;
                } catch(ArithmeticException ex) {
                    throw new TridentException(TridentException.Source.ARITHMETIC_ERROR, ex.getMessage(), pattern, ctx);
                }
            });

            handlers.put("primitive(int) > primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);
            handlers.put("primitive(real) > primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);
            handlers.put("primitive(int) > primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);
            handlers.put("primitive(real) > primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a > b);

            handlers.put("primitive(int) >= primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);
            handlers.put("primitive(real) >= primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);
            handlers.put("primitive(int) >= primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);
            handlers.put("primitive(real) >= primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a >= b);

            handlers.put("primitive(int) < primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);
            handlers.put("primitive(real) < primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);
            handlers.put("primitive(int) < primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);
            handlers.put("primitive(real) < primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a < b);

            handlers.put("primitive(int) <= primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);
            handlers.put("primitive(real) <= primitive(int)", (Double a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);
            handlers.put("primitive(int) <= primitive(real)", (Integer a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);
            handlers.put("primitive(real) <= primitive(real)", (Double a, Double b, TokenPattern<?> pattern, ISymbolContext ctx) -> a <= b);

            handlers.put("primitive(int) & primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a & b);
            handlers.put("primitive(int) | primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a | b);
            handlers.put("primitive(int) ^ primitive(int)", (Integer a, Integer b, TokenPattern<?> pattern, ISymbolContext ctx) -> a ^ b);

            handlers.put("lazy && lazy", (ILazyValue a, ILazyValue b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Boolean realA = a.getValue(Boolean.class);
                if(!realA) return false;
                return b.getValue(Boolean.class);
            });
            handlers.put("lazy || lazy", (ILazyValue a, ILazyValue b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Boolean realA = a.getValue(Boolean.class);
                if(realA) return true;
                return b.getValue(Boolean.class);
            });

            handlers.put("primitive(string) + primitive(string)", (OperatorHandler<String, String>) (s, str, pattern, compiler) -> s.concat(str));
            handlers.put("primitive(string) + *", (String a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                String converted = InterpolationManager.castToString(b, pattern, ctx);
                return a + converted;
            });

            handlers.put("* == *", (Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> equals(a,b, pattern, ctx));
            handlers.put("* != *", (Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> !equals(a,b, pattern, ctx));

            handlers.put("- primitive(int)", (Object nl, Integer a, TokenPattern<?> pattern, ISymbolContext ctx) -> -a);
            handlers.put("- primitive(real)", (Object nl, Double a, TokenPattern<?> pattern, ISymbolContext ctx) -> -a);
            handlers.put("+ primitive(int)", (Object nl, Integer a, TokenPattern<?> pattern, ISymbolContext ctx) -> a);
            handlers.put("+ primitive(real)", (Object nl, Double a, TokenPattern<?> pattern, ISymbolContext ctx) -> a);
            handlers.put("! primitive(boolean)", (Object nl, Boolean a, TokenPattern<?> pattern, ISymbolContext ctx) -> !a);
            handlers.put("~ primitive(int)", (Object nl, Integer a, TokenPattern<?> pattern, ISymbolContext ctx) -> ~a);

            handlers.put("symbol ++", (Symbol a, Object nl, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object oldValue = a.getValue(pattern, ctx);
                Object result = perform(a.getValue(pattern, ctx), Operator.ADD, 1, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return oldValue;
            });
            handlers.put("symbol --", (Symbol a, Object nl, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object oldValue = a.getValue(pattern, ctx);
                Object result = perform(a.getValue(pattern, ctx), Operator.SUBTRACT, 1, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return oldValue;
            });

            handlers.put("++ symbol", (Object nl, Symbol a, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.ADD, 1, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return a.getValue(pattern, ctx);
            });
            handlers.put("-- symbol", (Object nl, Symbol a, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.SUBTRACT, 1, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return a.getValue(pattern, ctx);
            });

            handlers.put("symbol = *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                a.safeSetValue(b, pattern, ctx);
                return b;
            });

            handlers.put("symbol += *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.ADD, b, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return result;
            });

            handlers.put("symbol -= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.SUBTRACT, b, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return result;
            });

            handlers.put("symbol *= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.MULTIPLY, b, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return result;
            });

            handlers.put("symbol /= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.DIVIDE, b, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return result;
            });

            handlers.put("symbol %= *", (Symbol a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) -> {
                Object result = perform(a.getValue(pattern, ctx), Operator.MODULO, b, pattern, ctx);
                a.safeSetValue(result, pattern, ctx);
                return result;
            });
        }

        public static boolean equals(Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) {
            if(a == b) return true;
            if((a == null) != (b == null)) return false;
            if(Objects.equals(a, b)) return true;

            TypeHandler aType = TridentTypeManager.getHandlerForObject(a);
            TypeHandler bType = TridentTypeManager.getHandlerForObject(b);

            //Try coercing a to b's type
            if(aType.canCoerce(a, bType) && equalsNoCoercion(aType.coerce(a, bType, pattern, ctx), b)) {
                return true;
            }
            //Try coercing b to a's type
            return bType.canCoerce(b, aType) && equalsNoCoercion(a, bType.coerce(b, aType, pattern, ctx));
        }

        public static boolean equalsNoCoercion(Object a, Object b) {
            if(a == b) return true;
            if((a == null) != (b == null)) return false;
            return Objects.equals(a, b);
        }
    }
}
