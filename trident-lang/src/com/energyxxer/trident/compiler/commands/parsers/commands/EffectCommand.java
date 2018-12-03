package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.effect.EffectClearCommand;
import com.energyxxer.commodore.functionlogic.commands.effect.EffectGiveCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.StatusEffect;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.trident.compiler.util.Using.using;

@ParserMember(key = "effect")
public class EffectCommand implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "CLEAR": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file.getCompiler());
                Type effect = CommonParsers.parseType(inner.find(".EFFECT_ID"), file.getCompiler(), d->d.effect);
                return new EffectClearCommand(entity, effect);
            }
            case "GIVE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file.getCompiler());
                StatusEffect effect = new StatusEffect(CommonParsers.parseType(inner.find("EFFECT_ID"), file.getCompiler(), d->d.effect));
                using(inner.find(".DURATION")).notIfNull().run(p -> effect.setDuration(20 * Integer.parseInt(p.flatten(false))));
                using(inner.find("..AMPLIFIER")).notIfNull().run(p -> effect.setAmplifier(Integer.parseInt(p.flatten(false))));
                using(inner.find("..HIDE_PARTICLES")).notIfNull().run(p -> effect.setVisibility(p.flatten(false).equals("true") ? StatusEffect.ParticleVisibility.HIDDEN : StatusEffect.ParticleVisibility.VISIBLE));
                return new EffectGiveCommand(entity, effect);
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }
}
