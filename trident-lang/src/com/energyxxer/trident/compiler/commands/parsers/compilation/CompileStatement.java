package com.energyxxer.trident.compiler.commands.parsers.compilation;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;

@ParserGroup
public interface CompileStatement {
    void execute(TokenPattern<?> pattern, CompilationBlock context);
}
