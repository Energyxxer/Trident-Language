package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.effect.EffectClearCommand;
import com.energyxxer.commodore.functionlogic.commands.effect.EffectGiveCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.StatusEffect;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.trident.compiler.util.Using.using;

@AnalyzerMember(key = "effect")
public class EffectParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "CLEAR": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                Type effect = CommonParsers.parseType(inner.find(".EFFECT_ID"), file, d->d.effect);
                return new EffectClearCommand(entity, effect);
            }
            case "GIVE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                StatusEffect effect = new StatusEffect(CommonParsers.parseType(inner.find("EFFECT_ID"), file, d->d.effect));
                using(inner.find(".DURATION")).notIfNull()
                        .except(CommodoreException.class,
                                (ex, obj) -> TridentException.handleCommodoreException((CommodoreException) ex, inner, file)
                                        .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, inner.find(".DURATION"))
                                        .invokeThrow()
                        )
                        .run(p -> effect.setDuration(20 * CommonParsers.parseInt(p, file)));
                using(inner.find("..AMPLIFIER")).notIfNull()
                        .except(CommodoreException.class,
                                (ex, obj) -> TridentException.handleCommodoreException((CommodoreException) ex, inner, file)
                                        .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, inner.find(".DURATION"))
                                        .invokeThrow()
                        )
                        .run(p -> effect.setAmplifier(CommonParsers.parseInt(p, file)));
                using(inner.find("..HIDE_PARTICLES")).notIfNull().run(p -> effect.setVisibility(p.flatten(false).equals("true") ? StatusEffect.ParticleVisibility.HIDDEN : StatusEffect.ParticleVisibility.VISIBLE));
                return new EffectGiveCommand(entity, effect);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }
}
