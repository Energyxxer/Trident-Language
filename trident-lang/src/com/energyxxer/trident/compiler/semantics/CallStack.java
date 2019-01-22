package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.ArrayList;
import java.util.List;
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
            return "at " + calledFile.getResourceLocation() + " :: " + calledFunction + " (" + calledBy.getFile().getName() + ":" + calledBy.getStringLocation().line + ")\n";
        }
    }

    public static class StackTrace {
        private ArrayList<Call> calls;
        private TokenPattern<?> leaf;

        StackTrace(List<Call> calls) {
            this.calls = new ArrayList<>();
            for(int i = calls.size()-1; i >= 0; i--) {
                this.calls.add(calls.get(i));
            }
        }

        public TokenPattern<?> getLeaf() {
            return leaf;
        }

        public StackTrace setLeaf(TokenPattern<?> leaf) {
            this.leaf = leaf;
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < calls.size(); i++) {
                Call called = calls.get(i);
                TokenPattern<?> calling = i - 1 >= 0 ? calls.get(i-1).calledBy : leaf;
                sb.append("    at ");
                sb.append(called.calledFile != null ? called.calledFile.getResourceLocation() : "<internal>");
                sb.append(" ~ ");
                sb.append(called.calledFunction);
                if(calling != null) {
                    sb.append(" (");
                    sb.append(calling.getFile().getName());
                    sb.append(":");
                    sb.append(calling.getStringLocation().line);
                    sb.append(")");
                }
                if(i < calls.size() -1) sb.append("\n");
            }
            return sb.toString();
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

    public StackTrace getView() {
        return new StackTrace(stack);
    }
}
