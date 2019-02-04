package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.gamerule.GameruleQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.gamerule.GameruleSetCommand;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "gamerule")
public class GameruleParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "GAMERULE": {
                return new GameruleQueryCommand(ctx.getCompiler().getModule().minecraft.types.gamerule.get(inner.flatten(false)));
            }
            case "GAMERULE_SETTER": {
                Type gamerule = ctx.getCompiler().getModule().minecraft.types.gamerule.get(inner.find("GAMERULE_ID").flatten(false));
                String value = inner.find("GAMERULE_ARGUMENT").flatten(true);
                return new GameruleSetCommand(gamerule, value);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}
