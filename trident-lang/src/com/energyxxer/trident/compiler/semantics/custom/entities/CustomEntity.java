package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.types.Type;

public class CustomEntity {
    private final String id;
    private final Type defaultType;
    private TagCompound defaultNBT;

    public CustomEntity(String id, Type defaultType) {
        this.id = id;
        this.defaultType = defaultType;
    }

    public String getId() {
        return id;
    }

    public Type getDefaultType() {
        return defaultType;
    }

    public TagCompound getDefaultNBT() {
        return defaultNBT;
    }

    public void setDefaultNBT(TagCompound defaultNBT) {
        this.defaultNBT = defaultNBT;
    }
}
