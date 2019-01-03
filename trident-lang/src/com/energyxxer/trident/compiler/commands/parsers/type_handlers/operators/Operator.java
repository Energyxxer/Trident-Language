package com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperandType.VALUE;
import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperandType.VARIABLE;
import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperationOrder.LTR;
import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperationOrder.RTL;
import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperatorType.BINARY;
import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperatorType.UNARY_ANY;
import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators.OperatorType.UNARY_LEFT;

public enum Operator {
    //USED ON OPERATIONS
    INCREMENT("++", 0, UNARY_ANY, LTR, VARIABLE),
    DECREMENT("--", 0, UNARY_ANY, LTR, VARIABLE),
    NOT("!", 0, UNARY_LEFT, RTL, VALUE),
    MULTIPLY("*", 1, BINARY, LTR, VALUE),
    DIVIDE("/", 1, BINARY, LTR, VALUE),
    MODULO("%", 1, BINARY, LTR, VALUE),
    ADD("+", 2, BINARY, LTR, VALUE),
    SUBTRACT("-", 2, BINARY, LTR, VALUE),
    LESS_THAN("<", 3, BINARY, LTR, VALUE),
    LESS_THAN_OR_EQUAL("<=", 3, BINARY, LTR, VALUE),
    GREATER_THAN(">", 3, BINARY, LTR, VALUE),
    GREATER_THAN_OR_EQUAL(">=", 3, BINARY, LTR, VALUE),
    EQUAL("==", 4, BINARY, LTR, VALUE),
    NOT_EQUAL("!=", 4, BINARY, LTR, VALUE),
    AND("&&", 5, BINARY, LTR, VALUE),
    OR("||", 6, BINARY, LTR, VALUE),
    //USED BY THE EXPRESSION PARSER INSTEAD OF THE EXPRESSION HANDLER
    INSTANCEOF("instanceof", 3, BINARY, LTR, VARIABLE),
    ASSIGN("=", 8, BINARY, RTL, VARIABLE),
    ADD_THEN_ASSIGN("+=", 8, BINARY, RTL, VARIABLE),
    SUBTRACT_THEN_ASSIGN("-=", 8, BINARY, RTL, VARIABLE),
    MULTIPLY_THEN_ASSIGN("*=", 8, BINARY, RTL, VARIABLE),
    DIVIDE_THEN_ASSIGN("/=", 8, BINARY, RTL, VARIABLE),
    MODULO_THEN_ASSIGN("%=", 8, BINARY, RTL, VARIABLE),
    AND_THEN_ASSIGN("&=", 8, BINARY, RTL, VARIABLE),
    OR_THEN_ASSIGN("|=", 8, BINARY, RTL, VARIABLE);

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

    public static Operator getOperatorForSymbol(String symbol) {
        for(Operator op : values())
            if(op.symbol.equals(symbol)) return op;
        throw new IllegalArgumentException("Unknown operator symbol '" + symbol + "'");
    }

    public static Operator getNoAssign(Operator op) {
        return (op.name().endsWith("_THEN_ASSIGN")) ? valueOf(op.name().substring(0, op.name().indexOf("_THEN_ASSIGN"))) : null;
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
