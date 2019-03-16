package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

public class NBTToDictionary {
    public static Object convert(NBTTag tag, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(tag.getType()) {
            case "TAG_Byte": {
                return (int)((TagByte) tag).getValue();
            }
            case "TAG_Short": {
                return (int)((TagShort) tag).getValue();
            }
            case "TAG_Int": {
                return ((TagInt) tag).getValue();
            }
            case "TAG_Float": {
                return (double)((TagFloat) tag).getValue();
            }
            case "TAG_Double": {
                return ((TagDouble) tag).getValue();
            }
            case "TAG_Long": {
                return tag;
            }
            case "TAG_String": {
                return ((TagString) tag).getValue();
            }
            case "TAG_Compound": {
                DictionaryObject dict = new DictionaryObject();
                TagCompound compound = (TagCompound) tag;

                for(NBTTag inner : compound.getAllTags()) {
                    dict.put(inner.getName(), convert(inner, pattern, ctx));
                }

                return dict;
            }
            case "TAG_List":
            case "TAG_Byte_Array":
            case "TAG_Int_Array":
            case "TAG_Long_Array": {
                ListObject list = new ListObject();
                ComplexNBTTag compound = (ComplexNBTTag) tag;

                for(NBTTag inner : compound.getAllTags()) {
                    list.add(convert(inner, pattern, ctx));
                }

                return list;
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown NBT tag type: " + tag.getType(), pattern, ctx);
            }
        }
    }
}
