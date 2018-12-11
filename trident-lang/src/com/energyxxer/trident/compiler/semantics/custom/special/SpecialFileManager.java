package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

public class SpecialFileManager {
    private final TridentCompiler compiler;
    public final ItemEventFile itemEvents;

    private Function tickFunction = null;

    public SpecialFileManager(TridentCompiler compiler) {
        this.compiler = compiler;
        this.itemEvents = new ItemEventFile(this);
    }

    public void compile() {
        itemEvents.compile();
    }

    public Namespace getNamespace() {
        return compiler.getModule().createNamespace(compiler.getDefaultNamespace());
    }

    public Function getTickFunction() {
        if(tickFunction == null) {
            tickFunction = getNamespace().functions.create("trident/tick");
            compiler.getModule().minecraft.tags.functionTags.create("tick").addValue(new FunctionReference(tickFunction));
        }
        return tickFunction;
    }

    public TridentCompiler getCompiler() {
        return compiler;
    }
}
