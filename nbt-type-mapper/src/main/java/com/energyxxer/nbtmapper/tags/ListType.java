package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathIndex;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.PathContext;

public class ListType extends DataType implements DeepDataType {
    private DataType innerType;

    public ListType(NBTTypeMap parent, DataType innerType) {
        super(parent);
        this.innerType = innerType;
    }

    @Override
    public void collectDataTypeFor(PathContext context, NBTPath path, DataTypeQueryResponse response) {
        NBTPathNode node = path.getNode();
        if(node instanceof NBTPathIndex || node instanceof NBTListMatch) {
            if(path.hasNext()) {
                if(innerType instanceof DeepDataType) {
                    ((DeepDataType) innerType).collectDataTypeFor(context, path.getNext(), response);
                }
            } else {
                response.addLikelyType(innerType);
            }
        }
    }

    @Override
    public Class<? extends NBTTag> getCorrespondingTagType() {
        return null;
    }

    @Override
    public String toString() {
        return "[ " + innerType + " ]";
    }
}
