package com.energyxxer.trident.compiler.analyzers.type_handlers.operators;

import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.BooleanTypeHandler;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.util.logger.Debug;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

public class DefaultOperators {

    @OperatorManager.NativeOperator(symbol = "+", grade = 2)
    public static int add(int a, int b) {
        return a + b;
    }
    @OperatorManager.NativeOperator(symbol = "+", grade = 2)
    public static double add(double a, double b) {
        return a + b;
    }

    @OperatorManager.NativeOperator(symbol = "+", grade = 2)
    public static String concatenate(String a, String b) {
        return a + b;
    }
    @OperatorManager.NativeOperator(symbol = "+", grade = 2)
    public static String concatenate(String a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) {
        return a + ctx.getTypeSystem().castToString(b, pattern, ctx);
    }

    @OperatorManager.NativeOperator(symbol = "-", grade = 2)
    public static int subtract(int a, int b) {
        return a - b;
    }
    @OperatorManager.NativeOperator(symbol = "-", grade = 2)
    public static double subtract(double a, double b) {
        return a - b;
    }

    @OperatorManager.NativeOperator(symbol = "*", grade = 2)
    public static int multiply(int a, int b) {
        return a * b;
    }
    @OperatorManager.NativeOperator(symbol = "*", grade = 2)
    public static double multiply(double a, double b) {
        return a * b;
    }

    @OperatorManager.NativeOperator(symbol = "/", grade = 2)
    public static int divide(int a, int b) {
        return a / b;
    }
    @OperatorManager.NativeOperator(symbol = "/", grade = 2)
    public static double divide(double a, double b) {
        return a / b;
    }

    @OperatorManager.NativeOperator(symbol = "%", grade = 2)
    public static int modulo(int a, int b) {
        return a % b;
    }
    @OperatorManager.NativeOperator(symbol = "%", grade = 2)
    public static double modulo(double a, double b) {
        return a % b;
    }

    @OperatorManager.NativeOperator(symbol = ">", grade = 2)
    public static boolean greater(double a, double b) {
        return a > b;
    }
    @OperatorManager.NativeOperator(symbol = ">=", grade = 2)
    public static boolean greaterEqual(double a, double b) {
        return a >= b;
    }

    @OperatorManager.NativeOperator(symbol = "<", grade = 2)
    public static boolean less(double a, double b) {
        return a < b;
    }
    @OperatorManager.NativeOperator(symbol = "<=", grade = 2)
    public static boolean lessEqual(double a, double b) {
        return a <= b;
    }

    @OperatorManager.NativeOperator(symbol = "&", grade = 2)
    public static int bitwiseAnd(int a, int b) {
        return a & b;
    }
    @OperatorManager.NativeOperator(symbol = "|", grade = 2)
    public static int bitwiseOr(int a, int b) {
        return a | b;
    }
    @OperatorManager.NativeOperator(symbol = "^", grade = 2)
    public static int bitwiseXor(int a, int b) {
        return a ^ b;
    }

    @OperatorManager.NativeOperator(symbol = "<<", grade = 2)
    public static int bitShiftLeft(int a, int b) {
        return a << b;
    }
    @OperatorManager.NativeOperator(symbol = ">>", grade = 2)
    public static int bitShiftRight(int a, int b) {
        return a >> b;
    }

    @OperatorManager.NativeOperator(symbol = "==", grade = 2)
    public static boolean equals(Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(a == b) return true;
        if((a == null) != (b == null)) return false;
        if(Objects.equals(a, b)) return true;

        TypeHandler aType = ctx.getTypeSystem().getHandlerForObject(a);
        TypeHandler bType = ctx.getTypeSystem().getHandlerForObject(b);

        //Try coercing a to b's type
        if(aType.canCoerce(a, bType, ctx) && equalsNoCoercion(aType.coerce(a, bType, pattern, ctx), b)) {
            return true;
        }
        //Try coercing b to a's type
        return bType.canCoerce(b, aType, ctx) && equalsNoCoercion(a, bType.coerce(b, aType, pattern, ctx));
    }
    @OperatorManager.NativeOperator(symbol = "!=", grade = 2)
    public static boolean notEquals(Object a, Object b, TokenPattern<?> pattern, ISymbolContext ctx) {
        return !equals(a, b, pattern, ctx);
    }
    @OperatorManager.NativeOperator(symbol = "-_", grade = 1)
    public static int negate(int a) {
        return -a;
    }
    @OperatorManager.NativeOperator(symbol = "-_", grade = 1)
    public static double negate(double a) {
        return -a;
    }
    @OperatorManager.NativeOperator(symbol = "+_", grade = 1)
    public static int identity(int a) {
        return a;
    }
    @OperatorManager.NativeOperator(symbol = "+_", grade = 1)
    public static double identity(double a) {
        return a;
    }

