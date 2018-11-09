package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.report.Notice;

public interface CompilerExtension {
    TridentCompiler getCompiler();

    default void reportNotice(Notice n) {
        getCompiler().getReport().addNotice(n);
    }
}
