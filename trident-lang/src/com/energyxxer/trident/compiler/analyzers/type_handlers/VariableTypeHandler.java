package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.ILazyValue;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.*;

@AnalyzerGroup
public interface VariableTypeHandler<T> {
    Object getMember(T object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol);

    Object getIndexer(T object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol);

    @SuppressWarnings("unchecked")
    <F> F cast(T object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx);

    default Object coerce(T object, Class targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    default Iterator<?> getIterator(T object) {
        return null;
    }

    class Static {

        public static String getIdentifierForClass(Class<?> cls) {
            if(VariableMethod.class.isAssignableFrom(cls)) {
                return VariableMethod.class.getName();
            }
            if(Entity.class.isAssignableFrom(cls)) {
                return Entity.class.getName();
            }
            if(TextComponent.class.isAssignableFrom(cls)) {
                return TextComponent.class.getName();
            }
            if(ILazyValue.class.isAssignableFrom(cls)) {
                return ILazyValue.class.getName();
            }
            return cls.getName();
        }

        private static HashMap<String, Class> shorthands = new HashMap<>();
        private static ArrayList<Class> superclasses = new ArrayList<>();

        static {
            shorthands.put("int",               Integer.class);
            shorthands.put("real",              Double.class);
            shorthands.put("int_range",         IntegerRange.class);
            shorthands.put("real_range",        DoubleRange.class);
            shorthands.put("boolean",           Boolean.class);
            shorthands.put("string",            String.class);
            shorthands.put("entity",            Entity.class);
            shorthands.put("block",             Block.class);
            shorthands.put("item",              Item.class);
            shorthands.put("text_component",    TextComponent.class);
            shorthands.put("nbt",               TagCompound.class);
            shorthands.put("tag_compound",      TagCompound.class);
            shorthands.put("tag_list",          TagList.class);
            shorthands.put("tag_byte",          TagByte.class);
            shorthands.put("tag_short",         TagShort.class);
            shorthands.put("tag_int",           TagInt.class);
            shorthands.put("tag_float",         TagFloat.class);
            shorthands.put("tag_double",        TagDouble.class);
            shorthands.put("tag_long",          TagLong.class);
            shorthands.put("tag_string",        TagString.class);
            shorthands.put("tag_byte_array",    TagByteArray.class);
            shorthands.put("tag_int_array",     TagIntArray.class);
            shorthands.put("tag_long_array",    TagLongArray.class);
            shorthands.put("nbt_value",         NBTTag.class);
            shorthands.put("nbt_path",          NBTPath.class);
            shorthands.put("coordinates",       CoordinateSet.class);
            shorthands.put("resource",          TridentUtil.ResourceLocation.class);
            shorthands.put("pointer",           PointerObject.class);
            shorthands.put("dictionary",        DictionaryObject.class);
            shorthands.put("list",              ListObject.class);
            shorthands.put("custom_entity",     CustomEntity.class);
            shorthands.put("custom_item",       CustomItem.class);
            shorthands.put("function",          VariableMethod.class);
            shorthands.put("exception",         TridentException.class);

            superclasses.add(NBTTag.class);
        }

        public static Class getClassForShorthand(String shorthand) {
            return shorthands.get(shorthand);
        }

        public static String getShorthandForObject(Object obj) {
            return shorthands.entrySet().stream().filter(e -> e.getValue().isInstance(obj) && !superclasses.contains(e.getValue())).max(Comparator.comparingInt(a -> a.getKey().length())).map(Map.Entry::getKey).orElse(null);
        }

        public static String getShorthandForClass(Class cls) {
            return shorthands.entrySet().stream().filter(e -> e.getValue() == cls).max(Comparator.comparingInt(a -> a.getKey().length())).map(Map.Entry::getKey).orElse(null);
        }
    }
}
