package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.nbtmapper.PathContext;

public interface DeepDataType {
    void collectDataTypeFor(PathContext context, NBTPath path, DataTypeQueryResponse response);
}
