package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@AnalyzerGroup
public interface DefaultLibraryProvider {
    void populate(SymbolStack stack, TridentCompiler compiler);
}
