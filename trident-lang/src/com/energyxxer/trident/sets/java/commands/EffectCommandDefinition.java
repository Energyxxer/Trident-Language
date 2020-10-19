package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.effect.EffectClearCommand;
import com.energyxxer.commodore.functionlogic.commands.effect.EffectGiveCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.StatusEffect;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class EffectCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"effect"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("effect"),
                choice(
                        group(
                                literal("clear"),
                                optional(
                                        TridentProductions.sameLine(),
                                        productions.getOrCreateStructure("ENTITY"),
                                        wrapperOptional(productions.getOrCreateStructure("EFFECT_ID")).setName("EFFECT_TO_CLEAR")
                                ).setName("INNER")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];

                            Entity entity = (Entity) p.findThenEvaluate("INNER.ENTITY", null, ctx);
                            Type effectToClear = (Type) p.findThenEvaluate("INNER.EFFECT_TO_CLEAR", null, ctx);
                            return new EffectClearCommand(entity, effectToClear);
                        }),
                        group(
                                literal("give"),
                                productions.getOrCreateStructure("ENTITY"),
                                productions.getOrCreateStructure("EFFECT_ID"),
                                optional(
                                        TridentProductions.integer(productions).setName("DURATION").addTags("cspn:Duration (seconds)"),
                                        optional(
                                                TridentProductions.integer(productions).setName("AMPLIFIER").addTags("cspn:Amplifier"),
                                                TridentProductions.rawBoolean().setOptional().setName("HIDE_PARTICLES").addTags(SuggestionTags.ENABLED, "cspn:Hide Particles?")
                                        ).setName("INNER")
                                ).setName("INNER")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];

                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                            Type effectToGive = (Type) p.find("EFFECT_ID").evaluate(ctx);

                            StatusEffect effect = new StatusEffect(effectToGive);

                            int duration = (int) p.findThenEvaluate("INNER.DURATION", StatusEffect.DEFAULT_DURATION / 20, ctx) * 20;
                            try {
                                effect.setDuration(duration);
                            } catch (CommodoreException ex) {
                                TridentExceptionUtil.handleCommodoreException(ex, p, ctx)
                                        .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, p.tryFind("INNER.DURATION"))
                                        .invokeThrow();
                            }

                            int amplifier = (int) p.findThenEvaluate("INNER.INNER.AMPLIFIER", StatusEffect.DEFAULT_AMPLIFIER, ctx);
                            try {
                                effect.setAmplifier(amplifier);
                            } catch (CommodoreException ex) {
                                TridentExceptionUtil.handleCommodoreException(ex, p, ctx)
                                        .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, p.tryFind("INNER.INNER.AMPLIFIER"))
                                        .invokeThrow();
                            }

                            boolean hideParticles = (boolean) p.findThenEvaluate("INNER.INNER.HIDE_PARTICLES", false, ctx);
                            effect.setVisibility(hideParticles ? StatusEffect.ParticleVisibility.HIDDEN : StatusEffect.ParticleVisibility.VISIBLE);

                            return new EffectGiveCommand(entity, effect);
                        })
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
