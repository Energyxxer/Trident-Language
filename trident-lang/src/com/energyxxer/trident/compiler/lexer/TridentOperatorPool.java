package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.prismarine.operators.OperationOrder;
import com.energyxxer.prismarine.operators.OperatorPool;

public class TridentOperatorPool {
    public static final OperatorPool INSTANCE;

    private TridentOperatorPool() {}

    static {
        INSTANCE = new OperatorPool();

        INSTANCE.addBinaryOperator("*", 2, OperationOrder.LTR);
        INSTANCE.addBinaryOperator("/", 2, OperationOrder.LTR);
        INSTANCE.addBinaryOperator("%", 2, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("+", 3, OperationOrder.LTR);
        INSTANCE.addBinaryOperator("-", 3, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("<<", 4, OperationOrder.LTR);
        INSTANCE.addBinaryOperator(">>", 4, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("<", 5, OperationOrder.LTR);
        INSTANCE.addBinaryOperator("<=", 5, OperationOrder.LTR);
        INSTANCE.addBinaryOperator(">", 5, OperationOrder.LTR);
        INSTANCE.addBinaryOperator(">=", 5, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("==", 6, OperationOrder.LTR);
        INSTANCE.addBinaryOperator("!=", 6, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("&", 7, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("^", 8, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("|", 9, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("&&", 10, OperationOrder.LTR);
        INSTANCE.addBinaryOperator("||", 11, OperationOrder.LTR);

        INSTANCE.addBinaryOperator("??", 12, OperationOrder.RTL);

        INSTANCE.addTernaryOperator("?", ":", 13, OperationOrder.RTL);

        INSTANCE.addBinaryOperator("=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("+=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("-=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("*=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("/=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("%=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("&=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("^=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("|=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator("<<=", 99, OperationOrder.RTL);
        INSTANCE.addBinaryOperator(">>=", 99, OperationOrder.RTL);

        INSTANCE.addEitherUnaryOperator("++");
        INSTANCE.addEitherUnaryOperator("--");

        INSTANCE.addLeftUnaryOperator("!");
        INSTANCE.addLeftUnaryOperator("-");
        INSTANCE.addLeftUnaryOperator("+");
        INSTANCE.addLeftUnaryOperator("~");
    }
}
