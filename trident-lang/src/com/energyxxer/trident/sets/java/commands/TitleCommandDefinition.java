package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.title.TitleClearCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleResetCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleShowCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleTimesCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TitleCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"title"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("title"),
                productions.getOrCreateStructure("ENTITY"),
                choice(
                        group(enumChoice(TitleShowCommand.Display.class).setName("DISPLAY"), productions.getOrCreateStructure("TEXT_COMPONENT")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) d[1];

                            TitleShowCommand.Display display = (TitleShowCommand.Display) p.find("DISPLAY").evaluate();
                            TextComponent text = (TextComponent) p.find("TEXT_COMPONENT").evaluate(ctx);

                            try {
                                return new TitleShowCommand(entity, display, text);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[2])
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        choice(literal("clear").setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) d[1];
                            try {
                                return new TitleClearCommand(entity);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[2])
                                        .invokeThrow();
                                return null;
                            }
                        }), literal("reset").setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) d[1];
                            try {
                                return new TitleResetCommand(entity);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[2])
                                        .invokeThrow();
                                return null;
                            }
                        })),
                        group(
                                literal("times"),
                                TridentProductions.integer(productions).setName("FADE_IN").addTags("cspn:Fade In"),
                                TridentProductions.integer(productions).setName("STAY").addTags("cspn:Stay"),
                                TridentProductions.integer(productions).setName("FADE_OUT").addTags("cspn:Fade Out")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) d[1];
                            int in = (int) p.find("FADE_IN").evaluate(ctx);
                            int stay = (int) p.find("STAY").evaluate(ctx);
                            int out = (int) p.find("FADE_OUT").evaluate(ctx);
                            try {
                                return new TitleTimesCommand(entity, in, stay, out);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[2])
                                        .map("FADE_IN", p.tryFind("FADE_IN"))
                                        .map("STAY", p.tryFind("STAY"))
                                        .map("FADE_OUT", p.tryFind("FADE_OUT"))
                                        .invokeThrow();
                                return null;
                            }
                        })
                ).setName("INNER")
        ).setSimplificationFunction(d -> {
            ISymbolContext ctx = (ISymbolContext) d.data[0];
            TokenPattern<?> entityPattern = d.pattern.find("ENTITY");
            Entity entity = (Entity) entityPattern.evaluate(ctx);

            d.data = new Object[]{ctx, entity, entityPattern};
            d.pattern = d.pattern.find("INNER");
        });
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
