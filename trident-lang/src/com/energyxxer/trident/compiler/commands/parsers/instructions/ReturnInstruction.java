package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.ReturnException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "return")
public class ReturnInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        throw new ReturnException(pattern, InterpolationManager.parse(pattern.find("RETURN_VALUE.LINE_SAFE_INTERPOLATION_VALUE"), file));
    }
}
