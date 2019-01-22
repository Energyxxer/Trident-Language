package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.util.logger.Debug;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Stack;

public class SymbolStack {
    private Stack<SymbolTable> stack = new Stack<>();

    private SymbolTable global;

    private Stack<StackTraceElement> debugStack = new Stack<>();

    public SymbolStack() {
        global = new SymbolTable(null);
    }

    public SymbolTable push(@NotNull SymbolTable item) {
        debugStack.push(Thread.currentThread().getStackTrace()[2]);
        return stack.push(item);
    }
    public SymbolTable pop() {
        if(!debugStack.peek().getMethodName().equals(Thread.currentThread().getStackTrace()[2].getMethodName())) {
            StackTraceElement owner = debugStack.peek();
            StackTraceElement intruder = Thread.currentThread().getStackTrace()[2];
            Debug.log("Illegal call stack pop");
        }
        debugStack.pop();
        return stack.pop();
    }

    public SymbolTable peek() {
        return stack.peek();
    }

    public boolean empty() {
        return stack.empty();
    }

    public Symbol search(@NotNull String name) {
        for (Iterator<SymbolTable> it = new ArrayDeque<>(stack).descendingIterator(); it.hasNext(); ) {
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
