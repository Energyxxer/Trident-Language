package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.commands.stopsound.StopSoundCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "stopsound")
public class StopSoundParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);

        TokenPattern<?> inner = pattern.find("CHOICE");
        if(inner != null) {
            inner = ((TokenStructure) inner).getContents();
            switch(inner.getName()) {
                case "STOP_BY_CHANNEL": {
                    PlaySoundCommand.Source channel = PlaySoundCommand.Source.valueOf(inner.find("CHANNEL").flatten(false).toUpperCase());
                    TokenPattern<?> rawResource = inner.find("RESOURCE_LOCATION");
                    if(rawResource != null) {
                        return new StopSoundCommand(entity, channel, new TridentUtil.ResourceLocation(rawResource.flatten(false)).toString());
                    } else {
                        return new StopSoundCommand(entity, channel);
                    }
                }
                case "STOP_BY_EVENT": {
                    return new StopSoundCommand(entity, new TridentUtil.ResourceLocation(inner.find("RESOURCE_LOCATION").flatten(false)).toString());
                }
                default: {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                    return null;
                }
            }
        }
        return new StopSoundCommand(entity);
    }
}
