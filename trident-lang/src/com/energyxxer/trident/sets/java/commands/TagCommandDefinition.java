package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.commands.tag.TagQueryCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.commands.CommandDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.CUSTOM_COMMAND_KEYWORD;

public class TagCommandDefinition implements CommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"tag"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("tag"),
                productions.getOrCreateStructure("ENTITY"),
                choice(
                        literal("list").setEvaluator((p, d) -> {
                            Entity entity = (Entity) d[1];
                            return Collections.singletonList(new TagQueryCommand(entity));
                        }),
                        group(literal("add"), TridentProductions.noToken().addTags("cspn:Tag"), wrapper(TridentProductions.identifierA(productions)).setName("TAG"))
                                .setEvaluator((p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    Entity entity = (Entity) d[1];

                                    String tag = (String) p.find("TAG").evaluate(ctx);
                                    return Collections.singletonList(new TagCommand(TagCommand.Action.ADD, entity, tag));
                                }),
                        group(literal("remove"), TridentProductions.noToken().addTags("cspn:Tag"), wrapper(TridentProductions.identifierA(productions)).setName("TAG"))
                                .setEvaluator((p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    Entity entity = (Entity) d[1];

                                    String tag = (String) p.find("TAG").evaluate(ctx);
                                    return Collections.singletonList(new TagCommand(TagCommand.Action.REMOVE, entity, tag));
                                }),
                        group(matchItem(CUSTOM_COMMAND_KEYWORD, "update"), TridentProductions.noToken().addTags("cspn:Tag"), wrapper(TridentProductions.identifierA(productions)).setName("TAG"))
                                .setEvaluator((p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    Entity entity = (Entity) d[1];

                                    TridentUtil.assertLanguageLevel(ctx, 2, "The tag-update subcommand is", p);

                                    String tag = (String) p.find("TAG").evaluate(ctx);

                                    ArrayList<Command> commands = new ArrayList<>();
                                    Entity removeFrom = entity instanceof Selector ? new Selector(((Selector) entity).getBase()) : new Selector(Selector.BaseSelector.ALL_PLAYERS);
                                    commands.add(new TagCommand(TagCommand.Action.REMOVE, removeFrom, tag));
                                    commands.add(new TagCommand(TagCommand.Action.ADD, entity, tag));
                                    return commands;
                                })
                ).setName("INNER")
        ).setSimplificationFunction(d -> {
            ISymbolContext ctx = (ISymbolContext) d.data[0];
            Entity entity = (Entity) d.pattern.find("ENTITY").evaluate(ctx);
            d.pattern = d.pattern.find("INNER");
            d.data = new Object[] {ctx, entity};
        });
    }

    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> modifiers) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
