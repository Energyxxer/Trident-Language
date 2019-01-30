package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.gamemode.GamemodeCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "gamemode")
public class GamemodeParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Type gamemode = file.getCompiler().getModule().minecraft.types.gamemode.get(pattern.find("GAMEMODE").flatten(false));
        Entity entity = EntityParser.parseEntity(pattern.find("PLAYER.ENTITY"), file);

        try {
            return new GamemodeCommand(gamemode, entity);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, file)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("PLAYER.ENTITY"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("GAMEMODE"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, file);
        }
    }
}
