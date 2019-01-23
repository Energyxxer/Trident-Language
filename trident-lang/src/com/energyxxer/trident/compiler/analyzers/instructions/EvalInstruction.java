package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "eval")
public class EvalInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        InterpolationManager.parse(pattern.find("LINE_SAFE_INTERPOLATION_VALUE"), file);
    }
}
