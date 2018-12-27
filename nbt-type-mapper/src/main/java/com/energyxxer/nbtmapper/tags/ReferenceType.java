package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.PathContext;

public class ReferenceType extends DataType implements DeepDataType {

    private String name;

    public ReferenceType(NBTTypeMap parent, String name) {
        this(parent, name, null);
    }

    public ReferenceType(NBTTypeMap parent, String name, TypeFlags flags) {
        super(parent);
        this.name = name;
        this.setFlags(flags);
    }

    @Override
    public void collectDataTypeFor(PathContext context, NBTPath path, DataTypeQueryResponse response) {
        this.parent.collectDataTypeFor(name, context, path, response);
    }

    @Override
    public Class<? extends NBTTag> getCorrespondingTagType() {
        return TagCompound.class;
    }

    @Override
    public String toString() {
        return "$" + name + (flags != null ? " " + flags : "");
    }
}
