package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@AnalyzerMember(key = "Types")
public class TypeLib implements DefaultLibraryProvider {
    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject block = new DictionaryObject();

        block.put("exists",
                new MethodWrapper<>("exists", ((instance, params) -> {
                    TridentUtil.ResourceLocation loc = TridentUtil.ResourceLocation.createStrict((String)params[0]);
                    if(loc == null) return false;
                    return compiler.getModule().namespaceExists(loc.namespace) && compiler.getModule().getNamespace(loc.namespace).types.block.exists(loc.body);
                }), String.class).createForInstance(null));
        stack.getGlobal().put(new Symbol("Block", Symbol.SymbolAccess.GLOBAL, block));
    }
}
