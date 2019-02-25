package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.types.defaults.FunctionReference;

public class ObjectiveCreationFile extends SpecialFile {

    public ObjectiveCreationFile(SpecialFileManager parent) {
        super(parent, "create_objectives");
    }

    @Override
    public boolean shouldForceCompile() {
        return !parent.getCompiler().getModule().getObjectiveManager().getAll().isEmpty();
    }

    @Override
    protected void compile() {
        parent.getCompiler().getModule().getObjectiveManager().setCreationFunction(this.function);
        Tag loadTag = parent.getCompiler().getModule().minecraft.tags.functionTags.create("load");
        loadTag.setExport(true);
        loadTag.addValue(new FunctionReference(this.function));
    }
}
