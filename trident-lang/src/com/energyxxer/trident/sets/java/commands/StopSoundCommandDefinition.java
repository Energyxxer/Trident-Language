package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.commands.stopsound.StopSoundCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.symbol;

public class StopSoundCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"stopsound"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("stopsound"),
                productions.getOrCreateStructure("ENTITY"),
                choice(
                        group(
                                productions.getOrCreateStructure("SOUND_CHANNEL"),
                                TridentProductions.resourceLocationFixer,
                                optional(
                                        TridentProductions.sameLine(),
                                        productions.getOrCreateStructure("RESOURCE_LOCATION")
                                ).setSimplificationFunctionContentIndex(1).setName("SOUND_RESOURCE").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.RESOURCE, TridentSuggestionTags.SOUND_RESOURCE).addTags("cspn:Sound")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) d[1];

                            PlaySoundCommand.Source soundChannel = (PlaySoundCommand.Source) p.find("SOUND_CHANNEL").evaluate(ctx);
                            ResourceLocation sound = (ResourceLocation) p.findThenEvaluate("SOUND_RESOURCE", null, ctx);

                            return new StopSoundCommand(entity, soundChannel, sound != null ? sound.toString() : null);
                        }),
                        group(
                                symbol("*"),
                                TridentProductions.sameLine(),
                                TridentProductions.resourceLocationFixer,
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION")).setName("SOUND_RESOURCE").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.RESOURCE, TridentSuggestionTags.SOUND_RESOURCE).addTags("cspn:Sound")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) d[1];
                            ResourceLocation sound = (ResourceLocation) p.find("SOUND_RESOURCE").evaluate(ctx);
                            return new StopSoundCommand(entity, sound.toString());
                        })
                ).setOptional().setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx);

        try {
            return (Command) pattern.findThenEvaluateLazyDefault("INNER", () -> new StopSoundCommand(entity), ctx, entity);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
