package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.Collections;

public interface SimpleCommandParser extends CommandParser {
    Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx);

    default Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> preModifiers) {
        Command result = parseSimple(pattern, ctx);
        if(result != null) return Collections.singletonList(result);
        else return Collections.emptyList();
    }
}
