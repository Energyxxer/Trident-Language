package com.energyxxer.trident.filewalkers;

import com.energyxxer.commodore.module.ModulePackGenerator;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.enxlex.lexical_analysis.token.SourceFile;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.in.ProjectReader;
import com.energyxxer.prismarine.util.PathMatcher;
import com.energyxxer.prismarine.walker.FileWalker;
import com.energyxxer.prismarine.walker.FileWalkerStop;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.worker.tasks.SetupBuildConfigTask;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupResourcePackTask;
import com.energyxxer.trident.worker.tasks.SetupTypeMapTask;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class CompilerWalkers {

    public static final FileWalkerStop<PrismarineCompiler> FUNCTION_STOP = new FileWalkerStop<PrismarineCompiler>("datapack/data/(*)/functions/(**).mcfunction") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {
            String functionName = pathMatchResult.groups[1] + '/' + pathMatchResult.groups[2];
            Debug.log("Function name: " + functionName);
            Namespace ns = worker.output.get(SetupModuleTask.INSTANCE).getNamespace(pathMatchResult.groups[1]);
            if(!ns.functions.exists(functionName)) {
                ns.functions.create(functionName).setExport(false);
            }

            return false;
        }
    };

    public static final FileWalkerStop<PrismarineCompiler> DATA_EXPORTABLE_STOP = new FileWalkerStop<PrismarineCompiler>("datapack/(**.*)") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {
            if(relativePath.getFileName().endsWith(Trident.FUNCTION_EXTENSION)) return true;

            ProjectReader.Result result = walker.getReader().startQuery(relativePath)
                    .needsBytes()
                    .perform();

            byte[] data = result.getBytes();
            worker.output.get(SetupModuleTask.INSTANCE).exportables.add(new RawExportable(pathMatchResult.groups[1], data));

            return true;
        }
    };

    public static final FileWalkerStop<PrismarineCompiler> RESOURCE_EXPORTABLE_STOP = new FileWalkerStop<PrismarineCompiler>("resources/(**.*)") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {
            ResourcePackGenerator resourcePack = worker.output.get(SetupResourcePackTask.INSTANCE);
            if(resourcePack == null) return true;

            TridentBuildConfiguration buildConfig = worker.output.get(SetupBuildConfigTask.INSTANCE);

            ProjectReader.Query query = walker.getReader().startQuery(relativePath)
                    .needsBytes();

            if(resourcePack.getOutputType() == ModulePackGenerator.OutputType.FOLDER && !buildConfig.cleanResourcePackOutput) {
                query.skipIfNotChanged();
            }

            ProjectReader.Result result = query.perform();

            if(result != null) {
                resourcePack.exportables.add(new RawExportable(pathMatchResult.groups[1], result.getBytes()));
            }

            return true;
        }
    };

    public static final FileWalkerStop<PrismarineCompiler> TYPE_MAP_STOP = new FileWalkerStop<PrismarineCompiler>("internal/**.nbttm") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {

            ProjectReader.Result result = walker.getReader().startQuery(relativePath)
                    .needsString()
                    .perform();

            String str = result.getString();
            worker.output.get(SetupTypeMapTask.INSTANCE).parsing.parseNBTTMFile(new SourceFile(file), str);

            return true;
        }
    };

    private CompilerWalkers() {}
}
