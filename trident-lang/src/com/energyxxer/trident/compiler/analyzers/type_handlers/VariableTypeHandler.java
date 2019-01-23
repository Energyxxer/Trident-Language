package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.HashMap;

@AnalyzerGroup
public interface VariableTypeHandler<T> {
    Object getMember(T object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol);

    Object getIndexer(T object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol);

    @SuppressWarnings("unchecked")
    <F> F cast(T object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file);

    default Object coerce(T object, Class targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
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
            if(cls.isInstance(CommonParsers.SAMPLE_INT_RANGE)) return cls.getName() + "<Integer>";
            if(cls.isInstance(CommonParsers.SAMPLE_REAL_RANGE)) return cls.getName() + "<Double>";
            return cls.getName();
        }

        private static HashMap<String, Class> shorthands = new HashMap<>();

        static {
            shorthands.put("int", Integer.class);
            shorthands.put("real", Double.class);
            shorthands.put("boolean", Boolean.class);
            shorthands.put("string", String.class);
            shorthands.put("entity", Entity.class);
            shorthands.put("block", Block.class);
            shorthands.put("item", Item.class);
            shorthands.put("text_component", TextComponent.class);
            shorthands.put("nbt", TagCompound.class);
            shorthands.put("tag_compound", TagCompound.class);
            shorthands.put("tag_list", TagList.class);
            shorthands.put("tag_byte", TagByte.class);
            shorthands.put("tag_short", TagShort.class);
            shorthands.put("tag_int", TagInt.class);
            shorthands.put("tag_float", TagFloat.class);
            shorthands.put("tag_double", TagDouble.class);
            shorthands.put("tag_long", TagLong.class);
            shorthands.put("tag_string", TagString.class);
            shorthands.put("tag_byte_array", TagByteArray.class);
            shorthands.put("tag_int_array", TagIntArray.class);
            shorthands.put("tag_long_array", TagLongArray.class);
            shorthands.put("nbt_value", NBTTag.class);
            shorthands.put("nbt_path", NBTPath.class);
            shorthands.put("coordinates", CoordinateSet.class);
            shorthands.put("resource", TridentUtil.ResourceLocation.class);

            //"coordinate", "int_range", "real_range", "dictionary", "array"
        }

        public static Class getClassForShorthand(String shorthand) {
            return shorthands.get(shorthand);
        }
    }
}