    @OperatorManager.NativeOperator(symbol = "++_", grade = 1)
    public static int incrementLeft(int a) {
        return a+1;
    }
    @OperatorManager.NativeOperator(symbol = "++_", grade = 1)
    public static double incrementLeft(double a) {
        return a+1;
    }

    @OperatorManager.NativeOperator(symbol = "_++", grade = 1)
    public static int incrementRight(int a) {
        return a+1;
    }
    @OperatorManager.NativeOperator(symbol = "_++", grade = 1)
    public static double incrementRight(double a) {
        return a+1;
    }

    @OperatorManager.NativeOperator(symbol = "--_", grade = 1)
    public static int decrementLeft(int a) {
        return a-1;
    }
    @OperatorManager.NativeOperator(symbol = "--_", grade = 1)
    public static double decrementLeft(double a) {
        return a-1;
    }

    @OperatorManager.NativeOperator(symbol = "_--", grade = 1)
    public static int decrementRight(int a) {
        return a-1;
    }
    @OperatorManager.NativeOperator(symbol = "_--", grade = 1)
    public static double decrementRight(double a) {
        return a-1;
    }

    @OperatorManager.NativeOperator(symbol = "||", grade = 2)
    public static boolean logicalOr(boolean a, boolean b) {
        //just here so if the special operator fails, it can show this as an overload
        return a || b;
    }
    @OperatorManager.NativeOperator(symbol = "&&", grade = 2)
    public static boolean logicalAnd(boolean a, boolean b) {
        //just here so if the special operator fails, it can show this as an overload
        return a && b;
    }

    @OperatorManager.NativeOperator(symbol = "??", grade = 2)
    public static Object nullCoalesce(Object a, Object b) {
        //just here so if the special operator fails, it can show this as an overload
        return a != null ? a : b;
    }

    @OperatorManager.NativeOperator(symbol = "!_", grade = 1)
    public static boolean logicalNegate(boolean a) {
        return !a;
    }
    @OperatorManager.NativeOperator(symbol = "~_", grade = 1)
    public static int bitwiseNot(int a) {
        return ~a;
    }

    @OperatorManager.NativeOperator(symbol = "?", grade = 3)
    public static Object conditional(boolean a, Object b, Object c) {
        Debug.log("Used the non-special conditional ternary op ?:", Debug.MessageType.WARN);
        return a ? b : c;
    }

    private static boolean equalsNoCoercion(Object a, Object b) {
        if(a == b) return true;
        if((a == null) != (b == null)) return false;
        return Objects.equals(a, b);
    }

    static Method[] allMethods;

    static {
        allMethods = DefaultOperators.class.getMethods();
    }

