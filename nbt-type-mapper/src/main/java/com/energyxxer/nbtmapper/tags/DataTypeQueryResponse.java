package com.energyxxer.nbtmapper.tags;

import java.util.ArrayList;
import java.util.Collection;

public class DataTypeQueryResponse {
    private ArrayList<DataType> possibleTypes = new ArrayList<>();

    void addLikelyType(DataType type) {
        possibleTypes.remove(type);
        possibleTypes.add(0, type); //add it to the start, even if it already exists
        //if it was previously unlikely, this will make it likely
    }

    void addUnlikelyType(DataType type) {
        if(!possibleTypes.contains(type)) possibleTypes.add(type); //add it to the end, if it doesn't already exist
        //if it was previously likely, this will not make it unlikely
    }

    public Collection<DataType> getPossibleTypes() {
        return possibleTypes;
    }

    public boolean isEmpty() {
        return possibleTypes.isEmpty();
    }
}
