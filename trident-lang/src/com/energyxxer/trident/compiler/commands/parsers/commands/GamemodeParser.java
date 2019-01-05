package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.gamemode.GamemodeCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "gamemode")
public class GamemodeParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Type gamemode = file.getCompiler().getModule().minecraft.types.gamemode.get(pattern.find("GAMEMODE").flatten(false));
        Entity entity = EntityParser.parseEntity(pattern.find("PLAYER.ENTITY"), file);

        return new GamemodeCommand(gamemode, entity);
    }
}
