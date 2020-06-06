package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

public abstract class TridentFunctionBranch {
    protected ArrayList<FormalParameter> formalParameters = new ArrayList<>();
    protected TypeConstraints returnConstraints;
    protected boolean shouldCoerce = true;

    public TridentFunctionBranch(Collection<FormalParameter> formalParameters) {
        this.formalParameters.addAll(formalParameters);
    }

    public ArrayList<FormalParameter> getFormalParameters() {
        return formalParameters;
    }

    public abstract TokenPattern<?> getFunctionPattern();

    public static TridentFunctionBranch parseDynamicFunction(TokenPattern<?> pattern, ISymbolContext ctx) {
        TypeConstraints returnConstraints = TypeConstraints.parseConstraints(pattern.find("TYPE_CONSTRAINTS"), ctx);

        ArrayList<FormalParameter> formalParams = new ArrayList<>();
        TokenList paramNames = (TokenList) pattern.find("FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
        if (paramNames != null) {
            for (TokenPattern<?> param : paramNames.searchByName("FORMAL_PARAMETER")) {
                formalParams.add(FormalParameter.create(param, ctx));
            }
        }

        return new TridentUserFunctionBranch(formalParams, pattern.find("ANONYMOUS_INNER_FUNCTION"), returnConstraints);
    }

    public abstract Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext declaringCtx, ISymbolContext callingCtx, Object thisObject);

    public TypeConstraints getReturnConstraints() {
        return returnConstraints;
    }

    public void setReturnConstraints(TypeConstraints returnConstraints) {
        this.returnConstraints = returnConstraints;
    }

    public boolean isShouldCoerce() {
        return shouldCoerce;
    }

    public void setShouldCoerce(boolean shouldCoerce) {
        this.shouldCoerce = shouldCoerce;
    }
}
