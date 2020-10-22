package com.energyxxer.trident.compiler.resourcepack;


import com.energyxxer.commodore.module.ModulePackGenerator;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.out.PrismarineExportablePack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ResourcePackGenerator extends PrismarineExportablePack {
    public ResourcePackGenerator(@Nullable PrismarineCompiler compiler, @NotNull File outFile) {
        super(compiler, outFile);
    }

    public ResourcePackGenerator(@Nullable PrismarineCompiler compiler, @NotNull File outFile, ModulePackGenerator.@NotNull OutputType outputType) {
        super(compiler, outFile, outputType);
    }

    @Override
    public String getProgressMessage() {
        return "Generating resource pack";
    }
}
