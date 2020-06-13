package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathCompoundRoot;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathKey;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.PathContext;

import java.util.*;

public class CompoundType extends DataType implements DeepDataType {
    private DataType defaultType;
    private HashMap<String, DataType> types = new HashMap<>();
    private HashMap<String, DataType> volatileTypes = new HashMap<>();

    public CompoundType(NBTTypeMap parent) {
        super(parent);
    }

    public void collectDataTypeFor(PathContext context, NBTPath path, DataTypeQueryResponse response) {
        NBTPathNode node = path.getNode();
        if(node instanceof NBTPathCompoundRoot) { //object condition, get next
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
                    } else {
                        response.addNotice(new Notice(NoticeType.ERROR, "'" + key + "' cannot contain children"));
                    }
                } else { //end of path
                    searchLeafTypes(type, response, parent, true);
                }
            }
            if(context.isSetting() && volatileTypes.containsKey(key)) {
                DataType type = volatileTypes.get(key);
                if(path.hasNext()) {
                    if(type instanceof DeepDataType) {
                        ((DeepDataType) type).collectDataTypeFor(context, path.getNext(), response);
                    } else {
                        response.addNotice(new Notice(NoticeType.ERROR, "'" + key + "' cannot contain children"));
                    }
                } else {
                    searchLeafTypes(type, response, parent, true);
                }
            }
            if(defaultType != null) {
                searchLeafTypes(defaultType, response, parent, false);
            }
        }
    }

    static void searchLeafTypes(DataType from, DataTypeQueryResponse response, NBTTypeMap parent, boolean likely) {
        if(from instanceof ReferenceType) {
            ArrayList<DataType> innerTypes = new ArrayList<>(parent.getDataTypesForRootName(((ReferenceType) from)));

            while(!innerTypes.isEmpty()) {
                DataType innerType = innerTypes.get(0);
                innerTypes.remove(0);

                if(innerType instanceof ReferenceType) {
                    innerTypes.addAll(0, parent.getDataTypesForRootName(((ReferenceType) innerType)));
                } else {
                    response.addLikelyType(innerType);
                }
            }
        } else {
            if(likely) response.addLikelyType(from);
            else response.addUnlikelyType(from);
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
    public String getShortTypeName() {
        return "Compound";
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CompoundType that = (CompoundType) o;
        return Objects.equals(defaultType, that.defaultType) &&
                Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defaultType, types);
    }

    public DataType get(String key) {
        return types.get(key);
    }
}
