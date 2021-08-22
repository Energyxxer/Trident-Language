package com.energyxxer.trident.filewalkers;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.ModulePackGenerator;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeNotFoundException;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.lexical_analysis.token.SourceFile;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.in.ProjectReader;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.util.PathMatcher;
import com.energyxxer.prismarine.walker.FileWalker;
import com.energyxxer.prismarine.walker.FileWalkerStop;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.worker.tasks.SetupBuildConfigTask;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupResourcePackTask;
import com.energyxxer.trident.worker.tasks.SetupTypeMapTask;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.logger.Debug;
import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.energyxxer.trident.extensions.EJsonElement.getAsBooleanOrNull;
import static com.energyxxer.trident.extensions.EJsonElement.getAsStringOrNull;

public class CompilerWalkers {

    public static final FileWalkerStop<PrismarineCompiler> FUNCTION_STOP = new FileWalkerStop<PrismarineCompiler>("datapack/data/(*)/functions/(**).mcfunction") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {
            String namespace = pathMatchResult.groups[1];
            String body = pathMatchResult.groups[2];
            Debug.log("Function name: " + namespace + ":" + body);
            Namespace ns = worker.output.get(SetupModuleTask.INSTANCE).getNamespace(pathMatchResult.groups[1]);
            if(!ns.functions.exists(body)) {
                ns.functions.create(body).setExport(false);
            }

            return false;
        }
    };

    public static final FileWalkerStop<PrismarineCompiler> TAG_STOP = new FileWalkerStop<PrismarineCompiler>("datapack/data/(*)/tags/(*)/(**).json") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {
            String namespaceName = pathMatchResult.groups[1];
            String categoryDirName = pathMatchResult.groups[2];
            String tagName = pathMatchResult.groups[3];

            CommandModule module = worker.output.get(SetupModuleTask.INSTANCE);
            Namespace namespace = module.getNamespace(namespaceName);

            ProjectReader.Result result = walker.getReader().startQuery(relativePath, worker)
                    .needsString()
                    .perform();
            String content = result.getString();

            for(TagGroup<?> group : namespace.tags.getGroups()) {
                if(categoryDirName.equals(group.getDirectoryName())) {
                    Tag tag = group.getOrCreate(tagName);
                    Debug.log("Created tag " + tag);
                    JsonObject obj;
                    try {
                        obj = new Gson().fromJson(content, JsonObject.class);
                    } catch(JsonSyntaxException x) {
                        walker.getReport().addNotice(new Notice(NoticeType.ERROR, "Invalid JSON in " + group.getCategory().toLowerCase() + " tag '" + tag + "': " + x.getMessage(), new Token("", new SourceFile(file), new StringLocation(0))));
                        return false;
                    }

                    tag.setOverridePolicy(Tag.OverridePolicy.valueOf(JsonTraverser.getThreadInstance().reset(obj).get("replace").asBoolean(Tag.OverridePolicy.DEFAULT_POLICY.valueBool)));
                    tag.setExport(true);

                    JsonArray values = JsonTraverser.getThreadInstance().reset(obj).get("values").asJsonArray();

                    if(values != null) {
                        for(JsonElement elem : values) {
                            Type value;
                            Tag.TagValueMode valueMode = Tag.TagValueMode.REQUIRED;

                            String rawId;

                            if(elem.isJsonObject()) {
                                rawId = getAsStringOrNull(elem.getAsJsonObject().get("id"));
                                valueMode = (Boolean.FALSE.equals(getAsBooleanOrNull(elem.getAsJsonObject().get("required")))) ? Tag.TagValueMode.OPTIONAL : Tag.TagValueMode.REQUIRED;
                            } else {
                                rawId = getAsStringOrNull(elem);
                            }

                            if(rawId == null) continue;
                            boolean isTag = rawId.startsWith("#");
                            if(isTag) rawId = rawId.substring(1);
                            ResourceLocation loc = new ResourceLocation(rawId);

                            if(isTag) {
                                Tag created = module.getNamespace(loc.namespace).getTagManager().getGroup(group.getCategory()).getOrCreate(loc.body);
                                created.setExport(true);
                                value = created;
                            } else {
                                Type created;
                                if(group.getCategory().equals(FunctionReference.CATEGORY)) {
                                    created = new FunctionReference(module.getNamespace(loc.namespace), loc.body);
                                } else {
                                    try {
                                        created = module.getNamespace(loc.namespace).getTypeManager().getOrCreateDictionary(group.getCategory(), true).get(loc.body);
                                    } catch(TypeNotFoundException x) {
                                        walker.getReport().addNotice(new Notice(NoticeType.WARNING, "Invalid value in " + group.getCategory().toLowerCase() + " tag '" + tag + "': " + loc + " is not a valid " + group.getCategory().toLowerCase() + " type", new Token("", new SourceFile(file), new StringLocation(0))));
                                        continue;
                                    }
                                }
                                value = created;
                            }

                            tag.addValue(value, valueMode);
                        }
                    }
                    break;
                }
            }

            return true;
        }
    };

    public static final FileWalkerStop<PrismarineCompiler> DATA_EXPORTABLE_STOP = new FileWalkerStop<PrismarineCompiler>("datapack/(**.*)") {
        @Override
        public boolean accept(File file, Path relativePath, PathMatcher.Result pathMatchResult, PrismarineProjectWorker worker, FileWalker<PrismarineCompiler> walker) throws IOException {
            if(relativePath.getFileName().endsWith(Trident.FUNCTION_EXTENSION)) return true;

            ProjectReader.Result result = walker.getReader().startQuery(relativePath, worker)
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

            ProjectReader.Query query = walker.getReader().startQuery(relativePath, worker)
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

            ProjectReader.Result result = walker.getReader().startQuery(relativePath, worker)
                    .needsString()
                    .perform();

            String str = result.getString();
            worker.output.get(SetupTypeMapTask.INSTANCE).parsing.parseNBTTMFile(new SourceFile(file), str);

            return true;
        }
    };

    private CompilerWalkers() {}
}
