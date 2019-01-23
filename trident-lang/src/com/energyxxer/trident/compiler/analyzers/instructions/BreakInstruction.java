package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.BreakException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "break")
public class BreakInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        String label = null;
        if(pattern.find("BREAK_LABEL") != null) {
            label =  pattern.find("BREAK_LABEL").flatten(false);
        }
        throw new BreakException(label, pattern);
    }
}
