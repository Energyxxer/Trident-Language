package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@ParserGroup
public interface DefaultLibraryPopulator {
    void populate(SymbolStack stack, TridentCompiler compiler);
}
