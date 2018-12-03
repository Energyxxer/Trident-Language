package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.List;

@ParserMember(key = "playsound")
public class PlaySoundParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TridentUtil.ResourceLocation soundEvent = new TridentUtil.ResourceLocation(pattern.find("RESOURCE_LOCATION").flatten(false));
        PlaySoundCommand.Source channel = PlaySoundCommand.Source.valueOf(pattern.find("CHANNEL").flatten(false).toUpperCase());
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file.getCompiler());

        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            CoordinateSet pos = CoordinateParser.parse(sub.find("COORDINATE_SET"));

            float maxVol = -1;
            float pitch = -1;
            float minVol = -1;

            List<TokenPattern<?>> numberArgs = sub.searchByName("REAL");
            if(numberArgs.size() >= 1) {
                maxVol = Float.parseFloat(numberArgs.get(0).flatten(false));
                if(numberArgs.size() >= 2) {
                    pitch = Float.parseFloat(numberArgs.get(1).flatten(false));
                    if(numberArgs.size() >= 3) {
                        minVol = Float.parseFloat(numberArgs.get(2).flatten(false));
                    }
                }
            }

            return new PlaySoundCommand(soundEvent.toString(), channel, entity, pos, maxVol, pitch, minVol);
        } else return new PlaySoundCommand(soundEvent.toString(), channel, entity);
    }
}
