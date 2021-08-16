package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.item.ItemHolderBlock;
import com.energyxxer.commodore.functionlogic.commands.item.ItemHolderEntity;
import com.energyxxer.commodore.functionlogic.commands.item.ItemReplaceCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.ItemSlot;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeChainAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeReplacementAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.Inspection;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.util.StringBounds;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ReplaceItemCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"replaceitem"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        TokenGroupMatch pattern = group(
                TridentProductions.commandHeader("replaceitem"),
                choice(
                        group(literal("block"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                            return new ItemReplaceCommand(new ItemHolderBlock(pos, (ItemSlot) d[1]), (Item) d[2], (int) d[3]);
                        }),
                        group(literal("entity"), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                            return new ItemReplaceCommand(new ItemHolderEntity(entity, (ItemSlot) d[1]), (Item) d[2], (int) d[3]);
                        })
                ).setName("TARGET"),
                productions.getOrCreateStructure("SLOT_ID"),
                productions.getOrCreateStructure("ITEM"),
                TridentProductions.integer(productions).setOptional().setName("COUNT").addTags("cspn:Count")
        );

        if(TridentProductions.checkVersionFeature(worker,"command.item", false)) {
            pattern.addProcessor((p, l) -> {
                if(l.getInspectionModule() != null) {

                    StringBounds bounds = p.getStringBounds();

                    int slotIdEndIndex = p.find("SLOT_ID").getStringBounds().end.index;

                    Inspection inspection = new Inspection("Convert to item command")
                            .setStartIndex(bounds.start.index)
                            .setEndIndex(bounds.end.index)
                            .addAction(
                                    new CodeChainAction(
                                            "Convert to item command",

                                            new CodeReplacementAction()
                                                    .setReplacementStartIndex(slotIdEndIndex)
                                                    .setReplacementEndIndex(slotIdEndIndex)
                                                    .setReplacementText(" with"),
                                            new CodeReplacementAction()
                                                    .setReplacementStartIndex(bounds.start.index)
                                                    .setReplacementEndIndex(bounds.start.index + "replaceitem".length())
                                                    .setReplacementText("item replace")
                                    )
                            );

                    l.getInspectionModule().addInspection(inspection);
                }
            });
        }

        return pattern;
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Type slot = (Type) pattern.find("SLOT_ID").evaluate(ctx);
        Item item = (Item) pattern.find("ITEM").evaluate(ctx, NBTMode.SETTING, false);
        int count = (int) pattern.findThenEvaluate("COUNT", 1, ctx);

        try {
            return (Command) pattern.find("TARGET").evaluate(ctx, slot, item, count);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("TARGET.ENTITY"))
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("COUNT"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("ITEM"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
