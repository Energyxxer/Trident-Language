package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.ModulePackGenerator;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeNotFoundException;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.commodore.versioning.JavaEditionVersion;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatures;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.ParsingSignature;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.symbols.GlobalSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.util.TridentProjectSummary;
import com.energyxxer.util.Lazy;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.util.processes.AbstractProcess;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.energyxxer.trident.extensions.EJsonElement.getAsStringOrNull;
import static com.energyxxer.trident.extensions.EJsonObject.getAsBoolean;

public class TridentCompiler extends AbstractProcess {

    public static final String PROJECT_FILE_NAME = ".tdnproj";
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String TRIDENT_LANGUAGE_VERSION = "0.5.0-beta";

    //Resources
    private DefinitionPack[] defaultDefinitionPacks;
    private DefinitionPack[] definitionPacks;
    private Map<String, DefinitionPack> definitionPackAliases = null;
    private VersionFeatures featureMap;
    private String[] typeMapsRaw = null;
    private NBTTypeMap typeMap;

    //Output objects
    private final File rootDir;
    private CommandModule module;
    private ResourcePackGenerator resourcePack;
    private SpecialFileManager specialFileManager;

    //Utilities
    private Lazy<Objective> globalObjective;

    //Properties
    private JsonObject properties = null;
    private int languageLevel = 1;
    private String defaultNamespace = null;

    //Caller Feedback
    private CompilerReport report = null;

    //File Structure Tracking
    private ArrayList<String> ownFiles = new ArrayList<>();
    private HashMap<String, ParsingSignature> filePatterns = new HashMap<>();
    private HashMap<String, TridentFile> files = new HashMap<>();

    //idk why we need a gson object but here it is
    private Gson gson;

    //Stacks
    private GlobalSymbolContext global = new GlobalSymbolContext(this);
    private CallStack callStack = new CallStack();
    private TryStack tryStack = new TryStack();
    private Stack<TridentFile> writingStack = new Stack<>();

    //Caches
    private HashMap<String, ParsingSignature> inResourceCache = new HashMap<>();
    private HashMap<String, ParsingSignature> outResourceCache = new HashMap<>();
    private Dependency.Mode dependencyMode;

    public TridentCompiler(File rootDir) {
        super("Trident-Compiler[" + rootDir.getName() + "]");
        this.rootDir = rootDir;
        initializeThread(this::runCompilation);
        this.thread.setUncaughtExceptionHandler((th, ex) -> {
            logException(ex);
        });
        report = new CompilerReport();

        specialFileManager = new SpecialFileManager(this);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        this.gson = gsonBuilder.create();
        globalObjective = new Lazy<>(() -> this.getModule().getObjectiveManager().create("trident_global"));
    }

    public void setStartingDefinitionPacks(DefinitionPack[] definitionPacks) {
        this.definitionPacks = definitionPacks;
    }

    public void setStartingFeatureMap(VersionFeatures featureMap) {
        this.featureMap = featureMap;
    }

    public void setStartingRawTypeMaps(String[] typeMapsRaw) {
        this.typeMapsRaw = typeMapsRaw;
    }

