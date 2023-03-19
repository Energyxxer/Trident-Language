package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.title.TitleClearCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleResetCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleShowCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleTimesCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TitleCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"title"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("title"),
                productions.getOrCreateStructure("ENTITY"),
                choice(
                        group(enumChoice(TitleShowCommand.Display.class).setName("DISPLAY"), productions.getOrCreateStructure("TEXT_COMPONENT")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity entity = (Entity) d[0];

                            TitleShowCommand.Display display = (TitleShowCommand.Display) p.find("DISPLAY").evaluate(ctx, null);
                            TextComponent text = (TextComponent) p.find("TEXT_COMPONENT").evaluate(ctx, null);

                            try {
                                return new TitleShowCommand(entity, display, text);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[1])
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        choice(literal("clear").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity entity = (Entity) d[0];
                            try {
                                return new TitleClearCommand(entity);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[1])
                                        .invokeThrow();
                                return null;
                            }
                        }), literal("reset").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity entity = (Entity) d[0];
                            try {
                                return new TitleResetCommand(entity);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[1])
                                        .invokeThrow();
                                return null;
                            }
                        })),
                        group(
                                literal("times"),
                                TridentProductions.integer(productions).setName("FADE_IN").addTags("cspn:Fade In"),
                                TridentProductions.integer(productions).setName("STAY").addTags("cspn:Stay"),
                                TridentProductions.integer(productions).setName("FADE_OUT").addTags("cspn:Fade Out")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity entity = (Entity) d[0];
                            int in = (int) p.find("FADE_IN").evaluate(ctx, null);
                            int stay = (int) p.find("STAY").evaluate(ctx, null);
                            int out = (int) p.find("FADE_OUT").evaluate(ctx, null);
                            try {
                                return new TitleTimesCommand(entity, in, stay, out);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, (TokenPattern<?>) d[1])
                                        .map("FADE_IN", p.tryFind("FADE_IN"))
                                        .map("STAY", p.tryFind("STAY"))
                                        .map("FADE_OUT", p.tryFind("FADE_OUT"))
                                        .invokeThrow();
                                return null;
                            }
                        })
                ).setName("INNER")
        ).setSimplificationFunction(d -> {
            TokenPattern<?> pattern = d.pattern;
            ISymbolContext ctx = (ISymbolContext) d.ctx;

            d.unlock(); d = null;
            TokenPattern<?> entityPattern = pattern.find("ENTITY");
            Entity entity = (Entity) entityPattern.evaluate(ctx, null);

            TokenPattern.SimplificationDomain.get(pattern.find("INNER"), ctx, new Object[] {entity, entityPattern});
        });
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
