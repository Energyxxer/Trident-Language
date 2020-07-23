package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.module.*;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeNotFoundException;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.ParsingSignature;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.nbtmapper.NBTTypeMapPack;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.symbols.GlobalSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.util.JsonTraverser;
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

import static com.energyxxer.trident.extensions.EJsonElement.getAsBooleanOrNull;
import static com.energyxxer.trident.extensions.EJsonElement.getAsStringOrNull;

public class TridentCompiler extends AbstractProcess {

    public static final String PROJECT_FILE_NAME = ".tdnproj";
    public static final String PROJECT_BUILD_FILE_NAME = ".tdnbuild";
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String TRIDENT_LANGUAGE_VERSION = "1.2.0";

    //Resources
    private TridentProjectWorker worker;
    private TridentBuildConfiguration buildConfig;
    private NBTTypeMap typeMap;
    private boolean workerHasWorked = false;

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
    private String anonymousFunctionTemplate = "_anonymous*";

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
    private HashSet<String> dataSubFolderNames = new HashSet<>();
    private HashSet<String> resourceSubFolderNames = new HashSet<>();

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

        worker = new TridentProjectWorker(rootDir);
        workerHasWorked = false;
    }

    TridentCompiler(TridentProjectWorker worker) {
        super("Trident-Compiler[" + worker.rootDir.getName() + "]");
        this.rootDir = worker.rootDir;
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

        this.worker = worker;
        workerHasWorked = true;
    }

    public TridentProjectWorker getWorker() {
        return worker;
    }

    public void setBuildConfig(TridentBuildConfiguration buildConfig) {
        this.buildConfig = buildConfig;
    }

    public TridentBuildConfiguration getBuildConfig() {
        return buildConfig;
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

        invokeStart();

        this.setProgress("Initializing analyzers");

        AnalyzerManager.initialize();
        TridentTypeManager.initialize();

        this.setProgress("Reading project settings file");

        worker.setup.setupProperties = true;
        worker.setup.setupModule = true;
        worker.setup.setupProductions = true;
        worker.setup.useReport = true;
        worker.setBuildConfig(buildConfig);

        try {
            if(!workerHasWorked) worker.work();
            workerHasWorked = true;
        } catch(IOException x) {
            logException(x, "Error while importing vanilla definitions: ");
            return;
        }

        properties = worker.output.properties;

        languageLevel = JsonTraverser.INSTANCE.reset(properties).get("language-level").asInt(1);
        if(languageLevel < 1 || languageLevel > 3) {
            report.addNotice(new Notice(NoticeType.ERROR, "Invalid language level: " + languageLevel));
            languageLevel = 1;
        }

        if(properties.has("default-namespace") && properties.get("default-namespace").isJsonPrimitive() && properties.get("default-namespace").getAsJsonPrimitive().isString() && !properties.get("default-namespace").getAsString().isEmpty()) {
            defaultNamespace = properties.get("default-namespace").getAsString().trim();
        }

        if(buildConfig.resourcePackOutput != null) {
            resourcePack = new ResourcePackGenerator(this, buildConfig.resourcePackOutput);
        }

        if(properties.has("anonymous-function-name") && properties.get("anonymous-function-name").isJsonPrimitive() && properties.get("anonymous-function-name").getAsJsonPrimitive().isString()) {
            anonymousFunctionTemplate = properties.get("anonymous-function-name").getAsString();
        }


        this.setProgress("Importing vanilla definitions");

        module = worker.output.module;

        report.addNotices(worker.report.getAllNotices());
        if(report.hasErrors()) {
            finalizeProcess(false);
            return;
        }

        typeMap = new NBTTypeMap(module);

        if(buildConfig.typeMapPacks != null) {
            for(NBTTypeMapPack pack : buildConfig.typeMapPacks) {
                typeMap.parsing.parsePack(rootDir, pack);
            }
        } else {
            typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("common.nbttm"));
            typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("entities.nbttm"));
            typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("block_entities.nbttm"));
        }
        typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("trident.nbttm"));

        this.setProgress("Adding native methods");

        {
            for(DefaultLibraryProvider lib : AnalyzerManager.getAllParsers(DefaultLibraryProvider.class)) {
                lib.populate(global, this);
            }
        }

        this.setProgress("Parsing files");
        TokenStream ts = new TokenStream();
        LazyLexer lex = new LazyLexer(ts, worker.output.productions.FILE);
        recursivelyParse(lex, rootDir);

        report.addNotices(lex.getNotices());
        report.addNotices(typeMap.getNotices());
        if(report.hasErrors()) {
            finalizeProcess(false);
            return;
        }

        this.setProgress("Resolving dependencies");

        for(TridentProjectWorker dependencyWorker : worker.output.dependencies) {
            TridentCompiler subCompiler = dependencyWorker.createCompiler();
            subCompiler.setParentCompiler(this);
            subCompiler.addProgressListener((process) -> this.updateStatus(process.getStatus()));
            try {
                subCompiler.runCompilation();
            } catch(Exception ex) {
                logException(ex);
                return;
            }
            report.addNotices(subCompiler.getReport().getAllNotices());
            if(!subCompiler.isSuccessful()) {
                finalizeProcess(false);
            } else {
                files.putAll(subCompiler.files);
                if(!dependencyWorker.getDependencyInfo().doExport) {
                    subCompiler.module.propagateExport(false);
                    for(TridentFile file : subCompiler.files.values()) {
                        file.setShouldExportFunction(false);
                    }
                }
                module.join(subCompiler.module);
                global.join(subCompiler.global);

                subCompiler.setRerouteRoot(true);
            }
        }

        VersionFeatureManager.setActiveFeatureMap(buildConfig.featureMap);
        module.setSettingsActive();

        Resources.populate(ownFiles, filePatterns);

        for(String key : ownFiles) {
            ParsingSignature value = filePatterns.get(key);
            Path relativePath = Paths.get(key);
            if(relativePath.getNameCount() >= 2 && "functions".equals(relativePath.getName(1).toString())) {
                try {
                    files.put(key, new TridentFile(this, relativePath, value.getPattern()));
                } catch(CommodoreException ex) {
                    report.addNotice(new Notice(NoticeType.ERROR, ex.toString(), value.getPattern()));
                    break;
                }
            } else {
                report.addNotice(new Notice(NoticeType.WARNING, "Trident file found outside a functions directory. This file will be ignored.", value.getPattern()));
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
        if(buildConfig.dataPackOutput != null) {
            try {
                File dataOut = buildConfig.dataPackOutput;
                this.dataSubFolderNames.add("data");
                for(Exportable exportable : module.exportables) {
                    if(exportable.shouldExport() && exportable.getExportPath() != null) {
                        String firstName = exportable.getExportPath();
                        if(firstName.contains("/")) firstName = firstName.substring(0, firstName.indexOf("/"));
                        dataSubFolderNames.add(firstName);
                    }
                }
                if(buildConfig.cleanDataPackOutput) {
                    for(String subFolder : this.dataSubFolderNames) {
                        Debug.log("Clearing folder: " + subFolder);
                        recursivelyDelete(dataOut.toPath().resolve(subFolder).toFile());
                    }
                }
                module.compile(dataOut);
            } catch(IOException x) {
                logException(x, "Error while generating output data pack: ");
            }
        } else {
            this.report.addNotice(new Notice(NoticeType.ERROR, "Datapack output directory not specified"));
        }

        updateProgress(0);
        this.setProgress("Generating resource pack");
        try {
            if(resourcePack != null) {
                if(buildConfig.cleanResourcePackOutput) {
                    File resourceOut = buildConfig.resourcePackOutput;
                    this.resourceSubFolderNames.add("assets");
                    for(Exportable exportable : resourcePack.exportables) {
                        if(exportable.shouldExport() && exportable.getExportPath() != null) {
                            String firstName = exportable.getExportPath();
                            if(firstName.contains("/")) firstName = firstName.substring(0, firstName.indexOf("/"));
                            resourceSubFolderNames.add(firstName);
                        }
                    }
                    for(String subFolder : this.resourceSubFolderNames) {
                        Debug.log("Clearing folder: " + subFolder);
                        recursivelyDelete(resourceOut.toPath().resolve(subFolder).toFile());
                    }
                }
                resourcePack.generate();
            }
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
                                || buildConfig.cleanResourcePackOutput
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

        if(relPath.getNameCount() <= 1) {
            report.addNotice(new Notice(NoticeType.WARNING, "Tag is not in a type category folder", new Token("", file, new StringLocation(0))));
            return;
        }

        String tagDir = relPath.getName(0).toString();
        relPath = relPath.subpath(1, relPath.getNameCount());

        String tagName = relPath.toString().replace(File.separator, "/");
        tagName = tagName.substring(0, tagName.length() - ".json".length());

        for(TagGroup<?> group : namespace.tags.getGroups()) {
            if(group.getDirectoryName().equals(tagDir)) {
                Tag tag = group.getOrCreate(tagName);
                tag.setExport(true);
                Debug.log("Created tag " + tag);

                JsonObject obj;
                try(FileReader fr = new FileReader(file)) {
                    obj = gson.fromJson(fr, JsonObject.class);
                } catch(JsonSyntaxException x) {
                    report.addNotice(new Notice(NoticeType.ERROR, "Invalid JSON in " + group.getCategory().toLowerCase() + " tag '" + tag + "': " + x.getMessage(), new Token("", file, new StringLocation(0))));
                    continue;
                }

                tag.setOverridePolicy(Tag.OverridePolicy.valueOf(JsonTraverser.INSTANCE.reset(obj).get("replace").asBoolean(Tag.OverridePolicy.DEFAULT_POLICY.valueBool)));
                tag.setExport(true);

                JsonArray values = JsonTraverser.INSTANCE.reset(obj).get("values").asJsonArray();

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
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(rawId);

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
                                    report.addNotice(new Notice(NoticeType.WARNING, "Invalid value in " + group.getCategory().toLowerCase() + " tag '" + tag + "': " + loc + " is not a valid " + group.getCategory().toLowerCase() + " type", new Token("", file, new StringLocation(0))));
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

    public void setDependencyMode(Dependency.Mode dependencyMode) {
        this.dependencyMode = dependencyMode;
    }

    public Dependency.Mode getDependencyMode() {
        return dependencyMode;
    }

    public String createAnonymousFunctionName(int index) {
        return anonymousFunctionTemplate.replace("*",String.valueOf(index));
    }

    public static void summarizeLibraries(TridentProjectSummary summary) {
        Resources.summarize(summary);
    }



    static boolean recursivelyDelete(File folder) {
        if(!folder.exists()) return true;
        if(!Files.isWritable(folder.toPath())) return false;
        if(!folder.isDirectory()) {
            return folder.delete();
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    recursivelyDelete(f);
                } else {
                    f.delete();
                }
            }
        }
        return folder.delete();
    }
    public static final File LIBRARY_TOKEN_FILE = new File(System.getProperty("user.home"));

    private static class Resources {
        public static final HashMap<String, String> defaults = new HashMap<>();
        public static final ArrayList<Library> libraries = new ArrayList<>();
        public static final CommandModule dummyModule = new CommandModule("Trident Dummy");

        static {
            defaults.put("common.nbttm", read("/typemaps/common.nbttm"));
            defaults.put("entities.nbttm", read("/typemaps/entities.nbttm"));
            defaults.put("block_entities.nbttm", read("/typemaps/block_entities.nbttm"));
            defaults.put("trident.nbttm", read("/typemaps/trident.nbttm"));

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

            libraries.add(new Library("trident-util/functions/native.tdn", read("/trident_utils/datapack/data/trident-util/functions/native_compiler.tdn"), Library.Availability.COMPILER_ONLY));
            libraries.add(new Library("trident-util/functions/native.tdn", read("/trident_utils/datapack/data/trident-util/functions/native_summary.tdn"), Library.Availability.SUMMARY_ONLY));
            libraries.add(new Library("trident-util/functions/type_checking.tdn", read("/trident_utils/datapack/data/trident-util/functions/type_checking.tdn")));
            libraries.add(new Library("trident-util/functions/generators.tdn", read("/trident_utils/datapack/data/trident-util/functions/generators.tdn")));
            libraries.add(new Library("trident-util/functions/shared.tdn", read("/trident_utils/datapack/data/trident-util/functions/shared.tdn")));
            libraries.add(new Library("trident-util/functions/predicate.tdn", read("/trident_utils/datapack/data/trident-util/functions/predicate.tdn")));
            libraries.add(new Library("trident-util/functions/advancement_triggers.tdn", read("/trident_utils/datapack/data/trident-util/functions/advancement_triggers.tdn")));
        }

        static void populate(ArrayList<String> ownFiles, HashMap<String, ParsingSignature> filePatterns) {
            for(Library lib : libraries) {
                if(!lib.availability.compiler) continue;
                ownFiles.add(lib.path);
                filePatterns.put(lib.path, lib.signature);
            }
        }

        static void summarize(TridentProjectSummary summary) {
            for(Library lib : libraries) {
                if(!lib.availability.summary) continue;
                summary.store(null, lib.fileSummary);
                lib.fileSummary.setParentSummary(summary);
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

            public enum Availability {
                COMPILER_ONLY(true, false), SUMMARY_ONLY(false, true), BOTH(true, true);
                private final boolean compiler;
                private final boolean summary;

                Availability(boolean compiler, boolean summary) {
                    this.compiler = compiler;
                    this.summary = summary;
                }
            }

            String path;
            TokenPattern<?> pattern;
            ParsingSignature signature;
            TridentSummaryModule fileSummary;
            Availability availability;

            public Library(String path, String content) {
                this(path, content, Availability.BOTH);
            }

            public Library(String path, String content, Availability availability) {
                this.path = path;

                Path relPath = Paths.get(path);

                fileSummary = new TridentSummaryModule(null);
                fileSummary.setFileLocation(new TridentUtil.ResourceLocation(relPath.getName(0) + ":" + relPath.subpath(2, relPath.getNameCount()).toString().replace(File.separator, "/").replaceAll(".tdn$","")));

                TokenStream ts = new TokenStream();
                LazyLexer lex = new LazyLexer(ts, new TridentProductions(dummyModule).FILE);
                lex.setSummaryModule(fileSummary);
                lex.tokenizeParse(LIBRARY_TOKEN_FILE, content, new TridentLexerProfile());

                if(!lex.getMatchResponse().matched) {
                    throw new RuntimeException("Native lib threw an error on parse: " + lex.getMatchResponse().getErrorMessage());
                }

                this.pattern = lex.getMatchResponse().pattern;

                this.signature = new ParsingSignature(content.hashCode(), pattern, null);

                this.availability = availability;
            }
        }
    }
    static class Dependency {

        enum Mode {
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
