package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class NBTToDictionary {
    public static Object convert(NBTTag tag, TridentFile file) {
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
                    dict.put(inner.getName(), convert(inner, file));
                }

                return dict;
            }
            case "TAG_List":
            case "TAG_Byte_Array":
            case "TAG_Int_Array":
            case "TAG_Long_Array": {
                ListType list = new ListType();
                ComplexNBTTag compound = (ComplexNBTTag) tag;

                for(NBTTag inner : compound.getAllTags()) {
                    list.add(convert(inner, file));
                }

                return list;
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown NBT tag type: " + tag.getType()));
                throw new EntryParsingException();
            }
        }
    }
}
