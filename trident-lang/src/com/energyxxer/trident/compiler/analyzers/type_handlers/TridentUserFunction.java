package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.CallStack;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.stream.Collectors;

public class TridentUserFunction implements TridentFunction {
    private TridentFunctionBranch branch;
    private ISymbolContext declaringContext;
    private Object thisObject;
    private String functionName = "<anonymous function>";

    public TridentUserFunction(TokenPattern<?> functionPattern, ISymbolContext declaringContext, Collection<FormalParameter> formalParameters, Object thisObject, String functionName, TypeConstraints returnConstraints) {
        this.declaringContext = declaringContext;
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;

        this.branch = new TridentUserFunctionBranch(formalParameters, functionPattern, returnConstraints);
    }
    public TridentUserFunction(String functionName, TridentFunctionBranch branch, ISymbolContext declaringContext, Object thisObject) {
        this.branch = branch;
        this.declaringContext = declaringContext;
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> functionPattern = branch.getFunctionPattern();

        if(declaringContext != null) ctx.getCompiler().getCallStack().push(new CallStack.Call(functionName, functionPattern, declaringContext.getStaticParentFile(), pattern));

        try {
            return branch.call(params, patterns, pattern, declaringContext, ctx, thisObject);
        } catch(StackOverflowError x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Stack Overflow Error", pattern, ctx);
        } finally {
            if(declaringContext != null) ctx.getCompiler().getCallStack().pop();
        }
    }

    @Override
    public Object getMember(TridentFunction object, String member, TokenPattern<?> pattern, ISymbolContext file, boolean keepSymbol) {
        if(member.equals("formalParameters")) return new ListObject(branch.getFormalParameters().stream().map(p -> p.getConstraints().getHandler()).collect(Collectors.toList()));
        if(member.equals("declaringFile")) return declaringContext.getStaticParentFile().getResourceLocation();
        throw new MemberNotFoundException();
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setThisObject(Object thisObject) {
        this.thisObject = thisObject;
    }

    @Override
    public String toString() {
        return "<function(" + (branch.getFormalParameters().stream().map(Object::toString).collect(Collectors.joining(", "))) + ")>";
    }

    public TridentFunctionBranch getBranch() {
        return branch;
    }

    public static class FixedThisFunction implements TridentFunction {
        private final TridentUserFunction function;
        private final Object thisObject;

        public FixedThisFunction(TridentUserFunction function, Object thisObject) {
            this.function = function;
            this.thisObject = thisObject;
        }

        @Override
        public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
            function.setThisObject(thisObject);
            Object returnValue = function.call(params, patterns, pattern, ctx);
            function.setThisObject(null);
            return returnValue;
        }
    }
}
