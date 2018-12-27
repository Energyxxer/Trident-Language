package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTObjectMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathKey;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.PathContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CompoundType extends DataType implements DeepDataType {
    private DataType defaultType;
    private HashMap<String, DataType> types = new HashMap<>();
    private HashMap<String, DataType> volatileTypes = new HashMap<>();

    public CompoundType(NBTTypeMap parent) {
        super(parent);
    }

    public void collectDataTypeFor(PathContext context, NBTPath path, DataTypeQueryResponse response) {
        NBTPathNode node = path.getNode();
        if(node instanceof NBTObjectMatch) { //object condition, get next
            if(path.hasNext()) {
                this.collectDataTypeFor(context, path.getNext(), response);
            } else {
                response.addLikelyType(this);
            }
        } else if(node instanceof NBTPathKey) {
            String key = node.getPathString();
            if(types.containsKey(key)) {
                DataType type = types.get(key);
                if(path.hasNext()) {
                    if(type instanceof DeepDataType) {
                        ((DeepDataType) type).collectDataTypeFor(context, path.getNext(), response);
                    }
                } else { //end of path
                    response.addLikelyType(type);
                }
            } else if(context.isSetting() && volatileTypes.containsKey(key)) {
                DataType type = volatileTypes.get(key);
                if(path.hasNext()) {
                    if(type instanceof DeepDataType) {
                        ((DeepDataType) type).collectDataTypeFor(context, path.getNext(), response);
                    }
                } else {
                    response.addLikelyType(volatileTypes.get(key));
                }
            } else if(defaultType != null) {
                response.addUnlikelyType(defaultType);
            }
        }
    }


    @Override
    public Class<? extends NBTTag> getCorrespondingTagType() {
        return TagCompound.class;
    }

    public void put(String key, DataType value) {
        this.types.put(key, value);
    }

    public void putVolatile(String key, DataType value) {
        this.volatileTypes.put(key, value);
    }

    public void setDefaultType(DataType type) {
        this.defaultType = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");

        ArrayList<Map.Entry<String, DataType>> joined = new ArrayList<>(types.entrySet());
        joined.addAll(volatileTypes.entrySet());
        if(defaultType != null) joined.add(new Map.Entry<String, DataType>() {
            @Override
            public String getKey() {
                return "*";
            }

            @Override
            public DataType getValue() {
                return defaultType;
            }

            @Override
            public DataType setValue(DataType value) {
                return null;
            }
        });

        Iterator<Map.Entry<String, DataType>> it = joined.iterator();
        while(it.hasNext()) {
            Map.Entry<String, DataType> entry = it.next();
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            if(it.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append("}");
        return sb.toString();
    }
}
