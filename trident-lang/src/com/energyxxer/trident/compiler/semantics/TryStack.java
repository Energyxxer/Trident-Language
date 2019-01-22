package com.energyxxer.trident.compiler.semantics;

import java.util.Stack;

public class TryStack {
    public static final int BREAKING_TRY = 1;
    public static final int RECOVERING_TRY = 2;
    private Stack<Integer> stack = new Stack<>();

    public void pushBreaking() {
        stack.push(BREAKING_TRY);
    }

    public void pushRecovering() {
        stack.push(RECOVERING_TRY);
    }

    public Integer push(Integer item) {
        return stack.push(item);
    }

    public Integer pop() {
        return stack.pop();
    }

    public Integer peek() {
        return stack.peek();
    }

    public boolean isBreaking() {
        return stack.peek() == BREAKING_TRY;
    }

    public boolean isRecovering() {
        return stack.peek() == RECOVERING_TRY;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
