package com.energyxxer.trident.compiler.analyzers.type_handlers.operators;

import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.expressions.TokenBinaryExpression;
import com.energyxxer.prismarine.expressions.TokenExpression;
import com.energyxxer.prismarine.expressions.TokenTernaryExpression;
import com.energyxxer.prismarine.expressions.TokenUnaryExpression;
import com.energyxxer.prismarine.operators.BinaryOperator;
import com.energyxxer.prismarine.operators.OperationOrder;
import com.energyxxer.prismarine.operators.TernaryOperator;
import com.energyxxer.prismarine.operators.UnaryOperator;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiFunction;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class OperatorManager {
    private PrismarineTypeSystem typeSystem;

    HashMap<String, ClassMethodFamily> unaryLeftOperators = new HashMap<>();
    HashMap<String, ClassMethodFamily> unaryRightOperators = new HashMap<>();
    HashMap<String, ClassMethodFamily> binaryOperators = new HashMap<>();
    HashMap<String, ClassMethodFamily> ternaryOperators = new HashMap<>();

    HashMap<String, BiFunction<TokenUnaryExpression, ISymbolContext, Object>> specialUnaryLeftOperators = new HashMap<>();
    HashMap<String, BiFunction<TokenUnaryExpression, ISymbolContext, Object>> specialUnaryRightOperators = new HashMap<>();
    HashMap<String, BiFunction<TokenBinaryExpression, ISymbolContext, Object>> specialBinaryOperators = new HashMap<>();
    HashMap<String, BiFunction<TokenTernaryExpression, ISymbolContext, Object>> specialTernaryOperators = new HashMap<>();

    public OperatorManager() {
    }

    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    public void setTypeSystem(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    void addOperator(String symbol, HashMap<String, ClassMethodFamily> map, Method method, PrismarineTypeSystem typeSystem) {
        addOperator(symbol, map, new ClassMethod(((TridentTypeSystem) typeSystem).getBaseClass(), null, nativeMethodsToFunction(typeSystem, null, method)).setVisibility(TridentSymbolVisibility.PUBLIC));
    }

    private void addOperator(String symbol, HashMap<String, ClassMethodFamily> map, ClassMethod method) {
        ClassMethodFamily family = map.get(symbol);
        if(family == null) {
            map.put(symbol, family = new ClassMethodFamily(symbol));
        }
        family.putOverload(method, CustomClass.MemberParentMode.FORCE, null, null);
    }

    private Object evaluate(TokenUnaryExpression expr, ISymbolContext ctx) {
        UnaryOperator op = expr.getOperator();
        String sym = op.getSymbol();

        HashMap<String, ClassMethodFamily> correspondingMap = op.getOrder() == OperationOrder.LTR ? unaryRightOperators : unaryLeftOperators;
        HashMap<String, BiFunction<TokenUnaryExpression, ISymbolContext, Object>> correspondingSpecialMap = op.getOrder() == OperationOrder.LTR ? specialUnaryRightOperators : specialUnaryLeftOperators;

        Object[] operands;

        if(correspondingSpecialMap.containsKey(sym)) {
            try {
                return correspondingSpecialMap.get(sym).apply(expr, ctx);
            } catch(SpecialOperatorFailure failure) {
                //special operator overload didn't match the types
                //Reuse the already-parsed operands
                operands = failure.getOperandsAsEvaluated();
            }
        } else {
            TokenPattern<?>[] operandPatterns = expr.getOperands();
            operands = Arrays.copyOf(operandPatterns, operandPatterns.length, Object[].class);
        }

        ClassMethodFamily family = correspondingMap.get(sym);
        if(family == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Unknown unary operator " + sym, expr, ctx);
        }

        return evaluate(family, operands, expr, ctx);
    }
    private Object evaluate(TokenBinaryExpression expr, ISymbolContext ctx) {
        BinaryOperator op = expr.getOperator();
        String sym = op.getSymbol();

        Object[] operands;

        if(specialBinaryOperators.containsKey(sym)) {
            try {
                return specialBinaryOperators.get(sym).apply(expr, ctx);
            } catch(SpecialOperatorFailure failure) {
                //special operator overload didn't match the types
                //Reuse the already-parsed operands
                operands = failure.getOperandsAsEvaluated();
            }
        } else {
            TokenPattern<?>[] operandPatterns = expr.getOperands();
            operands = Arrays.copyOf(operandPatterns, operandPatterns.length, Object[].class);
        }

        ClassMethodFamily family = binaryOperators.get(sym);
        if(family == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Unknown binary operator " + sym, expr, ctx);
        }

        return evaluate(family, operands, expr, ctx);
    }
    private Object evaluate(TokenTernaryExpression expr, ISymbolContext ctx) {
        TernaryOperator op = expr.getOperator();
        String sym = op.getSymbol();

        Object[] operands;

        if(specialTernaryOperators.containsKey(sym)) {
            try {
                return specialTernaryOperators.get(sym).apply(expr, ctx);
            } catch(SpecialOperatorFailure failure) {
                //special operator overload didn't match the types
                //Reuse the already-parsed operands
                operands = failure.getOperandsAsEvaluated();
            }
        } else {
            TokenPattern<?>[] operandPatterns = expr.getOperands();
            operands = Arrays.copyOf(operandPatterns, operandPatterns.length, Object[].class);
        }

        ClassMethodFamily family = ternaryOperators.get(sym);
        if(family == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Unknown ternary operator " + sym + " " + op.getTernaryRight().getSymbol(), expr, ctx);
        }

        return evaluate(family, operands, expr, ctx);
    }
    public Object evaluate(@NotNull TokenExpression expr, ISymbolContext ctx) {
        if(expr instanceof TokenUnaryExpression) return evaluate((TokenUnaryExpression) expr, ctx);
        if(expr instanceof TokenBinaryExpression) return evaluate((TokenBinaryExpression) expr, ctx);
        if(expr instanceof TokenTernaryExpression) return evaluate((TokenTernaryExpression) expr, ctx);
        throw new IllegalArgumentException("TokenExpression that is not unary, binary nor ternary: " + expr.getClass());
    }

    Object evaluate(ClassMethodFamily family, Object[] operands, TokenExpression expr, ISymbolContext ctx) {
        TokenPattern<?>[] operandPatterns = expr.getOperands();
        for(int i = 0; i < operands.length; i++) {
            if(operands[i] instanceof TokenExpression) {
                operands[i] = evaluate((TokenExpression) operands[i], ctx);
            } else if(operands[i] instanceof TokenPattern<?>) {
                operands[i] = ((TokenPattern<?>) operands[i]).evaluate(ctx);
            }
        }

        PrismarineFunction function = family.pickOverload(new ActualParameterList(Arrays.asList(operands), Arrays.asList(operandPatterns), expr), expr, ctx);
        return function.safeCall(operands, operandPatterns, expr, ctx, null);
    }

    public void registerUnaryLeftOperator(String symbol, ClassMethod method) {
        addOperator(symbol, unaryLeftOperators, method);
    }

    public void registerUnaryRightOperator(String symbol, ClassMethod method) {
        addOperator(symbol, unaryRightOperators, method);
    }

    public void registerBinaryOperator(String symbol, ClassMethod method) {
        addOperator(symbol, binaryOperators, method);
    }

    public void registerTernaryOperator(String symbol, ClassMethod method) {
        addOperator(symbol, ternaryOperators, method);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NativeOperator {
        String symbol();

        int grade();
    }

    public static class SpecialOperatorFailure extends RuntimeException {
        private Object[] operandsAsEvaluated;

        public SpecialOperatorFailure(Object[] operandsAsEvaluated) {
            this.operandsAsEvaluated = operandsAsEvaluated;
        }

        public Object[] getOperandsAsEvaluated() {
            return operandsAsEvaluated;
        }
    }
}
