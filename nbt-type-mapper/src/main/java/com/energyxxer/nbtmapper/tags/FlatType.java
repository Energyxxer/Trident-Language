package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.nbtmapper.NBTTypeMap;

import java.util.HashMap;

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
        keyToClassMap.put("String", TagString.class);
        keyToClassMap.put("Boolean", TagByte.class); //TODO
    }

    public FlatType(NBTTypeMap parent, String typeName) {
        super(parent);
        this.typeName = typeName;
        this.correspondingType = keyToClassMap.get(typeName);
    }

    @Override
    public Class<? extends NBTTag> getCorrespondingTagType() {
        return null;
    }

    @Override
    public String toString() {
        return typeName + (flags != null ? " " + flags : "");
    }
}
