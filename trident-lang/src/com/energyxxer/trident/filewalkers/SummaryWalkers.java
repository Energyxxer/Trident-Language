package com.energyxxer.trident.filewalkers;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.prismarine.in.ProjectReader;
import com.energyxxer.prismarine.summaries.PrismarineProjectSummary;
import com.energyxxer.prismarine.util.PathMatcher;
import com.energyxxer.prismarine.walker.FileWalker;
import com.energyxxer.prismarine.walker.FileWalkerStop;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.lexer.summaries.TridentProjectSummary;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class SummaryWalkers {

    public static final FileWalkerStop<PrismarineProjectSummary> FUNCTION_STOP = new FileWalkerStop<PrismarineProjectSummary>("datapack/data/(*)/functions/(**).mcfunction") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineProjectSummary> walker) throws IOException {
            String functionName = pathMatchResult.groups[1] + '/' + pathMatchResult.groups[2];
            Namespace ns = worker.output.get(SetupModuleTask.INSTANCE).getNamespace(pathMatchResult.groups[1]);
            if(!ns.functions.exists(functionName)) {
                ns.functions.create(functionName).setExport(false);
            }

            ResourceLocation loc = new ResourceLocation(pathMatchResult.groups[1] + ":" + pathMatchResult.groups[2]);
            ((TridentProjectSummary) walker.getSubject()).addRawFunction(loc);

            return true;
        }
    };

    public static final FileWalkerStop<PrismarineProjectSummary> TAG_STOP = new FileWalkerStop<PrismarineProjectSummary>("datapack/data/(*)/tags/(*)/(**).json") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineProjectSummary> walker) throws IOException {

            String namespaceName = pathMatchResult.groups[1];
            String categoryDirName = pathMatchResult.groups[2];
            String tagName = pathMatchResult.groups[3];

            CommandModule module = worker.output.get(SetupModuleTask.INSTANCE);
            Namespace namespace = module.getNamespace(namespaceName);

            for(TagGroup<?> group : namespace.tags.getGroups()) {
                if (categoryDirName.equals(group.getDirectoryName())) {
                    group.getOrCreate(tagName);
                    break;
                }
            }

            return true;
        }
    };

    public static final FileWalkerStop<PrismarineProjectSummary> SOUNDS_JSON_STOP = new FileWalkerStop<PrismarineProjectSummary>("resources/assets/*/sounds.json") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineProjectSummary> walker) throws IOException {
            TridentProjectSummary summary = ((TridentProjectSummary) walker.getSubject());

            try {
                ProjectReader.Result result = walker.getReader().startQuery(relativePath)
                        .needsJSON()
                        .perform();

                for(String body : result.getJsonObject().keySet()) {
                    summary.addSoundEvent(new ResourceLocation(body));
                }
            } catch(JsonSyntaxException | ClassCastException | IOException ignored) {
            }

            return true;
        }
    };

    private SummaryWalkers() {}
}
