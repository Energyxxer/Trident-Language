package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

public class TridentUserMethodBranch {
    private ArrayList<FormalParameter> formalParameters = new ArrayList<>();
    private TokenPattern<?> functionPattern;
    private TypeConstraints returnConstraints;

    public TridentUserMethodBranch(Collection<FormalParameter> formalParameters, TokenPattern<?> functionPattern, TypeConstraints returnConstraints) {
        this.formalParameters.addAll(formalParameters);
        this.functionPattern = functionPattern;
        this.returnConstraints = returnConstraints;
    }

    public ArrayList<FormalParameter> getFormalParameters() {
        return formalParameters;
    }

    public TokenPattern<?> getFunctionPattern() {
        return functionPattern;
    }

    public TypeConstraints getReturnConstraints() {
        return returnConstraints;
    }

    public static TridentUserMethodBranch parseDynamicFunction(TokenPattern<?> pattern, ISymbolContext ctx) {
        TypeConstraints returnConstraints = TypeConstraints.parseConstraints(pattern.find("TYPE_CONSTRAINTS"), ctx);

        ArrayList<FormalParameter> formalParams = new ArrayList<>();
        TokenList paramNames = (TokenList) pattern.find("FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
        if (paramNames != null) {
            for (TokenPattern<?> param : paramNames.searchByName("FORMAL_PARAMETER")) {
                formalParams.add(new FormalParameter(param.find("FORMAL_PARAMETER_NAME").flatten(false), TypeConstraints.parseConstraints(param.find("TYPE_CONSTRAINTS"), ctx)));
            }
        }

        return new TridentUserMethodBranch(formalParams, pattern.find("ANONYMOUS_INNER_FUNCTION"), returnConstraints);
    }
}
