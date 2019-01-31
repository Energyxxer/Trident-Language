package com.energyxxer.trident.compiler.semantics.datastores;

import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.trident.compiler.semantics.Symbol;

public class DataBank extends Symbol {

    private final Object value;

    public DataBank(String name, Object value) {
        super(name, SymbolVisibility.GLOBAL);

        this.value = value;

        if (!(value instanceof CoordinateSet || value instanceof Entity)) {
            throw new IllegalArgumentException("Value passed to a data bank is not a coordinate nor an entity");
        }
    }
}
