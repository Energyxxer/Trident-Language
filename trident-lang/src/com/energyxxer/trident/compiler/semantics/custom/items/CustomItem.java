package com.energyxxer.trident.compiler.semantics.custom.items;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.types.Type;

public class CustomItem {
    private final String id;
    private final Type defaultType;
    private TagCompound defaultNBT;
    private boolean useModelData = false;
    private int customModelData = 0;
    private boolean fullyDeclared = false;

    public CustomItem(String id, Type defaultType) {
        this.id = id;
        this.defaultType = defaultType;
    }
}
