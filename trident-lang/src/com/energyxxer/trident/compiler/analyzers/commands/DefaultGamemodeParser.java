package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.defaultgamemode.DefaultGamemodeCommand;
import com.energyxxer.commodore.types.defaults.GamemodeType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "defaultgamemode")
public class DefaultGamemodeParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new DefaultGamemodeCommand(CommonParsers.parseType(pattern.find("GAMEMODE.GAMEMODE_ID"), ctx, GamemodeType.CATEGORY));
    }
}