package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.help.HelpCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "help")
public class HelpParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new HelpCommand(pattern.flattenTokens().get(1).value);
    }
}
