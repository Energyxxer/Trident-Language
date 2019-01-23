package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "throw")
public class ThrowInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        String message = CommonParsers.parseStringLiteral(pattern.find("STRING"), file);
        throw new TridentException(TridentException.Source.USER_EXCEPTION, message, pattern, file).setBreaking(true);
    }
}
