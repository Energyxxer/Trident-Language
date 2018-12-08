package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.types.Type;

public class CustomEntity {
    private final String id;
    private final Type defaultType;
    private TagCompound defaultNBT;
    private String idTag;
    private boolean fullyDeclared = false;

    public CustomEntity(String id, Type defaultType) {
        this.id = id;
        this.defaultType = defaultType;
        this.idTag = "trident-entity." + id.replace(':', '.').replace('/','.');
        this.defaultNBT = getBaseNBT();
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
        this.defaultNBT = defaultNBT.merge(getBaseNBT());
    }

    public String getIdTag() {
        return idTag;
    }

    private TagCompound getBaseNBT() {
        return new TagCompound(new TagList("Tags", new TagString(idTag)));
    }

    public boolean isFullyDeclared() {
        return fullyDeclared;
    }

    public void endDeclaration() {
        fullyDeclared = true;
    }
}