    private void runCompilation() {

        if(parentCompiler != null) {
            TridentCompiler next = parentCompiler;
            while(next != null) {
                if(next.rootDir.equals(this.rootDir)) {
                    Debug.log("Stopping circular dependencies");
                    finalizeProcess(false);
                    return;
                }
                next = next.parentCompiler;
            }
        }

        this.setProgress("Initializing analyzers");

        AnalyzerManager.initialize();

        this.setProgress("Reading project settings file");
        try {
            properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
        } catch(JsonSyntaxException | IOException x) {
            logException(x, "Error while reading project settings: ");
            return;
        }

        if(properties.has("language-level") && properties.get("language-level").isJsonPrimitive() && properties.get("language-level").getAsJsonPrimitive().isNumber()) {
            languageLevel = properties.get("language-level").getAsInt();
            if(languageLevel < 1 || languageLevel > 3) {
                report.addNotice(new Notice(NoticeType.ERROR, "Invalid language level: " + languageLevel));
                languageLevel = 1;
            }
        }

        if(properties.has("default-namespace") && properties.get("default-namespace").isJsonPrimitive() && properties.get("default-namespace").getAsJsonPrimitive().isString() && !properties.get("default-namespace").getAsString().isEmpty()) {
            defaultNamespace = properties.get("default-namespace").getAsString().trim();
        }

        if(properties.has("resources-output") && properties.get("resources-output").isJsonPrimitive() && properties.get("resources-output").getAsJsonPrimitive().isString()) {
            resourcePack = new ResourcePackGenerator(this, newFileObject(properties.get("resources-output").getAsString()));
        }

        VersionFeatureManager.setActiveFeatureMap(featureMap);

        this.setProgress("Importing vanilla definitions");
        try {
            module = createModuleForProject(rootDir.getName(), rootDir, properties, definitionPacks != null ? definitionPacks : defaultDefinitionPacks, definitionPackAliases);
        } catch(IOException x) {
            logException(x, "Error while importing vanilla definitions: ");
            return;
        }

        typeMap = new NBTTypeMap(module);

        if(typeMapsRaw != null) {
            for(String rawContent : typeMapsRaw) {
                typeMap.parsing.parseNBTTMFile(rootDir, rawContent);
            }
        } else {
            typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("common.nbttm"));
            typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("entities.nbttm"));
            typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("block_entities.nbttm"));
        }

        this.setProgress("Adding native methods");

        {
            for(DefaultLibraryProvider lib : AnalyzerManager.getAllParsers(DefaultLibraryProvider.class)) {
                lib.populate(global, this);
            }
        }

        this.setProgress("Parsing files");
        TokenStream ts = new TokenStream();
        LazyLexer lex = new LazyLexer(ts, new TridentProductions(module).FILE);
        recursivelyParse(lex, rootDir);

        report.addNotices(lex.getNotices());
        report.addNotices(typeMap.getNotices());
        if(report.getTotal() > 0) {
            finalizeProcess(false);
            return;
        }

        this.setProgress("Resolving dependencies");
        ArrayList<Dependency> dependencies = new ArrayList<>();

        if(properties.has("dependencies") && properties.get("dependencies").isJsonArray()) {
            for(JsonElement rawElem : properties.get("dependencies").getAsJsonArray()) {
                if(rawElem.isJsonObject()) {
                    JsonObject obj = rawElem.getAsJsonObject();
                    if(obj.has("path") && obj.get("path").isJsonPrimitive() && obj.get("path").getAsJsonPrimitive().isString()) {
                        String dependencyPath = obj.get("path").getAsString();
                        Dependency dependency = new Dependency(new TridentCompiler(newFileObject(dependencyPath)));
                        dependency.compiler.setStartingRawTypeMaps(typeMapsRaw);
                        dependency.compiler.setStartingFeatureMap(featureMap);
                        dependency.compiler.setDefaultDefinitionPacks(definitionPacks != null ? definitionPacks : defaultDefinitionPacks);
                        if(obj.has("export") && obj.get("export").isJsonPrimitive() && obj.get("export").getAsJsonPrimitive().isBoolean()) {
                            dependency.doExport = obj.get("export").getAsBoolean();
                        }
                        if(obj.has("mode") && obj.get("mode").isJsonPrimitive() && obj.get("mode").getAsJsonPrimitive().isString()) {
                            switch(obj.get("mode").getAsString()) {
                                case "precompile": {
                                    dependency.mode = Dependency.Mode.PRECOMPILE;
                                    break;
                                }
                                case "combine": {
                                    dependency.mode = Dependency.Mode.COMBINE;
                                    break;
                                }
                            }
                        }

                        dependencies.add(dependency);
                    }
                }
            }
        }

        for(Dependency dependency : dependencies) {
            dependency.compiler.setParentCompiler(this);
            dependency.compiler.setDependencyMode(dependency.mode);
            dependency.compiler.addProgressListener((process) -> this.updateStatus(process.getStatus()));
            try {
                dependency.compiler.runCompilation();
            } catch(Exception ex) {
                logException(ex);
                return;
            }
            report.addNotices(dependency.compiler.getReport().getAllNotices());
            if(!dependency.compiler.isSuccessful()) {
                finalizeProcess(false);
            } else {
                files.putAll(dependency.compiler.files);
                if(!dependency.doExport) {
                    dependency.compiler.module.propagateExport(false);
                    for(TridentFile file : dependency.compiler.files.values()) {
                        file.setShouldExportFunction(false);
                    }
                }
                module.join(dependency.compiler.module);
                global.join(dependency.compiler.global);

                dependency.compiler.setRerouteRoot(true);
            }
        }

        Resources.populate(ownFiles, filePatterns);

        for(String key : ownFiles) {
            ParsingSignature value = filePatterns.get(key);
            Path relativePath = Paths.get(key);
            if("functions".equals(relativePath.getName(1).toString())) {
                try {
                    files.put(key, new TridentFile(this, relativePath, value.getPattern()));
                } catch(CommodoreException ex) {
                    report.addNotice(new Notice(NoticeType.ERROR, ex.toString(), value.getPattern()));
                    break;
                }
            }
        }

        if(parentCompiler != null && dependencyMode == Dependency.Mode.COMBINE) {
            finalizeProcess(true);
            return;
        }

        files.values().forEach(TridentFile::checkCircularRequires);

        if(report.hasErrors()) {
            finalizeProcess(false);
            return;
        }

        ArrayList<TridentFile> sortedFiles = new ArrayList<>(files.values());

        files.values().forEach(this::getAllRequires);

        sortedFiles.sort((a,b) -> (a.isCompileOnly() != b.isCompileOnly()) ? a.isCompileOnly() ? -2 : 2 : (int) Math.signum((b.getPriority() - a.getPriority()) * 1000));

        updateProgress(0);
        float delta = 1f / sortedFiles.size();
        for(TridentFile file : sortedFiles) {
            this.setProgress("Analyzing " + file.getResourceLocation());
            try {
                file.resolveEntries();
            } catch(ReturnException r) {
                report.addNotice(new Notice(NoticeType.ERROR, "Return instruction outside inner function", r.getPattern()));
            } catch(BreakException b) {
                report.addNotice(new Notice(NoticeType.ERROR, "Break instruction outside loop or switch", b.getPattern()));
            } catch(ContinueException c) {
                report.addNotice(new Notice(NoticeType.ERROR, "Continue instruction outside loop", c.getPattern()));
            }
            updateProgress(getProgress()+delta);
        }

        if(report.hasErrors()) {
            finalizeProcess(false);
            return;
        }

        specialFileManager.compile();

        if(report.hasErrors()) {
            finalizeProcess(false);
            return;
        }


        if(parentCompiler != null && dependencyMode == Dependency.Mode.PRECOMPILE) {
            finalizeProcess(true);
            return;
        }

        updateProgress(-1);
        this.setProgress("Generating data pack");
        if(properties.has("datapack-output") && properties.get("datapack-output").isJsonPrimitive() && properties.get("datapack-output").getAsJsonPrimitive().isString()) {
            try {
                module.compile(newFileObject(properties.get("datapack-output").getAsString()));
            } catch(IOException x) {
                logException(x, "Error while generating output data pack: ");
            }
        } else {
            this.report.addNotice(new Notice(NoticeType.ERROR, "Datapack output directory not specified"));
        }

        updateProgress(0);
        this.setProgress("Generating resource pack");
        try {
            resourcePack.generate();
        } catch(IOException x) {
            logException(x, "Error while generating output resource pack: ");
        }

        finalizeProcess(true);
    }

    private Collection<TridentUtil.ResourceLocation> getAllRequires(TridentFile file) {
        if(file.getCascadingRequires() == null) {
            file.addCascadingRequires(Collections.emptyList());
            file.getRequires().forEach(fl -> file.addCascadingRequires(getAllRequires(getFile(fl))));
        }
        return file.getCascadingRequires();
    }

    private String toSourceCacheKey(File file) {
        Path dataRoot = rootDir.toPath().resolve("datapack").resolve("data");

        return dataRoot.relativize(file.toPath()).toString().replace(File.separator, "/");
    }

    private void recursivelyParse(LazyLexer lex, File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory() && (!file.getParentFile().equals(rootDir) || Arrays.asList("datapack", "resources", "internal").contains(file.getName()))) {
                recursivelyParse(lex, file);
            } else {
                if(file.toPath().startsWith(rootDir.toPath().resolve("datapack"))) {
                    this.setProgress("Parsing file: " + rootDir.toPath().relativize(file.toPath()));
                    if(name.endsWith(".tdn")) {
                        try {
                            String str = new String(Files.readAllBytes(Paths.get(file.getPath())), DEFAULT_CHARSET);
                            int hashCode = str.hashCode();

                            String cacheKey = toSourceCacheKey(file);
                            ownFiles.add(cacheKey);

                            if(!filePatterns.containsKey(cacheKey) || filePatterns.get(cacheKey).getHashCode() != hashCode) {
                                lex.tokenizeParse(file, str, new TridentLexerProfile());

                                if (lex.getMatchResponse().matched) {
                                    filePatterns.put(cacheKey, new ParsingSignature(hashCode, lex.getMatchResponse().pattern, lex.getSummaryModule()));
                                }
                            }
                        } catch (IOException x) {
                            logException(x);
                        }
                    } else {
                        Path dataPath = rootDir.toPath().resolve("datapack").resolve("data");
                        try {
                            if (name.endsWith(".json") && file.toPath().startsWith(dataPath) && dataPath.relativize(file.toPath()).getName(1).startsWith("tags")) {
                                loadTag(file);
                            } else {
                                Path relPath = rootDir.toPath().resolve("datapack").relativize(file.toPath());
                                byte[] data = Files.readAllBytes(file.toPath());
                                module.exportables.add(new RawExportable(relPath.toString().replace(File.separator, "/"), data));


                                if(name.endsWith(".mcfunction") && relPath.getNameCount() >= 4 && "data".equals(relPath.getName(0).toString()) && "functions".equals(relPath.getName(2).toString())) {
                                    String functionName = relPath.subpath(3, relPath.getNameCount()).toString().replace(File.separator, "/");
                                    functionName = functionName.substring(0, functionName.length() - ".mcfunction".length());
                                    Namespace ns = module.getNamespace(relPath.getName(1).toString());
                                    if(!ns.functions.exists(functionName)) {
                                        ns.functions.create(functionName).setExport(false);
                                    }
                                }
                            }
                        } catch (IOException x) {
                            logException(x);
                        }
                    }
                } else if(file.toPath().startsWith(rootDir.toPath().resolve("resources"))) {
                    if(resourcePack == null) break;
                    this.setProgress("Parsing file: " + rootDir.toPath().relativize(file.toPath()));

                    try {
                        String relPath = rootDir.toPath().resolve("resources").relativize(file.toPath()).toString().replace(File.separator, "/");
                        byte[] data = Files.readAllBytes(file.toPath());
                        int hashCode = Arrays.hashCode(data);

                        outResourceCache.put(relPath, new ParsingSignature(hashCode));

                        if(resourcePack.getOutputType() == ModulePackGenerator.OutputType.ZIP
                                || !Objects.equals(inResourceCache.get(relPath), new ParsingSignature(hashCode))) {
                            resourcePack.exportables.add(new RawExportable(relPath, data));
                        }
                    } catch (IOException x) {
                        logException(x);
                    }
                } else if(file.toPath().startsWith(rootDir.toPath().resolve("internal"))) {
                    this.setProgress("Parsing file: " + rootDir.toPath().relativize(file.toPath()));
                    if(name.endsWith(".nbttm")) {
                        try {
                            String str = new String(Files.readAllBytes(Paths.get(file.getPath())), DEFAULT_CHARSET);
                            typeMap.parsing.parseNBTTMFile(file, str);

                        } catch(IOException x) {
                            logException(x);
                        }
                    }
                }
            }
        }
    }

    private void loadTag(File file) throws IOException {
        Path dataPath = rootDir.toPath().resolve("datapack").resolve("data");

        String namespaceName = dataPath.relativize(file.toPath()).getName(0).toString();
        Namespace namespace = module.getNamespace(namespaceName);

        Path relPath = dataPath.relativize(file.toPath());
        relPath = relPath.subpath(2, relPath.getNameCount());
        String tagDir = relPath.getName(0).toString();
        relPath = relPath.subpath(1, relPath.getNameCount());

        String tagName = relPath.toString().replace(File.separator, "/");
        tagName = tagName.substring(0, tagName.length() - ".json".length());

        for(TagGroup<?> group : namespace.tags.getGroups()) {
            if(group.getDirectoryName().equals(tagDir)) {
                Tag tag = group.getOrCreate(tagName);
                tag.setExport(true);
                Debug.log("Created tag " + tag);

                JsonObject obj = gson.fromJson(new FileReader(file), JsonObject.class);

                tag.setOverridePolicy(Tag.OverridePolicy.valueOf(getAsBoolean(obj, "replace", Tag.OverridePolicy.DEFAULT_POLICY.valueBool)));
                tag.setExport(true);
                JsonArray values = obj.getAsJsonArray("values");

                for(JsonElement elem : values) {
                    String value = getAsStringOrNull(elem);
                    if(value == null) continue;
                    boolean isTag = value.startsWith("#");
                    if(isTag) value = value.substring(1);
                    TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(value);

                    if(isTag) {
                        Tag created = module.getNamespace(loc.namespace).getTagManager().getGroup(group.getCategory()).getOrCreate(loc.body);
                        created.setExport(true);
                        tag.addValue(created);
                    } else {
                        Type created;
                        if(group.getCategory().equals(FunctionReference.CATEGORY)) {
                            created = new FunctionReference(module.getNamespace(loc.namespace), loc.body);
                        } else {
                            try {
                                created = module.getNamespace(loc.namespace).getTypeManager().createDictionary(group.getCategory(), true).get(loc.body);
                            } catch(TypeNotFoundException x) {
                                report.addNotice(new Notice(NoticeType.WARNING, "Invalid value in " + group.getCategory().toLowerCase() + " tag '" + tag + "': " + loc + " is not a valid " + group.getCategory().toLowerCase() + " type", new Token("", file, new StringLocation(0))));
                                continue;
                            }
                        }
                        tag.addValue(created);
                    }
                }

                break;
            }
        }
    }

    private boolean rerouteRoot = false;

    private TridentCompiler parentCompiler = null;

    public TridentCompiler getRootCompiler() {
        return parentCompiler != null && rerouteRoot ? parentCompiler.getRootCompiler() : this;
    }
    public void setParentCompiler(TridentCompiler parentCompiler) {
        this.parentCompiler = parentCompiler;
    }

    private void setRerouteRoot(boolean rerouteRoot) {
        this.rerouteRoot = rerouteRoot;
    }

    @Override
    public void updateProgress(float progress) {
        super.updateProgress(progress);
    }

    private void logException(Throwable x) {
        logException(x, "");
    }

    private void logException(Throwable x, String prefix) {
        this.report.addNotice(new Notice(NoticeType.ERROR, prefix+x.toString() + " ; See console for details"));
        x.printStackTrace();
        finalizeProcess(false);
    }

    protected void finalizeProcess(boolean success) {
        this.setProgress("Compilation " + (success ? "completed" : "interrupted") + " with " + report.getTotalsString(), false);
        super.finalizeProcess(success);
    }

    public void setProgress(String message) {
        setProgress(message, true);
    }

    private void setProgress(String message, boolean includeProjectName) {
        updateStatus(message + (includeProjectName ? ("... [" + rootDir.getName() + "]") : ""));
    }

    public CompilerReport getReport() {
        return report;
    }

    public CommandModule getModule() {
        return module;
    }

    public void setReport(CompilerReport report) {
        this.report = report;
    }

    public JsonObject getProperties() {
        return properties;
    }

    private File newFileObject(String path) {
        return newFileObject(path, rootDir);
    }

    public static File newFileObject(String path, File rootDir) {
        path = Paths.get(path.replace("$PROJECT_DIR$", rootDir.getAbsolutePath())).normalize().toString();
        return new File(path);
    }

    public TridentFile getFile(TridentUtil.ResourceLocation loc) {
        for(TridentFile file : files.values()) {
            if(file.getResourceLocation().equals(loc)) return file;
        }
        return null;
    }

    public CallStack getCallStack() {
        return callStack;
    }

    public int getLanguageLevel() {
        return languageLevel;
    }

    private boolean givenDefaultNamespaceNotice = false;

    public Namespace getDefaultNamespace() {
        if(defaultNamespace == null) {
            defaultNamespace = "trident_temp_please_specify_default_namespace";
            if(!givenDefaultNamespaceNotice) {
                report.addNotice(new Notice(NoticeType.WARNING, "Some language features used require a default namespace. Please specify a default namespace in the project settings"));
                givenDefaultNamespaceNotice = true;
            }
        }
        return module.getNamespace(defaultNamespace);
    }

    public SpecialFileManager getSpecialFileManager() {
        return specialFileManager;
    }

    public NBTTypeMap getTypeMap() {
        return typeMap;
    }

    public Type fetchType(TridentUtil.ResourceLocation location, String category) {
        try {
            return module.getNamespace(location.namespace).types.getDictionary(category).get(location.body);
        }
        catch(NullPointerException | TypeNotFoundException x) {
            return null;
        }
    }

    public void setDefinitionPackAliases(Map<String, DefinitionPack> definitionPackAliases) {
        this.definitionPackAliases = definitionPackAliases;
    }

    public void setInResourceCache(HashMap<String, ParsingSignature> inResourceCache) {
        if(inResourceCache != null)
        this.inResourceCache = inResourceCache;
    }

    public HashMap<String, ParsingSignature> getOutResourceCache() {
        return outResourceCache;
    }

    public void setSourceCache(HashMap<String, ParsingSignature> cache) {
        if(cache != null)
        this.filePatterns = cache;
    }

    public HashMap<String, ParsingSignature> getSourceCache() {
        return filePatterns;
    }

    public TryStack getTryStack() {
        return tryStack;
    }

    public File getRootDir() {
        return rootDir;
    }

    public ResourcePackGenerator getResourcePackGenerator() {
        return resourcePack;
    }

    public void pushWritingFile(TridentFile file) {
        writingStack.push(file);
    }

    public void popWritingFile() {
        writingStack.pop();
    }

    public TridentFile getWritingFile() {
        return writingStack.peek();
    }

    public ISymbolContext getGlobalContext() {
        return global;
    }

    public Objective getGlobalObjective() {
        return globalObjective.getValue();
    }

    public Collection<TridentFile> getAllFiles() {
        return files.values();
    }

    public static CommandModule createModuleForProject(String name, File rootDir, DefinitionPack[] definitionPacks) throws IOException {
        JsonObject properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
        return createModuleForProject(name, rootDir, properties, definitionPacks);
    }

    public static CommandModule createModuleForProject(String name, File rootDir, JsonObject properties, DefinitionPack[] definitionPacks) throws IOException {
        return createModuleForProject(name, rootDir, properties, definitionPacks, null);
    }

    public static CommandModule createModuleForProject(String name, File rootDir, DefinitionPack[] definitionPacks, Map<String, DefinitionPack> definitionPackAliases) throws IOException {
        JsonObject properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
        return createModuleForProject(name, rootDir, properties, definitionPacks, definitionPackAliases);
    }

    public static CommandModule createModuleForProject(String name, File rootDir, JsonObject properties, DefinitionPack[] definitionPacks, Map<String, DefinitionPack> definitionPackAliases) throws IOException {
        CommandModule module = new CommandModule(name, "Command Module created with Trident");
        module.getSettingsManager().EXPORT_EMPTY_FUNCTIONS.setValue(true);

        if(definitionPacks == null) {
            definitionPacks = new DefinitionPack[] {StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_RELEASE};
        }

        JavaEditionVersion targetVersion = new JavaEditionVersion(1, 14, 0);
        if(properties.has("target-version") && properties.get("target-version").isJsonArray()) {
            JsonArray rawTargetVersion = properties.get("target-version").getAsJsonArray();
            try {
                int major = rawTargetVersion.get(0).getAsInt();
                int minor = rawTargetVersion.get(1).getAsInt();
                int patch = rawTargetVersion.get(2).getAsInt();

                targetVersion = new JavaEditionVersion(major, minor, patch);
            } catch(IndexOutOfBoundsException x) {
                throw new IOException("Invalid target version array: got less than 3 elements");
            } catch(UnsupportedOperationException x) {
                throw new IOException("Expected string in target version array");
            }
        }

        module.getSettingsManager().setTargetVersion(targetVersion);

        ArrayList<DefinitionPack> toImport = new ArrayList<>();

        if(properties.has("use-definitions") && properties.get("use-definitions").isJsonArray()) {
            for(JsonElement rawElement : properties.getAsJsonArray("use-definitions")) {
                if(rawElement.isJsonPrimitive() && rawElement.getAsJsonPrimitive().isString()) {
                    String element = rawElement.getAsString();
                    if(element.equals("DEFAULT")) {
                        toImport.addAll(Arrays.asList(definitionPacks));
                    } else {
                        File pathToPack = rootDir.toPath().resolve("defpacks").resolve(element).toFile();
                        File pathToZip = new File(pathToPack.getPath() + ".zip");

                        CompoundInput input = null;

                        if(pathToZip.exists() && pathToZip.isFile()) {
                            input = new ZipCompoundInput(pathToZip);
                        } else if(pathToPack.exists() && pathToPack.isDirectory()) {
                            input = new DirectoryCompoundInput(pathToPack);
                        }

                        if(input != null) {
                            toImport.add(new DefinitionPack(input));
                        } else if(definitionPackAliases != null && definitionPackAliases.containsKey(element)) {
                            toImport.add(definitionPackAliases.get(element));
                        } else {
                            throw new FileNotFoundException("Could not find folder nor zip at path '" + pathToPack + "'");
                        }
                    }
                }
            }
        } else {
            toImport.addAll(Arrays.asList(definitionPacks));
        }

        for(DefinitionPack defpack : toImport) {
            module.importDefinitions(defpack);
        }

        if(properties.has("aliases") && properties.get("aliases").isJsonObject()) {
            for(Map.Entry<String, JsonElement> categoryEntry : properties.getAsJsonObject("aliases").entrySet()) {
                String category = categoryEntry.getKey();

                if(categoryEntry.getValue().isJsonObject()) {
                    for(Map.Entry<String, JsonElement> entry : categoryEntry.getValue().getAsJsonObject().entrySet()) {
                        TridentUtil.ResourceLocation alias = TridentUtil.ResourceLocation.createStrict(entry.getKey());
                        TridentUtil.ResourceLocation real = TridentUtil.ResourceLocation.createStrict(entry.getValue().getAsString());
                        if(alias == null) continue;
                        if(real == null) continue;

                        module.getNamespace(alias.namespace).types.getDictionary(category).create((c, ns, n) -> new AliasType(c, ns, n, module.getNamespace(real.namespace), real.body), alias.body);
                        //Debug.log("Created alias '" + alias + "' for '" + real + "'");
                    }
                }
            }
        }
        return module;
    }

    public void setDependencyMode(Dependency.Mode dependencyMode) {
        this.dependencyMode = dependencyMode;
    }

    public Dependency.Mode getDependencyMode() {
        return dependencyMode;
    }

    private void setDefaultDefinitionPacks(DefinitionPack[] defaultDefinitionPacks) {
        this.defaultDefinitionPacks = defaultDefinitionPacks;
    }

    public static void summarizeLibraries(TridentProjectSummary summary) {
        Resources.summarize(summary);
    }

    private static class Resources {
        public static final HashMap<String, String> defaults = new HashMap<>();
        public static final ArrayList<Library> libraries = new ArrayList<>();
        public static final CommandModule dummyModule = new CommandModule("Trident Dummy");

        static {
            defaults.put("common.nbttm", read("/typemaps/common.nbttm"));
            defaults.put("entities.nbttm", read("/typemaps/entities.nbttm"));
            defaults.put("block_entities.nbttm", read("/typemaps/block_entities.nbttm"));

            dummyModule.minecraft.types.entity.create("player");
            dummyModule.minecraft.types.block.create("air");
            dummyModule.minecraft.types.item.create("air");
            dummyModule.minecraft.types.particle.create("cloud");
            dummyModule.minecraft.types.enchantment.create("protection");
            dummyModule.minecraft.types.gamerule.getOrCreate("commandBlockOutput").putProperty("argument", "boolean");
            dummyModule.minecraft.types.gamemode.create("survival");
            dummyModule.minecraft.types.dimension.create("overworld");
            dummyModule.minecraft.types.effect.create("speed");
            dummyModule.minecraft.types.slot.create("weapon.mainhand");
            dummyModule.minecraft.types.difficulty.create("normal");
            dummyModule.minecraft.types.structure.create("Village");
            dummyModule.minecraft.types.fluid.create("water");

            libraries.add(new Library("trident-util/functions/native.tdn", read("/trident_utils/datapack/data/trident-util/functions/native.tdn"), true));
            libraries.add(new Library("trident-util/functions/type_checking.tdn", read("/trident_utils/datapack/data/trident-util/functions/type_checking.tdn")));
            libraries.add(new Library("trident-util/functions/shared.tdn", read("/trident_utils/datapack/data/trident-util/functions/shared.tdn")));
            libraries.add(new Library("trident-util/functions/predicate.tdn", read("/trident_utils/datapack/data/trident-util/functions/predicate.tdn")));
            libraries.add(new Library("trident-util/functions/advancement_triggers.tdn", read("/trident_utils/datapack/data/trident-util/functions/advancement_triggers.tdn")));
        }

        static void populate(ArrayList<String> ownFiles, HashMap<String, ParsingSignature> filePatterns) {
            for(Library lib : libraries) {
                if(lib.summaryOnly) continue;
                ownFiles.add(lib.path);
                filePatterns.put(lib.path, lib.signature);
            }
        }

        static void summarize(TridentProjectSummary summary) {
            for(Library lib : libraries) {
                summary.store(null, lib.fileSummary);
            }
        }

        private static String read(String file) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(NBTTypeMap.class.getResourceAsStream(file)))) {
                StringBuilder sb = new StringBuilder();
                String line;
                for (; (line = br.readLine()) != null; ) {
                    sb.append(line);
                    sb.append("\n");
                }
                return sb.toString();
            } catch(NullPointerException x) {
                x.printStackTrace();
                Debug.log("File not found: " + file, Debug.MessageType.ERROR);
            } catch(IOException x) {
                Debug.log("Unable to access file: " + file, Debug.MessageType.ERROR);
            }
            return "";
        }

        private static class Library {
            String path;
            TokenPattern<?> pattern;
            ParsingSignature signature;
            TridentSummaryModule fileSummary;
            boolean summaryOnly;

            public Library(String path, String content) {
                this(path, content, false);
            }

            public Library(String path, String content, boolean summaryOnly) {
                this.path = path;

                Path relPath = Paths.get(path);

                fileSummary = new TridentSummaryModule(null);
                fileSummary.setFileLocation(new TridentUtil.ResourceLocation(relPath.getName(0) + ":" + relPath.subpath(2, relPath.getNameCount()).toString().replace(File.separator, "/").replaceAll(".tdn$","")));

                TokenStream ts = new TokenStream();
                LazyLexer lex = new LazyLexer(ts, new TridentProductions(dummyModule).FILE);
                lex.setSummaryModule(fileSummary);
                lex.tokenizeParse(new File(System.getProperty("user.home")), content, new TridentLexerProfile());

                if(!lex.getMatchResponse().matched) {
                    throw new RuntimeException("Native lib threw an error on parse: " + lex.getMatchResponse().getErrorMessage());
                }

                this.pattern = lex.getMatchResponse().pattern;

                this.signature = new ParsingSignature(content.hashCode(), pattern, null);

                this.summaryOnly = summaryOnly;
            }
        }
    }
    private static class Dependency {


        private enum Mode {
            PRECOMPILE, COMBINE;
        }

        TridentCompiler compiler;
        boolean doExport = false;
        Mode mode = Mode.PRECOMPILE;

        public Dependency(TridentCompiler compiler) {
            this.compiler = compiler;
        }
    }
}
