package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceAddCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceSetCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ExperienceCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"experience", "xp"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        TokenPatternMatch unitMatch = enumChoice(ExperienceCommand.Unit.class).setName("UNIT").setOptional();

        TokenGroupMatch g = new TokenGroupMatch();
        g.append(choice(
                group(literal("add"), productions.getOrCreateStructure("ENTITY"), TridentProductions.integer(productions).addTags("cspn:Amount"), unitMatch).setName("ADD"),
                group(literal("set"), productions.getOrCreateStructure("ENTITY"), TridentProductions.integer(productions).addTags("cspn:Amount"), unitMatch).setName("SET"),
                group(literal("query"), productions.getOrCreateStructure("ENTITY"), unitMatch).setName("QUERY")
        ).setName("SUBCOMMAND"));
        return group(
                choice(TridentProductions.commandHeader("experience"), TridentProductions.commandHeader("xp")),
                choice(
                        group(literal("add"), productions.getOrCreateStructure("ENTITY"), TridentProductions.integer(productions).setName("AMOUNT").addTags("cspn:Amount"), unitMatch).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity player = (Entity) p.find("ENTITY").evaluate(ctx, null);
                            int amount = (int) p.find("AMOUNT").evaluate(ctx, null);
                            ExperienceCommand.Unit unit = (ExperienceCommand.Unit) p.findThenEvaluate("UNIT", ExperienceCommand.Unit.POINTS, ctx, null);
                            return new ExperienceAddCommand(player, amount, unit);
                        }),
                        group(literal("set"), productions.getOrCreateStructure("ENTITY"), TridentProductions.integer(productions).setName("AMOUNT").addTags("cspn:Amount"), unitMatch).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity player = (Entity) p.find("ENTITY").evaluate(ctx, null);
                            int amount = (int) p.find("AMOUNT").evaluate(ctx, null);
                            ExperienceCommand.Unit unit = (ExperienceCommand.Unit) p.findThenEvaluate("UNIT", ExperienceCommand.Unit.POINTS, ctx, null);
                            return new ExperienceSetCommand(player, amount, unit);
                        }),
                        group(literal("query"), productions.getOrCreateStructure("ENTITY"), unitMatch).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity player = (Entity) p.find("ENTITY").evaluate(ctx, null);
                            ExperienceCommand.Unit unit = (ExperienceCommand.Unit) p.findThenEvaluate("UNIT", ExperienceCommand.Unit.POINTS, ctx, null);
                            return new ExperienceQueryCommand(player, unit);
                        })
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
