package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

import java.util.List;

@AnalyzerMember(key = "playsound")
public class PlaySoundParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentUtil.ResourceLocation soundEvent = CommonParsers.parseResourceLocation(pattern.find("SOUND_EVENT.RESOURCE_LOCATION"), ctx);
        soundEvent.assertStandalone(pattern, ctx);
        PlaySoundCommand.Source channel = PlaySoundCommand.Source.valueOf(pattern.find("CHANNEL").flatten(false).toUpperCase());
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);

        TokenPattern<?> sub = pattern.find("");
        try {
            if(sub != null) {
                CoordinateSet pos = CoordinateParser.parse(sub.find("COORDINATE_SET"), ctx);

                float maxVol = 1;
                float pitch = 1;
                float minVol = 0;

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
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY"))
                    .map("MAX_VOLUME", () -> sub.searchByName("REAL").get(0))
                    .map("PITCH", () -> sub.searchByName("REAL").get(1))
                    .map("MIN_VOLUME", () -> sub.searchByName("REAL").get(2))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", sub, ctx);
        }
    }
}
