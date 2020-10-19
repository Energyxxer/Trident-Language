package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.List;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class PlaySoundCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"playsound"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("playsound"),
                TridentProductions.resourceLocationFixer,
                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION")).setName("SOUND_EVENT").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.SOUND_RESOURCE).addTags("cspn:Sound"),
                productions.getOrCreateStructure("SOUND_CHANNEL"),
                productions.getOrCreateStructure("ENTITY"),
                optional(
                        productions.getOrCreateStructure("COORDINATE_SET"),
                        TridentProductions.real(productions).setOptional().addTags("cspn:Max Volume"),
                        TridentProductions.real(productions).setOptional().addTags("cspn:Pitch"),
                        TridentProductions.real(productions).setOptional().addTags("cspn:Min Volume")
                )
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        ResourceLocation soundEvent = (ResourceLocation) pattern.find("SOUND_EVENT").evaluate(ctx);
        PlaySoundCommand.Source channel = (PlaySoundCommand.Source) pattern.find("SOUND_CHANNEL").evaluate(ctx);
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx);

        TokenPattern<?> sub = pattern.find("");
        try {
            if (sub != null) {
                CoordinateSet pos = (CoordinateSet) sub.find("COORDINATE_SET").evaluate(ctx);

                float maxVol = 1;
                float pitch = 1;
                float minVol = 0;

                List<TokenPattern<?>> numberArgs = sub.searchByName("REAL");
                if (numberArgs.size() >= 1) {
                    maxVol = (float) (double) numberArgs.get(0).evaluate(ctx);
                    if (numberArgs.size() >= 2) {
                        pitch = (float) (double) numberArgs.get(1).evaluate(ctx);
                        if (numberArgs.size() >= 3) {
                            minVol = (float) (double) numberArgs.get(2).evaluate(ctx);
                        }
                    }
                }

                return new PlaySoundCommand(soundEvent.toString(), channel, entity, pos, maxVol, pitch, minVol);
            } else return new PlaySoundCommand(soundEvent.toString(), channel, entity);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .map("MAX_VOLUME", () -> sub.searchByName("REAL").get(0))
                    .map("PITCH", () -> sub.searchByName("REAL").get(1))
                    .map("MIN_VOLUME", () -> sub.searchByName("REAL").get(2))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", sub, ctx);
        }
    }
}
