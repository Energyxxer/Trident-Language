package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerGroup(
        classes="CharacterLib,FileLib,JsonLib,MathLib,ProjectLib,RandomLib,ReflectionLib,TagLib,TextLib,TypeLib"
)
public interface DefaultLibraryProvider {
    void populate(ISymbolContext globalCtx, TridentCompiler compiler);
}
