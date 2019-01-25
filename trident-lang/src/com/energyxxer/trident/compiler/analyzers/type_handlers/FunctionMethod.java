package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class FunctionMethod implements VariableTypeHandler<FunctionMethod>, VariableMethod {
    private TokenPattern<?> functionPattern;
    private TridentFile declaringFile;
    private ArrayList<String> formalParameters = new ArrayList<>();
    private Object thisObject;
    private String functionName = "<anonymous function>";

    public FunctionMethod(TokenPattern<?> functionPattern, TridentFile declaringFile, Collection<String> formalParameters, Object thisObject, String functionName) {
        this.functionPattern = functionPattern;
        this.declaringFile = declaringFile;
        this.formalParameters.addAll(formalParameters);
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentFile file) {
        SymbolTable innerFrame = new SymbolTable(file);
        file.getCompiler().getSymbolStack().push(innerFrame);
        file.getCompiler().getCallStack().push(new CallStack.Call(functionName, functionPattern, declaringFile, pattern));

        for(int i = 0; i < formalParameters.size(); i++) {
            innerFrame.put(new Symbol(formalParameters.get(i), Symbol.SymbolAccess.LOCAL, i < params.length ? params[i] : null));
        }
        innerFrame.put(new Symbol("this", Symbol.SymbolAccess.LOCAL, thisObject));

        try {
            TridentFile.resolveInnerFileIntoSection(functionPattern, file, file.getFunction());
        } catch(StackOverflowError x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Stack Overflow Error", pattern, file);
        } catch(ReturnException x) {
            return x.getValue();
        } finally {
            file.getCompiler().getSymbolStack().pop();
            file.getCompiler().getCallStack().pop();
        }
        return null;
    }

    @Override
    public Object getMember(FunctionMethod object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("formalParameters")) return new ListType(formalParameters);
        if(member.equals("definingFile")) return declaringFile.getResourceLocation();
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(FunctionMethod object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(FunctionMethod object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return "<function(" + formalParameters.stream().collect(Collectors.joining(", ")) + ")>";
    }
}
