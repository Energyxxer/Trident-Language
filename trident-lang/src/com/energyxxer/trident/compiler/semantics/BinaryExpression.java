package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperandType;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.Operator;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class BinaryExpression implements ILazyValue {
    private Object rawA;
    private Operator operator;
    private Object rawB;

    private TokenPattern<?> pattern;
    private ISymbolContext ctx;

    private Object result = null;
    private boolean evaluated = false;

    public BinaryExpression(Object rawA, Operator operator, Object rawB, TokenPattern<?> pattern, ISymbolContext ctx) {
        this.rawA = rawA;
        this.operator = operator;
        this.rawB = rawB;

        this.pattern = pattern;
        this.ctx = ctx;
    }

    public Object evaluate() {

        Object a = rawA;
        Object b = rawB;

        if(operator.isShortCircuiting()) {
            a = toLazy(a);
            b = toLazy(b);
        } else {
            a = evaluateOperand(a, operator.getLeftOperandType() == OperandType.VARIABLE);
            b = evaluateOperand(b, false);
        }

        return OperatorHandler.Static.perform(a, operator, b, pattern, ctx);
    }

    private Object evaluateOperand(Object obj, boolean keepSymbol) {
        if(obj instanceof TokenPattern<?>) {
            return InterpolationManager.parse((TokenPattern<?>) obj, ctx, keepSymbol);
        } else if(obj instanceof ILazyValue) {
            return ((BinaryExpression) obj).getValue(Object.class);
        } else {
            return obj;
        }
    }

    private ILazyValue toLazy(Object obj) {
        if(obj instanceof ILazyValue) {
            return ((ILazyValue) obj);
        } else if(obj instanceof TokenPattern<?>) {
            return new LazyValue(((TokenPattern) obj), ctx);
        } else {
            throw new RuntimeException("no");
        }
    }

    @Override
    public <T> T getValue(Class<T> expected) {
        if(!evaluated) {
            result = evaluate();
            evaluated = true;
        }
        return assertOfType(result, pattern, ctx, expected);
    }
}
