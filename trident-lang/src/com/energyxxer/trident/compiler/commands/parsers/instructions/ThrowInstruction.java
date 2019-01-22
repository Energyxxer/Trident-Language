package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "throw")
public class ThrowInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        String message = CommonParsers.parseStringLiteral(pattern.find("STRING"), file);
        throw new TridentException(TridentException.Source.USER_EXCEPTION, message, pattern, file).setBreaking(true);
    }
}
