package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.nbtmapper.NBTTypeMap;

public abstract class DataType {
    protected final NBTTypeMap parent;
    protected TypeFlags flags;

    public DataType(NBTTypeMap parent) {
        this.parent = parent;
    }

    public abstract Class<? extends NBTTag> getCorrespondingTagType();

    public TypeFlags getFlags() {
        return flags;
    }

    public void setFlags(TypeFlags flags) {
        this.flags = flags;
    }
}
