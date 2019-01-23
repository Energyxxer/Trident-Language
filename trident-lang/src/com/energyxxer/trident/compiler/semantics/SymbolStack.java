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

    public Symbol search(@NotNull String name, TridentFile from) {
        for (Iterator<SymbolTable> it = new ArrayDeque<>(stack).descendingIterator(); it.hasNext(); ) {
            SymbolTable table = it.next();
            if(table.containsKey(name)) {
                Symbol symbol = table.get(name);
                if(symbol.getAccess() == Symbol.SymbolAccess.GLOBAL
                || (symbol.getAccess() == Symbol.SymbolAccess.LOCAL && (from.equals(table.getFile()) || hasImported(from, table.getFile()) || from.isSubFileOf(table.getFile())))
                || (symbol.getAccess() == Symbol.SymbolAccess.PRIVATE && (from.equals(table.getFile()) || from.isSubFileOf(table.getFile()))))
                return symbol;
            }
        }
        if(global.containsKey(name)) return global.get(name);
        return null;
    }

    private boolean hasImported(TridentFile source, TridentFile target) {
        return source.getCascadingRequires().contains(target.getResourceLocation());
    }

    public SymbolTable getGlobal() {
        return global;
    }
}