    public static void populateOperatorManager(OperatorManager operatorManager, PrismarineTypeSystem typeSystem) {
        operatorManager.setTypeSystem(typeSystem);
        for(Method method : allMethods) {
            OperatorManager.NativeOperator annot = method.getAnnotation(OperatorManager.NativeOperator.class);
            if(annot != null) {
                String symbol = annot.symbol();
                HashMap<String, ClassMethodFamily> targetMap;
                switch(annot.grade()) {
                    case 1: {
                        if(annot.symbol().charAt(0) == '_') {
                            targetMap = operatorManager.unaryRightOperators;
                        } else {
                            targetMap = operatorManager.unaryLeftOperators;
                        }
                        symbol = symbol.replace("_","");
                        break;
                    }
                    case 2: {
                        targetMap = operatorManager.binaryOperators;
                        break;
                    }
                    case 3: {
                        targetMap = operatorManager.ternaryOperators;
                        break;
                    }
                    default:
                        throw new RuntimeException("Native operator method with invalid parameter count: " + method);
                }
                operatorManager.addOperator(symbol, targetMap, method, typeSystem);
            }
        }

        operatorManager.specialBinaryOperators.put("||", (expr, ctx) -> {
            TypeConstraints boolConstraint = new TypeConstraints(typeSystem, (TypeHandler<?>) typeSystem.getHandlerForHandlerClass(BooleanTypeHandler.class), false);

            Object a = expr.getOperands()[0].evaluate(ctx);
            Object b = expr.getOperands()[1];
            if(boolConstraint.verify(a, ctx)) {
                boolean aAsBool = (boolean) boolConstraint.adjustValue(a, expr.getOperands()[0], ctx);
                if(aAsBool) {
                    return true; //short circuit
                } else {
                    b = ((TokenPattern<?>) b).evaluate(ctx);
                    if(boolConstraint.verify(b, ctx)) {
                        return boolConstraint.adjustValue(b, expr.getOperands()[1], ctx);
                    }
                }
            }
            //alright neither worked
            throw new OperatorManager.SpecialOperatorFailure(new Object[] {a, b});
        });
        operatorManager.specialBinaryOperators.put("&&", (expr, ctx) -> {
            TypeConstraints boolConstraint = new TypeConstraints(typeSystem, (TypeHandler<?>) typeSystem.getHandlerForHandlerClass(BooleanTypeHandler.class), false);

            Object a = expr.getOperands()[0].evaluate(ctx);
            Object b = expr.getOperands()[1];
            if(boolConstraint.verify(a, ctx)) {
                boolean aAsBool = (boolean) boolConstraint.adjustValue(a, expr.getOperands()[0], ctx);
                if(!aAsBool) {
                    return false; //short circuit
                } else {
                    b = ((TokenPattern<?>) b).evaluate(ctx);
                    if(boolConstraint.verify(b, ctx)) {
                        return boolConstraint.adjustValue(b, expr.getOperands()[1], ctx);
                    }
                }
            }
            //alright neither worked
            throw new OperatorManager.SpecialOperatorFailure(new Object[] {a, b});
        });
        operatorManager.specialBinaryOperators.put("??", (expr, ctx) -> {
            Object a = expr.getOperands()[0].evaluate(ctx);
            if(a != null) return a;
            return expr.getOperands()[1].evaluate(ctx);
        });
        operatorManager.specialTernaryOperators.put("?", (expr, ctx) -> {
            TypeConstraints boolConstraint = new TypeConstraints(typeSystem, (TypeHandler<?>) typeSystem.getHandlerForHandlerClass(BooleanTypeHandler.class), false);

            Object a = expr.getOperands()[0].evaluate(ctx);
            TokenPattern<?> b = expr.getOperands()[1];
            TokenPattern<?> c = expr.getOperands()[2];
            if(boolConstraint.verify(a, ctx)) {
                boolean aAsBool = (boolean) boolConstraint.adjustValue(a, expr.getOperands()[0], ctx);
                return (aAsBool ? b : c).evaluate(ctx);
            }
            //alright neither worked
            throw new OperatorManager.SpecialOperatorFailure(new Object[] {a, b, c});
        });

        operatorManager.specialBinaryOperators.put("=", (expr, ctx) -> {
            Object a = expr.getOperands()[0].evaluate(ctx, true);
            Object b = expr.getOperands()[1].evaluate(ctx, false);
            if(a instanceof Symbol) {
                ((Symbol) a).safeSetValue(b, expr, ctx);
                return b;
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Invalid left-hand side in assignment", expr, ctx);
            }
        });

        addCompoundAssignmentOperator("+", operatorManager);
        addCompoundAssignmentOperator("-", operatorManager);
        addCompoundAssignmentOperator("*", operatorManager);
        addCompoundAssignmentOperator("/", operatorManager);
        addCompoundAssignmentOperator("%", operatorManager);
        addCompoundAssignmentOperator("&", operatorManager);
        addCompoundAssignmentOperator("^", operatorManager);
        addCompoundAssignmentOperator("|", operatorManager);
        addCompoundAssignmentOperator("<<", operatorManager);
        addCompoundAssignmentOperator(">>", operatorManager);

        operatorManager.specialUnaryLeftOperators.put("++", (expr, ctx) -> {
            // do not throw any SpecialOperatorFailure exceptions here; this operator is not allowed
            // to pass through the standard overload list if there is not a symbol involved.
            Object a = expr.getOperands()[0].evaluate(ctx, true);
            if(a instanceof Symbol) {
                Object oldValue = ((Symbol) a).getValue(expr, ctx);
                Object newValue = operatorManager.evaluate(operatorManager.unaryLeftOperators.get("++"), new Object[] {oldValue}, expr, ctx);
                ((Symbol) a).safeSetValue(newValue, expr, ctx);
                return newValue;
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Invalid left-hand side in assignment", expr, ctx);
            }
        });
        operatorManager.specialUnaryRightOperators.put("++", (expr, ctx) -> {
            // do not throw any SpecialOperatorFailure exceptions here; this operator is not allowed
            // to pass through the standard overload list if there is not a symbol involved.
            Object a = expr.getOperands()[0].evaluate(ctx, true);
            if(a instanceof Symbol) {
                Object oldValue = ((Symbol) a).getValue(expr, ctx);
                Object newValue = operatorManager.evaluate(operatorManager.unaryRightOperators.get("++"), new Object[] {oldValue}, expr, ctx);
                ((Symbol) a).safeSetValue(newValue, expr, ctx);
                return oldValue;
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Invalid left-hand side in assignment", expr, ctx);
            }
        });

        operatorManager.specialUnaryLeftOperators.put("--", (expr, ctx) -> {
            // do not throw any SpecialOperatorFailure exceptions here; this operator is not allowed
            // to pass through the standard overload list if there is not a symbol involved.
            Object a = expr.getOperands()[0].evaluate(ctx, true);
            if(a instanceof Symbol) {
                Object oldValue = ((Symbol) a).getValue(expr, ctx);
                Object newValue = operatorManager.evaluate(operatorManager.unaryLeftOperators.get("--"), new Object[] {oldValue}, expr, ctx);
                ((Symbol) a).safeSetValue(newValue, expr, ctx);
                return newValue;
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Invalid left-hand side in assignment", expr, ctx);
            }
        });
        operatorManager.specialUnaryRightOperators.put("--", (expr, ctx) -> {
            // do not throw any SpecialOperatorFailure exceptions here; this operator is not allowed
            // to pass through the standard overload list if there is not a symbol involved.
            Object a = expr.getOperands()[0].evaluate(ctx, true);
            if(a instanceof Symbol) {
                Object oldValue = ((Symbol) a).getValue(expr, ctx);
                Object newValue = operatorManager.evaluate(operatorManager.unaryRightOperators.get("--"), new Object[] {oldValue}, expr, ctx);
                ((Symbol) a).safeSetValue(newValue, expr, ctx);
                return oldValue;
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Invalid left-hand side in assignment", expr, ctx);
            }
        });
    }

    private static void addCompoundAssignmentOperator(String baseOperator, OperatorManager operatorManager) {
        operatorManager.specialBinaryOperators.put(baseOperator + "=", (expr, ctx) -> {
            Object a = expr.getOperands()[0].evaluate(ctx, true);
            Object b = expr.getOperands()[1].evaluate(ctx);
            if(a instanceof Symbol) {
                Object newValue = operatorManager.evaluate(operatorManager.binaryOperators.get(baseOperator), new Object[] {((Symbol) a).getValue(expr, ctx), b}, expr, ctx);
                ((Symbol) a).safeSetValue(newValue, expr, ctx);
                return newValue;
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Invalid left-hand side in assignment", expr, ctx);
            }
        });
    }

}
