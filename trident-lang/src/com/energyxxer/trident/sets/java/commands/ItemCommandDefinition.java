package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.item.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.ItemModifier;
import com.energyxxer.enxlex.lexical_analysis.inspections.ReplacementInspectionAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.SuggestionInspection;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.util.StringBounds;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ItemCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"item"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        TokenPatternMatch ITEM_HOLDER = choice(
                group(literal("block"), productions.getOrCreateStructure("COORDINATE_SET"), productions.getOrCreateStructure("SLOT_ID")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                    Type slot = (Type) p.find("SLOT_ID").evaluate(ctx);
                    return new ItemHolderBlock(pos, slot);
                }),
                group(literal("entity"), productions.getOrCreateStructure("ENTITY"), productions.getOrCreateStructure("SLOT_ID")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                    Type slot = (Type) p.find("SLOT_ID").evaluate(ctx);
                    return new ItemHolderEntity(entity, slot);
                })
        ).setName("ITEM_HOLDER");

        return TridentProductions.versionLimited(productions.worker, "command.item", false, group(
                TridentProductions.commandHeader("item"),
                ITEM_HOLDER,
                choice(
                        group(literal("copy"), ITEM_HOLDER).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ItemHolder to = (ItemHolder) d[1];

                            ItemHolder from = (ItemHolder) p.find("ITEM_HOLDER").evaluate(ctx);

                            try {
                                return new ItemCopyCommand(to, from);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ITEM_HOLDER.ENTITY"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }
                        }).setName("ITEM_COPY_COMMAND"),
                        group(literal("replace"), productions.getOrCreateStructure("ITEM"), TridentProductions.integer(productions).setOptional().setName("COUNT").addTags("cspn:Count")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ItemHolder target = (ItemHolder) d[1];

                            Item item = (Item) p.find("ITEM").evaluate(ctx, NBTMode.SETTING, false);
                            int count = (int) p.findThenEvaluate("COUNT", 1, ctx);

                            try {
                                return new ItemReplaceCommand(target, item, count);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ITEM_HOLDER.ENTITY"))
                                        .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, p.tryFind("COUNT"))
                                        .map(CommodoreException.Source.TYPE_ERROR, p.tryFind("ITEM"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }
                        }).setName("ITEM_REPLACE_COMMAND"),
                        group(literal("modify"), productions.getOrCreateStructure("RESOURCE_LOCATION")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ItemHolder target = (ItemHolder) d[1];

                            ResourceLocation id = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx);
                            ItemModifier ref = new ItemModifier(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

                            try {
                                return new ItemModifyCommand(target, ref);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.TYPE_ERROR, p.tryFind("RESOURCE_LOCATION"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }
                        }).setName("ITEM_MODIFY_COMMAND")
                ).setName("SUBCOMMAND")
            ).setSimplificationFunction(d -> {
                ISymbolContext ctx = (ISymbolContext) d.data[0];

                ItemHolder holder = (ItemHolder) d.pattern.find("ITEM_HOLDER").evaluate(ctx);

                d.pattern = d.pattern.find("SUBCOMMAND");
                d.data = new Object[] {ctx, holder};
            })
                .addProcessor((p, l) -> {
                    if(l.getInspectionModule() != null && ((TokenStructure) p.find("SUBCOMMAND")).getContents().getName().equals("ITEM_REPLACE_COMMAND")) {

                        StringBounds bounds = p.getStringBounds();

                        StringBounds replaceLiteralBounds = p.find("SUBCOMMAND.LITERAL_REPLACE").getStringBounds();

                        SuggestionInspection inspection = new SuggestionInspection("Convert to replaceitem command")
                                .setStartIndex(bounds.start.index)
                                .setEndIndex(bounds.end.index)
                                .addAction(
                                        new ReplacementInspectionAction()
                                                .setReplacementStartIndex(replaceLiteralBounds.start.index-1)
                                                .setReplacementEndIndex(replaceLiteralBounds.end.index)
                                                .setReplacementText("")
                                )
                                .addAction(
                                        new ReplacementInspectionAction()
                                                .setReplacementStartIndex(bounds.start.index)
                                                .setReplacementEndIndex(bounds.start.index + "item".length())
                                                .setReplacementText("replaceitem")
                                );

                        l.getInspectionModule().addInspection(inspection);
                    }
                })
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
