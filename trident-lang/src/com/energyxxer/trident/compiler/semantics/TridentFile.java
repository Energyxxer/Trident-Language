package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.util.logger.Debug;

import java.nio.file.Path;

public class TridentFile {
    private final TridentCompiler compiler;
    private final CommandModule module;
    private final Namespace namespace;
    private TokenPattern<?> pattern;

    private Function function;



    public TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this.compiler = compiler;
        this.module = compiler.getModule();
        this.namespace = module.createNamespace(relSourcePath.getName(0).toString());
        this.pattern = filePattern;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length());
        this.function = namespace.functions.create(functionPath);

        Debug.log("Created function " + function);
        Debug.log(filePattern);

        Debug.log(filePattern.find("..DIRECTIVES"));
    }
}
