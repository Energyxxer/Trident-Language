package com.energyxxer.trident.compiler.util;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.summary.ProjectSummarizer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.ParsingSignature;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.util.logger.Debug;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
import java.util.Map;

import static com.energyxxer.trident.compiler.TridentCompiler.DEFAULT_CHARSET;

public class TridentProjectSummarizer implements ProjectSummarizer {
    private File rootDir;
    private Path dataPath;
    private Thread thread;
    private DefinitionPack[] defaultDefinitionPacks;
    private DefinitionPack[] definitionPacks;
    private Map<String, DefinitionPack> definitionPackAliases = null;
    private CommandModule module;
    private HashMap<String, ParsingSignature> filePatterns = new HashMap<>();

    private TridentProjectSummary summary = new TridentProjectSummary();
    private ArrayList<java.lang.Runnable> completionListeners = new ArrayList<>();

    public TridentProjectSummarizer(File rootDir) {
        this(rootDir, new DefinitionPack[] {StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_RELEASE});
    }

    public TridentProjectSummarizer(File rootDir, DefinitionPack[] definitionPacks) {
        this(rootDir, definitionPacks, null);
    }

    public TridentProjectSummarizer(File rootDir, DefinitionPack[] definitionPacks, Map<String, DefinitionPack> definitionPackAliases) {
        this.rootDir = rootDir;
        this.dataPath = rootDir.toPath().resolve("datapack").resolve("data");
        this.definitionPacks = definitionPacks;
        this.definitionPackAliases = definitionPackAliases;
    }

    private JsonObject properties;

    public void start() {
        this.thread = new Thread(this::runSummary,"Trident-Compiler-Summarizer[" + rootDir.getName() + "]");
        //report = new CompilerReport();
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

        try {
            properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + TridentCompiler.PROJECT_FILE_NAME)), JsonObject.class);
        } catch(JsonSyntaxException | IOException x) {
            logException(x);
            return;
        }

        try {
            module = TridentCompiler.createModuleForProject(rootDir.getName(), rootDir, properties, definitionPacks != null ? definitionPacks : defaultDefinitionPacks, definitionPackAliases);
        } catch(IOException x) {
            logException(x);
            return;
        }

        ArrayList<TridentProjectSummarizer> dependencies = new ArrayList<>();

        if(properties.has("dependencies") && properties.get("dependencies").isJsonArray()) {
            for(JsonElement rawElem : properties.get("dependencies").getAsJsonArray()) {
                if(rawElem.isJsonObject()) {
                    JsonObject obj = rawElem.getAsJsonObject();
                    if(obj.has("path") && obj.get("path").isJsonPrimitive() && obj.get("path").getAsJsonPrimitive().isString()) {
                        String dependencyPath = obj.get("path").getAsString();

                        dependencies.add(new TridentProjectSummarizer(TridentCompiler.newFileObject(dependencyPath, rootDir)));
                    }
                }
            }
        }

        for(TridentProjectSummarizer dependency : dependencies) {
            dependency.setParentSummarizer(this);
            dependency.setSourceCache(this.getSourceCache());
            dependency.setDefaultDefinitionPacks(definitionPacks != null ? definitionPacks : defaultDefinitionPacks);
            try {
                dependency.runSummary();
            } catch(Exception ex) {
                logException(ex);
                return;
            }
            this.setSourceCache(dependency.getSourceCache());
            this.summary.join(dependency.summary);
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
                                lex.tokenizeParse(file, str, new TridentLexerProfile(module));
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
                            try {
                                JsonObject soundsjson = new Gson().fromJson(new FileReader(file), JsonObject.class);
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
        throw new IllegalArgumentException();
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

    private void setDefaultDefinitionPacks(DefinitionPack[] defaultDefinitionPacks) {
        this.defaultDefinitionPacks = defaultDefinitionPacks;
    }
}
