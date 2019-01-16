package com.energyxxer.trident.compiler.semantics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Collectors;

public class SymbolStack {
    private Stack<SymbolTable> stack = new Stack<>();

    private SymbolTable global;

    public SymbolStack() {
        global = new SymbolTable(null);
    }

    public SymbolTable push(@NotNull SymbolTable item) {
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

    public Symbol search(@NotNull String name) {
        for (Iterator<SymbolTable> it = stack.parallelStream().collect(Collectors.toCollection(ArrayDeque::new)).descendingIterator(); it.hasNext(); ) {
            SymbolTable table = it.next();
            if(table.containsKey(name)) return table.get(name);
        }
        if(global.containsKey(name)) return global.get(name);
        return null;
    }

    public SymbolTable getGlobal() {
        return global;
    }
}
