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
        importUnit(productions -> group(literal("resource").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_RESOURCE_LOCATION").setSimplificationFunction(wrapperSimplification));

        //entity
        importUnit(productions -> group(literal("entity").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("LIMITED_ENTITY"), TridentProductions.brace(">")).setName("WRAPPED_ENTITY").setSimplificationFunction(wrapperSimplification));

        //block
        importUnit(productions -> group(literal("block").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("BLOCK_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_BLOCK").setSimplificationFunction(wrapperSimplification));

        //item
        importUnit(productions -> group(literal("item").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ITEM_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_ITEM").setSimplificationFunction(d -> {
            d.pattern = ((TokenGroup) d.pattern).getContents()[2];
            d.data = new Object[] {(ISymbolContext) d.data[0], NBTMode.SETTING};
        }));

        //text component
        importUnit(productions -> group(literal("text_component").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("TEXT_COMPONENT"), TridentProductions.brace(">")).setName("WRAPPED_TEXT_COMPONENT").setSimplificationFunction(wrapperSimplification));

        //nbt
        importUnit(productions -> group(literal("nbt").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NBT_COMPOUND"), TridentProductions.brace(">")).setName("WRAPPED_NBT").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("nbt_value").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NBT_VALUE"), TridentProductions.brace(">")).setName("WRAPPED_NBT_VALUE").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("nbt_path").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NBT_PATH"), TridentProductions.brace(">")).setName("WRAPPED_NBT_PATH").setSimplificationFunction(wrapperSimplification));

        //ranges
        importUnit(productions -> group(literal("int_range").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("INTEGER_NUMBER_RANGE"), TridentProductions.brace(">")).setName("WRAPPED_INT_RANGE").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("real_range").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("REAL_NUMBER_RANGE"), TridentProductions.brace(">")).setName("WRAPPED_REAL_RANGE").setSimplificationFunction(wrapperSimplification));

        //coordinates
        importUnit(productions -> group(literal("coordinates").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("COORDINATE_SET"), TridentProductions.brace(">")).setName("WRAPPED_COORDINATE").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("rotation").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ROTATION"), TridentProductions.brace(">")).setName("WRAPPED_ROTATION").setSimplificationFunction(wrapperSimplification));

        importUnit(productions -> group(literal("uuid").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("UUID"), TridentProductions.brace(">")).setName("WRAPPED_UUID").setSimplificationFunction(wrapperSimplification));


        //temp
        importUnit(productions -> group(literal("__temp_block_id").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("BLOCK_ID"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("__temp_block_tag").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("BLOCK_ID_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("__temp_entity_id").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ENTITY_ID"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("__temp_entity_tag").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ENTITY_ID_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));

        importUnit(productions -> group(literal("__temp_dimension_id").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("DIMENSION_ID"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));

        importUnit(productions -> group(literal("__temp_particle").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("PARTICLE"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));
        importUnit(productions -> group(literal("__temp_gamerule_setter").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("GAMERULE_SETTER"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));

        importUnit(productions -> group(literal("__temp_block").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("BLOCK"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));


        importUnit(productions -> group(literal("__temp_item_setting").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ITEM_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(d -> {
            d.pattern = ((TokenGroup) d.pattern).getContents()[2];
            d.data = new Object[] {(ISymbolContext) d.data[0], NBTMode.SETTING};
        }));
        importUnit(productions -> group(literal("__temp_item_testing").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("ITEM_TAGGED"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(d -> {
            d.pattern = ((TokenGroup) d.pattern).getContents()[2];
            d.data = new Object[] {(ISymbolContext) d.data[0], NBTMode.TESTING};
        }));

        importUnit(productions -> group(literal("__nel").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("NEW_ENTITY_LITERAL"), TridentProductions.brace(">")).setName("WRAPPED_TEMP").setSimplificationFunction(wrapperSimplification));
    }
}
