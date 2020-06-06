package com.energyxxer.trident.compiler.analyzers.type_handlers.operators;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperandType.VALUE;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperandType.VARIABLE;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorType.BINARY;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorType.UNARY_ANY;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorType.UNARY_LEFT;

public enum Operator {
    //USED ON OPERATIONS
    INCREMENT("++", 0, UNARY_ANY, OperationOrder.LTR, VARIABLE),
    DECREMENT("--", 0, UNARY_ANY, OperationOrder.LTR, VARIABLE),

    NOT("!", 1, UNARY_LEFT, OperationOrder.RTL, VALUE),
    NEGATE_UNARY("-", 1, UNARY_LEFT, OperationOrder.LTR, VALUE),
    PLUS_UNARY("+", 1, UNARY_LEFT, OperationOrder.LTR, VALUE),
    BITWISE_NOT("~", 1, UNARY_LEFT, OperationOrder.RTL, VALUE),

    MULTIPLY("*", 2, BINARY, OperationOrder.LTR, VALUE),
    DIVIDE("/", 2, BINARY, OperationOrder.LTR, VALUE),
    MODULO("%", 2, BINARY, OperationOrder.LTR, VALUE),

    ADD("+", 3, BINARY, OperationOrder.LTR, VALUE),
    SUBTRACT("-", 3, BINARY, OperationOrder.LTR, VALUE),

    BIT_SHIFT_LEFT("<<", 4, BINARY, OperationOrder.LTR, VALUE),
    BIT_SHIFT_RIGHT(">>", 4, BINARY, OperationOrder.LTR, VALUE),

    LESS_THAN("<", 5, BINARY, OperationOrder.LTR, VALUE),
    LESS_THAN_OR_EQUAL("<=", 5, BINARY, OperationOrder.LTR, VALUE),
    GREATER_THAN(">", 5, BINARY, OperationOrder.LTR, VALUE),
    GREATER_THAN_OR_EQUAL(">=", 5, BINARY, OperationOrder.LTR, VALUE),

    EQUAL("==", 6, BINARY, OperationOrder.LTR, VALUE),
    NOT_EQUAL("!=", 6, BINARY, OperationOrder.LTR, VALUE),

    BITWISE_AND("&", 7, BINARY, OperationOrder.LTR, VALUE),

    BITWISE_XOR("^", 8, BINARY, OperationOrder.LTR, VALUE),

    BITWISE_OR("|", 9, BINARY, OperationOrder.LTR, VALUE),

    AND("&&", 10, BINARY, OperationOrder.LTR, VALUE),
    OR("||", 11, BINARY, OperationOrder.LTR, VALUE),

    //USED BY THE EXPRESSION PARSER INSTEAD OF THE EXPRESSION HANDLER
    ASSIGN("=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    ADD_THEN_ASSIGN("+=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    SUBTRACT_THEN_ASSIGN("-=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    MULTIPLY_THEN_ASSIGN("*=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    DIVIDE_THEN_ASSIGN("/=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    MODULO_THEN_ASSIGN("%=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    AND_THEN_ASSIGN("&=", 99, BINARY, OperationOrder.RTL, VARIABLE),
    OR_THEN_ASSIGN("|=", 99, BINARY, OperationOrder.RTL, VARIABLE);

    private String symbol;
    private int precedence;
    private OperationOrder order;
    private OperatorType operatorType;
    private OperandType leftOperandType;

    Operator(String symbol, int precedence, OperatorType operatorType, OperationOrder order, OperandType leftOperandType) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.operatorType = operatorType;
        this.order = order;
        this.leftOperandType = leftOperandType;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getPrecedence() {
        return precedence;
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public OperationOrder getOrder() {
        return order;
    }

    public OperandType getLeftOperandType() {
        return leftOperandType;
    }

    public boolean isShortCircuiting() {
        return this == OR || this == AND;
    }

    public static Operator getOperatorForSymbol(String symbol) {
        return getOperatorForSymbol(symbol, false);
    }

    public static Operator getOperatorForSymbol(String symbol, boolean unary) {
        for(Operator op : values())
            if(op.symbol.equals(symbol) && ((unary && op.operatorType != BINARY) || (!unary && op.operatorType == BINARY))) return op;
        throw new IllegalArgumentException("Unknown operator symbol '" + symbol + "'");
    }

    public static Operator getNoAssign(Operator op) {
        return (op.name().endsWith("_THEN_ASSIGN")) ? valueOf(op.name().substring(0, op.name().indexOf("_THEN_ASSIGN"))) : null;
    }

    public String getUndefinedMessage(String leftType, String rightType) {
        if(operatorType == BINARY) {
            if(leftOperandType == VALUE) {
                return "The operator " + this.getSymbol() + " is not defined for types " + leftType + " and " + rightType;
            } else {
                return "Invalid left-hand side in assignment";
            }
        } else {
            return "The operator " + this.getSymbol() + " is not defined for type " + leftType;
        }
    }

    @Override
    public String toString() {
        return "Operator{" +
                "symbol='" + symbol + '\'' +
                ", precedence=" + precedence +
                ", order=" + order +
                '}';
    }
}
