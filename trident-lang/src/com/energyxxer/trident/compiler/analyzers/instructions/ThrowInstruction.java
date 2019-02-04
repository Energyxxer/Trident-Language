package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "throw")
public class ThrowInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = InterpolationManager.parse(pattern.find("LINE_SAFE_INTERPOLATION_VALUE"), ctx, String.class);
        throw new TridentException(TridentException.Source.USER_EXCEPTION, message, pattern, ctx).setBreaking(true);
    }
}
