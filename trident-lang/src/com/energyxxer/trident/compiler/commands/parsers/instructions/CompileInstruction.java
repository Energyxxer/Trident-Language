package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.compilation.CompilationBlock;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "compile")
public class CompileInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = pattern.find("COMPILE_BLOCK_INNER");
        if(inner != null) {
            CompilationBlock block = new CompilationBlock(inner, file);
            block.execute();
        }
    }
}
