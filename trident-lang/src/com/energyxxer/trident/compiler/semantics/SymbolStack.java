package com.energyxxer.trident.compiler.semantics;

import java.util.Stack;

public class SymbolStack {
    private Stack<SymbolTable> stack = new Stack<>();

    public SymbolStack() {
    }

    public SymbolTable push(SymbolTable item) {
        return stack.push(item);
    }

    public SymbolTable pop() {
        return stack.pop();
    }

    public SymbolTable peek() {
        return stack.peek();
    }

    public boolean empty() {
        return stack.empty();
    }

    public Symbol search(String name) {
        for(SymbolTable symbol : stack) {
            if(symbol.containsKey(name)) return symbol.get(name);
        }
        return null;
    }
}
