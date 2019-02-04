package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.ContinueException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "continue")
public class ContinueInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String label = null;
        if(pattern.find("CONTINUE_LABEL") != null) {
            label =  pattern.find("CONTINUE_LABEL").flatten(false);
        }
        throw new ContinueException(label, pattern);
    }
}
