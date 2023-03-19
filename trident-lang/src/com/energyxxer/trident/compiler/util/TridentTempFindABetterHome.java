package com.energyxxer.trident.compiler.util;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.functions.FormalParameter;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunctionBranch;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunctionBranch;

import java.util.ArrayList;

public class TridentTempFindABetterHome {
    public static PrismarineFunctionBranch parseDynamicFunction(TokenPattern<?> pattern, ISymbolContext ctx) {
        TypeConstraints returnConstraints = (TypeConstraints) pattern.find("PRE_CODE_BLOCK.TYPE_CONSTRAINTS").evaluate(ctx, null);

        ArrayList<FormalParameter> formalParams = new ArrayList<>();
        TokenList paramNames = (TokenList) pattern.find("PRE_CODE_BLOCK.FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
        if (paramNames != null) {
            for (TokenPattern<?> param : paramNames.getContentsExcludingSeparators()) {
                formalParams.add(createFormalParam(param, ctx));
            }
        }

        return new TridentUserFunctionBranch(ctx.getTypeSystem(), formalParams, pattern.find("ANONYMOUS_INNER_FUNCTION"), returnConstraints);
    }

    public static FormalParameter createFormalParam(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new FormalParameter(pattern.find("FORMAL_PARAMETER_NAME").flatten(false), (TypeConstraints) pattern.find("TYPE_CONSTRAINTS").evaluate(ctx, null));
    }

    public enum SymbolModifier {
        STATIC(0b1), FINAL(0b10), VIRTUAL(0b100);
        private final int bit;
        SymbolModifier(int bit) {
            this.bit = bit;
        }

        public int getBit() {
            return bit;
        }
    }
}
