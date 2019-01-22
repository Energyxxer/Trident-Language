package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.ContinueException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "continue")
public class ContinueInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        String label = null;
        if(pattern.find("CONTINUE_LABEL") != null) {
            label =  pattern.find("CONTINUE_LABEL").flatten(false);
        }
        throw new ContinueException(label, pattern);
    }
}
