package com.energyxxer.trident.compiler.util;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.summary.ProjectSummarizer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.ParsingSignature;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentProjectWorker;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.util.logger.Debug;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.energyxxer.trident.compiler.TridentCompiler.DEFAULT_CHARSET;

public class TridentProjectSummarizer implements ProjectSummarizer {
    private File rootDir;
    private Path dataPath;
    private Thread thread;
    private TridentBuildConfiguration buildConfig;
    private CommandModule module;
    private HashMap<String, ParsingSignature> filePatterns = new HashMap<>();

    private TridentProjectSummary summary = new TridentProjectSummary();
    private ArrayList<java.lang.Runnable> completionListeners = new ArrayList<>();

    private TridentProjectWorker worker;
    private boolean workerHasWorked = false;

    public TridentProjectSummarizer(File rootDir, TridentBuildConfiguration buildConfig) {
        this.rootDir = rootDir;
        this.dataPath = rootDir.toPath().resolve("datapack").resolve("data");
        this.buildConfig = buildConfig;

        this.worker = new TridentProjectWorker(rootDir);
        this.workerHasWorked = false;
    }

    public TridentProjectSummarizer(TridentProjectWorker worker) {
        this.rootDir = worker.rootDir;
        this.dataPath = rootDir.toPath().resolve("datapack").resolve("data");
        this.buildConfig = worker.getBuildConfig();

        this.worker = worker;
        this.workerHasWorked = true;
    }

    private JsonObject properties;

    public void start() {
        this.thread = new Thread(this::runSummary,"Trident-Compiler-Summarizer[" + rootDir.getName() + "]");
        //report = new Report();
        thread.start();
    }

    private void runSummary() {

        if(parentSummarizer != null) {
            TridentProjectSummarizer next = parentSummarizer;
            while(next != null) {
                if(next.rootDir.equals(this.rootDir)) {
                    return;
                }
                next = next.parentSummarizer;
            }
        }

        worker.setup.setupProperties = true;
        worker.setup.setupModule = true;
        worker.setup.setupDependencies = true;
        worker.setBuildConfig(buildConfig);

        try {
            if(!workerHasWorked) worker.work();
        } catch(IOException x) {
            logException(x);
            return;
        }

        properties = worker.output.properties;
        module = worker.output.module;

        for(TridentProjectWorker subWorker : worker.output.dependencies) {
            TridentProjectSummarizer subSummarizer = subWorker.createSummarizer();
            subSummarizer.setParentSummarizer(this);
            subSummarizer.setSourceCache(this.getSourceCache());
            try {
                subSummarizer.runSummary();
            } catch(Exception ex) {
                logException(ex);
                return;
            }
            this.setSourceCache(subSummarizer.getSourceCache());
            this.summary.join(subSummarizer.summary);
        }

        TridentCompiler.summarizeLibraries(summary);

        // Add default minecraft types and tags:
        for(Namespace ns : module.getAllNamespaces()) {
            for(TypeDictionary typeDict : ns.types.getAllDictionaries()) {
                String category = typeDict.getCategory();
                for(Type type : typeDict.list()) {
                    if(type instanceof AliasType) {
                        summary.addType(category, new TridentUtil.ResourceLocation(((AliasType) type).getAliasNamespace().getName() + ":" + ((AliasType) type).getAliasName()));
                    } else {
                        summary.addType(category, new TridentUtil.ResourceLocation(type.toString()));
                    }
                }
            }
            for(TagGroup<?> group : ns.tags.getGroups()) {
                String category = group.getCategory();
                for(Tag tag : group.getAll()) {
                    summary.addTag(category, new TridentUtil.ResourceLocation(tag.toString()));
                }
            }
        }
        JsonObject defaultSounds = ((JsonObject) module.getResource("sounds.json"));
        if(defaultSounds != null) {
            for(String key : defaultSounds.keySet()) {
                summary.addSoundEvent(new TridentUtil.ResourceLocation("minecraft:" + key));
            }
        }

        TokenStream ts = new TokenStream();
        LazyLexer lex = new LazyLexer(ts, new TridentProductions(module).FILE);
        recursivelyParse(lex, rootDir);

        //report.addNotices(lex.getNotices());
        //report.addNotices(typeMap.getNotices());
        /*if(report.getTotal() > 0) {
            finalizeCompilation();
            return;
        }*/

        for(java.lang.Runnable r : completionListeners) {
            r.run();
        }
    }

    private TridentProjectSummarizer parentSummarizer = null;

    public TridentProjectSummarizer getParentSummarizer() {
        return parentSummarizer;
    }

    public void setParentSummarizer(TridentProjectSummarizer parentSummarizer) {
        this.parentSummarizer = parentSummarizer;
    }

    private String toSourceCacheKey(File file) {
        return dataPath.relativize(file.toPath()).toString().replace(File.separator, "/");
    }

