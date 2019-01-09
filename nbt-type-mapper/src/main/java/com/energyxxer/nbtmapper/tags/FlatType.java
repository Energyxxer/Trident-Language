package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.nbtmapper.NBTTypeMap;

import java.util.HashMap;
import java.util.Objects;

public class FlatType extends DataType {

    private String typeName;
    private Class<? extends NBTTag> correspondingType;

    private static final HashMap<String, Class<? extends NBTTag>> keyToClassMap;

    static {
        keyToClassMap = new HashMap<>();
        keyToClassMap.put("Byte", TagByte.class);
        keyToClassMap.put("Short", TagShort.class);
        keyToClassMap.put("Int", TagInt.class);
        keyToClassMap.put("Float", TagFloat.class);
        keyToClassMap.put("Double", TagDouble.class);
        keyToClassMap.put("Long", TagLong.class);
        keyToClassMap.put("String", TagString.class);
        keyToClassMap.put("JSON_Boolean", TagByte.class); //TODO
    }

    public FlatType(NBTTypeMap parent, String typeName) {
        super(parent);
        this.typeName = typeName;
        this.correspondingType = keyToClassMap.get(typeName);
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
    public String toString() {
        return typeName + (flags != null ? " " + flags : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FlatType flatType = (FlatType) o;
        return Objects.equals(typeName, flatType.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), typeName);
    }
}
