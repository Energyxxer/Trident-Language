package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;

@AnalyzerGroup
public interface CommandParser {
    Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx);
}