    private void recursivelyParse(LazyLexer lex, File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory() && (!file.getParentFile().equals(rootDir) || Arrays.asList("datapack", "resources", "internal").contains(file.getName()))) {
                recursivelyParse(lex, file);
            } else {
                if(file.toPath().startsWith(dataPath)) {
                    Path relPath = dataPath.relativize(file.toPath());
                    if(name.endsWith(".tdn") && relPath.getNameCount() >= 2 && relPath.getName(1).startsWith("functions")) {
                        try {
                            String str = new String(Files.readAllBytes(Paths.get(file.getPath())), DEFAULT_CHARSET);
                            int hashCode = str.hashCode();

                            if(!filePatterns.containsKey(toSourceCacheKey(file)) || (filePatterns.get(toSourceCacheKey(file)).getHashCode() != hashCode || filePatterns.get(toSourceCacheKey(file)).getSummary() == null)) {
                                TridentSummaryModule fileSummary = new TridentSummaryModule(summary);
                                fileSummary.setFileLocation(new TridentUtil.ResourceLocation(relPath.getName(0) + ":" + relPath.subpath(2, relPath.getNameCount()).toString().replace(File.separator, "/").replaceAll(".tdn$","")));
                                lex.setSummaryModule(fileSummary);
                                lex.tokenizeParse(file, str, new TridentLexerProfile());
                                this.summary.store(file, fileSummary);

                                if (lex.getMatchResponse().matched) {
                                    filePatterns.put(toSourceCacheKey(file), new ParsingSignature(hashCode, lex.getMatchResponse().pattern, lex.getSummaryModule()));
                                }
                            } else if(filePatterns.containsKey(toSourceCacheKey(file))) {
                                this.summary.store(file, ((TridentSummaryModule) filePatterns.get(toSourceCacheKey(file)).getSummary()));
                            }
                        } catch (IOException x) {
                            logException(x);
                        }
                    } else {
                        if (name.endsWith(".json") && relPath.getName(1).startsWith("tags")) {

                            String namespaceName = relPath.getName(0).toString();

                            relPath = relPath.subpath(2, relPath.getNameCount());
                            String tagDir = relPath.getName(0).toString();

                            if(relPath.getNameCount() <= 1) {
                                Debug.log("Tag is not in a type category folder: " + relPath);
                                return;
                            }
                            relPath = relPath.subpath(1, relPath.getNameCount());

                            String tagName = relPath.toString().replace(File.separator, "/");
                            tagName = tagName.substring(0, tagName.length() - ".json".length());

                            TagGroup category = getGroupForDirectoryName(tagDir);
                            if(category != null) {
                                summary.addTag(category.getCategory(), new TridentUtil.ResourceLocation("#" + namespaceName + ":" + tagName));
                            } else {
                                Debug.log("Unknown tag directory name: " + tagDir);
                            }
                        } else {
                            if(name.endsWith(".mcfunction") && relPath.getNameCount() >= 3 && "functions".equals(relPath.getName(1).toString())) {
                                String functionName = relPath.subpath(2, relPath.getNameCount()).toString().replace(File.separator, "/");
                                functionName = functionName.substring(0, functionName.length() - ".mcfunction".length());
                                TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(relPath.getName(0).toString() + ":" + functionName);
                                summary.addRawFunction(loc);
                                Debug.log("Added: " + loc);
                            }
                        }
                    }
                } else if(file.toPath().startsWith(rootDir.toPath().resolve("resources"))) {
                    Path relPath = rootDir.toPath().resolve("resources").resolve("assets").relativize(file.toPath());
                    if(relPath.getNameCount() >= 2) {
                        if(relPath.getNameCount() == 2 && file.getName().equals("sounds.json")) {
                            String namespace = relPath.getName(0).toString();
                            try(FileReader fr = new FileReader(file)) {
                                JsonObject soundsjson = new Gson().fromJson(fr, JsonObject.class);
                                for(String body : soundsjson.keySet()) {
                                    summary.addSoundEvent(new TridentUtil.ResourceLocation(namespace + ":" + body));
                                }
                            } catch(JsonSyntaxException | ClassCastException | IOException ignored) {
                            }
                        }
                    }
                }
            }
        }
    }

    public void addCompletionListener(java.lang.Runnable r) {
        completionListeners.add(r);
    }

    public void removeCompletionListener(java.lang.Runnable r) {
        completionListeners.remove(r);
    }

    private TagGroup getGroupForDirectoryName(String tagDir) {
        for(TagGroup gr : module.minecraft.tags.getGroups()) {
            if(gr.getDirectoryName().equals(tagDir)) return gr;
        }
        return null;
    }

    private void logException(Exception x) {
        x.printStackTrace();
        for(java.lang.Runnable r : completionListeners) {
            r.run();
        }
    }

    public void setSourceCache(HashMap<String, ParsingSignature> cache) {
        if(cache != null)
            this.filePatterns = cache;
    }

    public HashMap<String, ParsingSignature> getSourceCache() {
        return filePatterns;
    }

    public TridentProjectSummary getSummary() {
        return summary;
    }
}
