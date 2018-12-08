package com.energyxxer.trident.compiler.commands.parsers.variable_functions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;

@ParserGroup
public interface VariableFunction {
    Object process(Object value, TokenPattern<?> pattern, TridentCompiler compiler);
}
