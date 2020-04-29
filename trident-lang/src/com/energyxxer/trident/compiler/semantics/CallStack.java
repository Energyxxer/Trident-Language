package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.StackTrace;

import java.util.ArrayList;
import java.util.Stack;

public class CallStack {

    public static class Call {

        String calledFunction;

        TokenPattern<?> calledPattern;
        TridentFile calledFile;
        TokenPattern<?> calledBy;
        public Call(TokenPattern<?> calledPattern, TridentFile calledFile, TokenPattern<?> calledBy) {
            this("<anonymous function>", calledPattern, calledFile, calledBy);
        }
        public Call(String calledFunction, TokenPattern<?> calledPattern, TridentFile calledFile, TokenPattern<?> calledBy) {
            this.calledFunction = calledFunction;
            this.calledPattern = calledPattern;
            this.calledFile = calledFile;
            this.calledBy = calledBy;
        }

        @Override
        public String toString() {
            return "at " + calledFile.getResourceLocation() + " ~ " + calledFunction + " (" + calledBy.getFile().getName() + ":" + calledBy.getStringLocation().line + ")\n";
        }
    }

    private Stack<Call> stack = new Stack<>();

    public Call push(Call item) {
        return stack.push(item);
    }

    public Call pop() {
        return stack.pop();
    }

    public Call peek() {
        return stack.peek();
    }

    public StackTrace getView(TokenPattern<?> leaf) {
        ArrayList<StackTrace.StackTraceElement> elements = new ArrayList<>();
        for(int i = stack.size()-1; i >= 0; i--) {
            Call call = stack.get(i);
            TokenPattern<?> calledBy = i < stack.size()-1 ? stack.get(i+1).calledBy : leaf;
            elements.add(new StackTrace.StackTraceElement("at " + (call.calledFile != null ? call.calledFile.getResourceLocation() : "<internal function>") + " ~ " + call.calledFunction, calledBy));
        }
        return new StackTrace(elements);
    }

    public TridentFile getWritingFile() {
        TridentFile prev = null;
        for(Call call : stack) {
            if(prev == null || prev == call.calledFile) prev = call.calledFile;
            else break;
        }
        return prev;
    }
}
