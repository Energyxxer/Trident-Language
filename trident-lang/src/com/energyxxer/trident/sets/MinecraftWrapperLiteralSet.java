package com.energyxxer.trident.sets;

import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

import java.util.function.Consumer;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.literal;

public class MinecraftWrapperLiteralSet extends PatternProviderSet {

    public MinecraftWrapperLiteralSet() {
        super("ROOT_INTERPOLATION_VALUE");

        final Consumer<TokenPattern.SimplificationDomain> wrapperSimplification = (d) -> {
            d.pattern = ((TokenGroup) d.pattern).getContents()[2];
            d.data = new Object[] {(ISymbolContext) d.data[0]};
        };

        //resource
        importUnit((productions, worker) -> group(literal("resource").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_RESOURCE_LOCATION").addTags("primitive:resource").setSimplificationFunction(wrapperSimplification));

        //entity
        importUnit((productions, worker) -> group(literal("entity").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("LIMITED_ENTITY"), TridentProductions.brace(">")).setName("WRAPPED_ENTITY").addTags("primitive:entity").setSimplificationFunction(wrapperSimplification));
        importUnit((productions, worker) -> group(literal("selector_argument").setName("VALUE_WRAPPER_KEY").setRecessive(), TridentProductions.brace("<").setRecessive(), productions.getOrCreateStructure("SELECTOR_ARGUMENT"), TridentProductions.brace(">")).setName("WRAPPED_SELECTOR_ARGUMENT").addTags("primitive:selector_argument")
                .setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    return ctx.getTypeSystem().sanitizeObject(p.find("SELECTOR_ARGUMENT").evaluate(ctx));
                }));

        //block
        importUnit((productions, worker) -> group(literal("block").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("BLOCK_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_BLOCK").addTags("primitive:block").setSimplificationFunction(wrapperSimplification));

        //item
        importUnit((productions, worker) -> group(literal("item").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ITEM_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_ITEM").addTags("primitive:item").setSimplificationFunction(d -> {
            d.pattern = ((TokenGroup) d.pattern).getContents()[2];
            d.data = new Object[] {(ISymbolContext) d.data[0], NBTMode.SETTING};
        }));

        //text component
        importUnit((productions, worker) -> group(literal("text_component").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("TEXT_COMPONENT"), TridentProductions.brace(">")).setName("WRAPPED_TEXT_COMPONENT").addTags("primitive:text_component").setSimplificationFunction(wrapperSimplification));

        //nbt
        importUnit((productions, worker) -> group(literal("nbt").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NBT_COMPOUND"), TridentProductions.brace(">")).setName("WRAPPED_NBT").addTags("primitive:tag_compound").setSimplificationFunction(wrapperSimplification));
        importUnit((productions, worker) -> group(literal("nbt_value").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NBT_VALUE"), TridentProductions.brace(">")).setName("WRAPPED_NBT_VALUE").addTags("primitive:nbt_value").setSimplificationFunction(wrapperSimplification));
        importUnit((productions, worker) -> group(literal("nbt_path").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NBT_PATH"), TridentProductions.brace(">")).setName("WRAPPED_NBT_PATH").addTags("primitive:nbt_path").setSimplificationFunction(wrapperSimplification));

        //ranges
        importUnit((productions, worker) -> group(literal("int_range").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("INTEGER_NUMBER_RANGE"), TridentProductions.brace(">")).setName("WRAPPED_INT_RANGE").addTags("primitive:int_range").setSimplificationFunction(wrapperSimplification));
        importUnit((productions, worker) -> group(literal("real_range").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("REAL_NUMBER_RANGE"), TridentProductions.brace(">")).setName("WRAPPED_REAL_RANGE").addTags("primitive:real_range").setSimplificationFunction(wrapperSimplification));

        //coordinates
        importUnit((productions, worker) -> group(literal("coordinates").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("COORDINATE_SET"), TridentProductions.brace(">")).setName("WRAPPED_COORDINATE").addTags("primitive:coordinates").setSimplificationFunction(wrapperSimplification));
        importUnit((productions, worker) -> group(literal("rotation").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ROTATION"), TridentProductions.brace(">")).setName("WRAPPED_ROTATION").addTags("primitive:rotation").setSimplificationFunction(wrapperSimplification));

        importUnit((productions, worker) -> group(literal("uuid").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("UUID"), TridentProductions.brace(">")).setName("WRAPPED_UUID").addTags("primitive:uuid").setSimplificationFunction(wrapperSimplification));

    }
}
