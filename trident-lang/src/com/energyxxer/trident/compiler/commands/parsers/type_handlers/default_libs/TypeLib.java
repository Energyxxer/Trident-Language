package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@ParserMember(key = "Types")
public class TypeLib implements DefaultLibraryPopulator {
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
