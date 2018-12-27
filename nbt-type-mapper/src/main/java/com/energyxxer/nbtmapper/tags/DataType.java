package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.nbtmapper.NBTTypeMap;

import java.util.Objects;

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

    public abstract String getShortTypeName();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataType dataType = (DataType) o;
        return Objects.equals(flags, dataType.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flags);
    }
}
