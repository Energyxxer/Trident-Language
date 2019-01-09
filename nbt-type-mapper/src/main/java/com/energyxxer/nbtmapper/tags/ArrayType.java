package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathIndex;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.PathContext;

import java.util.Objects;

public class ArrayType extends DataType implements DeepDataType {
    private DataType innerType;
    private Class<? extends ArrayNBTTag> correspondingType;
    private String typeName;

    public ArrayType(NBTTypeMap parent, String prefix) {
        super(parent);
        switch(prefix) {
            case "B": {
                innerType = new FlatType(parent, "Byte");
                correspondingType = TagByteArray.class;
                typeName = "TAG_Byte_Array";
                break;
            }
            case "I": {
                innerType = new FlatType(parent, "Int");
                correspondingType = TagIntArray.class;
                typeName = "TAG_Int_Array";
                break;
            }
            case "L": {
                innerType = new FlatType(parent, "Long");
                correspondingType = TagLongArray.class;
                typeName = "TAG_Long_Array";
                break;
            }
            default: throw new IllegalArgumentException("Unknown array type prefix: " + prefix);
        }
    }

    @Override
    public Class<? extends NBTTag> getCorrespondingTagType() {
        return correspondingType;
    }

    @Override
    public String getShortTypeName() {
        return typeName;
    }

    @Override
    public void collectDataTypeFor(PathContext context, NBTPath path, DataTypeQueryResponse response) {
        NBTPathNode node = path.getNode();
        if(node instanceof NBTPathIndex || node instanceof NBTListMatch) {
            if(path.hasNext()) {
                if(innerType instanceof DeepDataType) {
                    ((DeepDataType) innerType).collectDataTypeFor(context, path.getNext(), response);
                } else {
                    response.addNotice(new Notice(NoticeType.ERROR, "Elements of type '" + innerType.getShortTypeName() + "' cannot contain children"));
                }
            } else {
                response.addLikelyType(innerType);
            }
        }
    }

    @Override
    public String toString() {
        return "[" + typeName.charAt(4) + ";]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(typeName, arrayType.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), typeName);
    }
}
