package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.commands.stopsound.StopSoundCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "stopsound")
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
                    TokenPattern<?> rawResource = inner.find("SOUND_RESOURCE.RESOURCE_LOCATION");
                    TridentUtil.ResourceLocation soundResource = CommonParsers.parseResourceLocation(rawResource, file);
                    if(soundResource != null) {
                        soundResource.assertStandalone(rawResource, file);
                        return new StopSoundCommand(entity, channel, soundResource.toString());
                    } else {
                        return new StopSoundCommand(entity, channel);
                    }
                }
                case "STOP_BY_EVENT": {
                    TokenPattern<?> rawResource = inner.find("RESOURCE_LOCATION");
                    TridentUtil.ResourceLocation soundResource = CommonParsers.parseResourceLocation(rawResource, file);
                    soundResource.assertStandalone(rawResource, file);
                    return new StopSoundCommand(entity, rawResource.toString());
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
