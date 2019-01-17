package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@ParserMember(key = "NBT")
public class NBTLib implements DefaultLibraryProvider {
    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {

    }
}
