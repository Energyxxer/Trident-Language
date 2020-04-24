package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class TridentUserMethod implements TridentMethod {
    private TokenPattern<?> functionPattern;
    private ISymbolContext declaringContext;
    private ArrayList<FormalParameter> formalParameters = new ArrayList<>();
    private Object thisObject;
    private String functionName = "<anonymous function>";
    private TypeConstraints returnConstraints;

    public TridentUserMethod(TokenPattern<?> functionPattern, ISymbolContext declaringContext, Collection<FormalParameter> formalParameters, Object thisObject, String functionName) {
        this.functionPattern = functionPattern;
        this.declaringContext = declaringContext;
        this.formalParameters.addAll(formalParameters);
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {

        SymbolContext innerFrame = new SymbolContext(declaringContext);

        ctx.getCompiler().getCallStack().push(new CallStack.Call(functionName, functionPattern, declaringContext.getStaticParentFile(), pattern));

        for(int i = 0; i < formalParameters.size(); i++) {
            FormalParameter param = formalParameters.get(i);
            Symbol sym = new Symbol(param.getName(), Symbol.SymbolVisibility.PRIVATE);
            sym.setTypeConstraints(param.getConstraints());
            sym.safeSetValue(i < params.length ? params[i] : null, i < params.length ? patterns[i] : pattern, ctx);
            innerFrame.put(sym);
        }
        if(thisObject != null) innerFrame.put(new Symbol("this", Symbol.SymbolVisibility.PRIVATE, thisObject));

        try {
            TridentFile.resolveInnerFileIntoSection(functionPattern, innerFrame, ctx.getWritingFile().getFunction());
        } catch(StackOverflowError x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Stack Overflow Error", pattern, ctx);
        } catch(ReturnException x) {
            Object returnValue = x.getValue();
            if(returnConstraints != null) {
                returnConstraints.validate(returnValue, x.getPattern(), ctx);
            }
            return returnValue;
        } finally {
            ctx.getCompiler().getCallStack().pop();
        }
        if(returnConstraints != null) {
            TokenPattern<?>[] innerFunctContent = ((TokenGroup)((TokenStructure)functionPattern).getContents()).getContents();
            TokenPattern<?> closingBracePattern = innerFunctContent[innerFunctContent.length-1];
            returnConstraints.validate(null, closingBracePattern, ctx);
        }
        return null;
    }

    @Override
    public Object getMember(TridentMethod object, String member, TokenPattern<?> pattern, ISymbolContext file, boolean keepSymbol) {
        if(member.equals("formalParameters")) return new ListObject(formalParameters);
        if(member.equals("declaringFile")) return declaringContext.getStaticParentFile().getResourceLocation();
        throw new MemberNotFoundException();
    }

    @Override
    public String toString() {
        return "<function(" + formalParameters.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")>";
    }

    public void setReturnConstraints(TypeConstraints returnConstraints) {
        this.returnConstraints = returnConstraints;
    }
}
