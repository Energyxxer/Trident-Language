package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.item.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.ItemModifier;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeChainAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeReplacementAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.Inspection;
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
import static com.energyxxer.trident.compiler.TridentProductions.resourceLocationFixer;
import static com.energyxxer.trident.compiler.TridentProductions.sameLine;

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
                choice(
                        group(
                                ITEM_HOLDER,
                                choice(
                                        group(literal("copy"), ITEM_HOLDER, optional(sameLine(), resourceLocationFixer, productions.getOrCreateStructure("RESOURCE_LOCATION")).setName("COPY_MODIFIER").setSimplificationFunctionContentIndex(1).addTags("cspn:Item Modifier")).setEvaluator((p, d) -> {
                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                            ItemHolder to = (ItemHolder) d[1];

                                            ItemHolder from = (ItemHolder) p.find("ITEM_HOLDER").evaluate(ctx);

                                            ResourceLocation modifierId = (ResourceLocation) p.findThenEvaluate("COPY_MODIFIER", null, ctx);
                                            ItemModifier modifier = modifierId != null ? new ItemModifier(ctx.get(SetupModuleTask.INSTANCE).getNamespace(modifierId.namespace), modifierId.body) : null;

                                            try {
                                                return new ItemCopyCommand(to, from, modifier);
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
                        }).setName("OLD_ITEM_SYNTAX"),
                        group(
                                literal("modify"), ITEM_HOLDER, resourceLocationFixer, productions.getOrCreateStructure("RESOURCE_LOCATION")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ItemHolder target = (ItemHolder) p.find("ITEM_HOLDER").evaluate(ctx);

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
                        }).setName("NEW_ITEM_MODIFY"),
                        group(
                                literal("replace"), ITEM_HOLDER, choice(
                                        group(literal("with"), productions.getOrCreateStructure("ITEM"), TridentProductions.integer(productions).setOptional().setName("COUNT").addTags("cspn:Count")).setEvaluator((p, d) -> {
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
                                        }).setName("ITEM_REPLACE_WITH_COMMAND"),
                                        group(literal("from"), ITEM_HOLDER, optional(sameLine(), resourceLocationFixer, productions.getOrCreateStructure("RESOURCE_LOCATION")).setSimplificationFunctionContentIndex(1).setName("COPY_MODIFIER")).setEvaluator((p, d) -> {
                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                            ItemHolder to = (ItemHolder) d[1];

                                            ItemHolder from = (ItemHolder) p.find("ITEM_HOLDER").evaluate(ctx);

                                            ResourceLocation modifierId = (ResourceLocation) p.findThenEvaluate("COPY_MODIFIER", null, ctx);
                                            ItemModifier modifier = modifierId != null ? new ItemModifier(ctx.get(SetupModuleTask.INSTANCE).getNamespace(modifierId.namespace), modifierId.body) : null;

                                            try {
                                                return new ItemCopyCommand(to, from, modifier);
                                            } catch (CommodoreException x) {
                                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ITEM_HOLDER.ENTITY"))
                                                        .invokeThrow();
                                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                                            }
                                        }).setName("ITEM_REPLACE_FROM_COMMAND")
                                ).setName("SUBCOMMAND")
                        ).setSimplificationFunction(d -> {
                            ISymbolContext ctx = (ISymbolContext) d.data[0];

                            ItemHolder holder = (ItemHolder) d.pattern.find("ITEM_HOLDER").evaluate(ctx);

                            d.pattern = d.pattern.find("SUBCOMMAND");
                            d.data = new Object[] {ctx, holder};
                        }).setName("NEW_ITEM_REPLACE")
                ).setName("SUBCOMMAND")
            ).setSimplificationFunctionContentIndex(1)
                .addProcessor((p, l) -> {
                    if(l.getInspectionModule() != null) {
                        StringBounds bounds = p.getStringBounds();
                        TokenPattern<?> commandType = ((TokenStructure) p.find("SUBCOMMAND")).getContents();
                        StringBounds subCommandBounds = commandType.getStringBounds();

                        switch(commandType.getName()) {
                            case "OLD_ITEM_SYNTAX": {
                                //Convert to new syntax

                                TokenPattern<?> subCommand = ((TokenStructure) commandType.find("SUBCOMMAND")).getContents();

                                switch(subCommand.getName()) {
                                    case "ITEM_COPY_COMMAND": {
                                        StringBounds literalBounds = commandType.find("SUBCOMMAND.LITERAL_COPY").getStringBounds();
                                        Inspection inspection = new Inspection("Convert to new item syntax")
                                                .setStartIndex(bounds.start.index)
                                                .setEndIndex(bounds.end.index)
                                                .addAction(
                                                        new CodeChainAction(
                                                                "Convert to new item syntax",

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(literalBounds.start.index)
                                                                        .setReplacementEndIndex(literalBounds.end.index)
                                                                        .setReplacementText("from"),

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(subCommandBounds.start.index)
                                                                        .setReplacementEndIndex(subCommandBounds.start.index)
                                                                        .setReplacementText("replace ")
                                                        )
                                                );

                                        l.getInspectionModule().addInspection(inspection);
                                        break;
                                    }
                                    case "ITEM_REPLACE_COMMAND": {
                                        StringBounds literalBounds = commandType.find("SUBCOMMAND.LITERAL_REPLACE").getStringBounds();
                                        Inspection inspection = new Inspection("Convert to new item syntax")
                                                .setStartIndex(bounds.start.index)
                                                .setEndIndex(bounds.end.index)
                                                .addAction(
                                                        new CodeChainAction(
                                                                "Convert to new item syntax",

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(literalBounds.start.index)
                                                                        .setReplacementEndIndex(literalBounds.end.index)
                                                                        .setReplacementText("with"),

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(subCommandBounds.start.index)
                                                                        .setReplacementEndIndex(subCommandBounds.start.index)
                                                                        .setReplacementText("replace ")
                                                        )
                                                );

                                        l.getInspectionModule().addInspection(inspection);
                                        break;
                                    }
                                    case "ITEM_MODIFY_COMMAND": {
                                        StringBounds literalBounds = commandType.find("SUBCOMMAND.LITERAL_MODIFY").getStringBounds();
                                        Inspection inspection = new Inspection("Convert to new item syntax")
                                                .setStartIndex(bounds.start.index)
                                                .setEndIndex(bounds.end.index)
                                                .addAction(
                                                        new CodeChainAction(
                                                                "Convert to new item syntax",

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(literalBounds.start.index - 1)
                                                                        .setReplacementEndIndex(literalBounds.end.index)
                                                                        .setReplacementText(""),

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(subCommandBounds.start.index)
                                                                        .setReplacementEndIndex(subCommandBounds.start.index)
                                                                        .setReplacementText("modify ")
                                                        )
                                                );

                                        l.getInspectionModule().addInspection(inspection);
                                        break;
                                    }
                                }

                                break;
                            }
                            case "NEW_ITEM_REPLACE": {
                                //Convert to replaceitem

                                StringBounds replaceLiteralBounds = commandType.find("LITERAL_REPLACE").getStringBounds();
                                TokenPattern<?> subCommand = ((TokenStructure) commandType.find("SUBCOMMAND")).getContents();

                                if("ITEM_REPLACE_WITH_COMMAND".equals(subCommand.getName())) {
                                    StringBounds withLiteralBounds = subCommand.find("LITERAL_WITH").getStringBounds();
                                    Inspection inspection = new Inspection("Convert to replaceitem command")
                                            .setStartIndex(bounds.start.index)
                                            .setEndIndex(bounds.end.index)
                                            .addAction(
                                                    new CodeChainAction(
                                                            "Convert to replaceitem command",

                                                            new CodeReplacementAction()
                                                                    .setReplacementStartIndex(withLiteralBounds.start.index-1)
                                                                    .setReplacementEndIndex(withLiteralBounds.end.index)
                                                                    .setReplacementText(""),
                                                            new CodeReplacementAction()
                                                                    .setReplacementStartIndex(replaceLiteralBounds.start.index-1)
                                                                    .setReplacementEndIndex(replaceLiteralBounds.end.index)
                                                                    .setReplacementText(""),
                                                            new CodeReplacementAction()
                                                                    .setReplacementStartIndex(bounds.start.index)
                                                                    .setReplacementEndIndex(bounds.start.index + "item".length())
                                                                    .setReplacementText("replaceitem")
                                                    )
                                            );

                                    l.getInspectionModule().addInspection(inspection);
                                }


                                break;
                            }
                        }


//                        StringBounds replaceLiteralBounds = p.find("SUBCOMMAND.LITERAL_REPLACE").getStringBounds();
//
//                        Inspection inspection = new Inspection("Convert to replaceitem command")
//                                .setStartIndex(bounds.start.index)
//                                .setEndIndex(bounds.end.index)
//                                .addAction(
//                                        new CodeChainAction(
//                                                "Convert to replaceitem command",
//
//                                                new CodeReplacementAction()
//                                                        .setReplacementStartIndex(replaceLiteralBounds.start.index-1)
//                                                        .setReplacementEndIndex(replaceLiteralBounds.end.index)
//                                                        .setReplacementText(""),
//                                                new CodeReplacementAction()
//                                                        .setReplacementStartIndex(bounds.start.index)
//                                                        .setReplacementEndIndex(bounds.start.index + "item".length())
//                                                        .setReplacementText("replaceitem")
//                                        )
//                                );
//
//                        l.getInspectionModule().addInspection(inspection);
                    }
                })
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
