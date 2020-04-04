package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathIndex;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.PathContext;

import java.util.Objects;

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
                } else {
                    response.addNotice(new Notice(NoticeType.ERROR, "Elements of type '" + innerType.getShortTypeName() + "' cannot contain children"));
                }
            } else {
                CompoundType.searchLeafTypes(innerType, response, parent, true);
            }
        }
    }

    @Override
    public Class<? extends NBTTag> getCorrespondingTagType() {
        return TagList.class;
    }

    @Override
    public String getShortTypeName() {
        return "List";
    }

    @Override
    public String toString() {
        return "[ " + innerType + " ]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ListType listType = (ListType) o;
        return Objects.equals(innerType, listType.innerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), innerType);
    }
}
