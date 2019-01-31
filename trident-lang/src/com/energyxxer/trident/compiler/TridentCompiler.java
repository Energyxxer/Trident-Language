package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.functionlogic.functions.Function;
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
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.ParsingSignature;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.instructions.AliasInstruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.interfaces.ProgressListener;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.util.logger.Debug;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.energyxxer.trident.extensions.EJsonElement.getAsStringOrNull;
import static com.energyxxer.trident.extensions.EJsonObject.getAsBoolean;

public class TridentCompiler {

    public static final String PROJECT_FILE_NAME = ".tdnproj";
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private final DefinitionPack definitionPack = StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT;

    private final File rootDir;
    private CommandModule module;
    private ResourcePackGenerator resourcePack;

    private JsonObject properties = null;

    private ArrayList<ProgressListener> progressListeners = new ArrayList<>();
    private ArrayList<Runnable> completionListeners = new ArrayList<>();
    private CompilerReport report = null;

    private Thread thread;

    private HashMap<File, ParsingSignature> filePatterns = new HashMap<>();
    private HashMap<File, TridentFile> files = new HashMap<>();

    private Gson gson;

    private SymbolStack symbolStack = new SymbolStack();
    private CallStack callStack = new CallStack();
    private TryStack tryStack = new TryStack();
    private SpecialFileManager specialFileManager;
    private int languageLevel = 1;
    private String defaultNamespace = null;

    private NBTTypeMap typeMap;

    private HashMap<Integer, Integer> inResourceCache = new HashMap<>();
    private HashMap<Integer, Integer> outResourceCache = new HashMap<>();

    public TridentCompiler(File rootDir) {
        this.rootDir = rootDir;

        specialFileManager = new SpecialFileManager(this);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        this.gson = gsonBuilder.create();
    }

    public void compile() {
        this.thread = new Thread(this::runCompilation,"Trident-Compiler[" + rootDir.getName() + "]");
        report = new CompilerReport();
        thread.start();
    }

    public static CommandModule createModuleForProject(String name, File rootDir, DefinitionPack definitionPack) throws IOException {
        JsonObject properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
        return createModuleForProject(name, properties, definitionPack);
    }

    public static CommandModule createModuleForProject(String name, JsonObject properties, DefinitionPack definitionPack) throws IOException {
        CommandModule module = new CommandModule(name, "Command Module created with Trident", null);
        module.getOptionManager().EXPORT_EMPTY_FUNCTIONS.setValue(true);
        module.importDefinitions(definitionPack);

        if(properties.has("aliases") && properties.get("aliases").isJsonObject()) {
            for(Map.Entry<String, JsonElement> categoryEntry : properties.getAsJsonObject("aliases").entrySet()) {
                String category = categoryEntry.getKey();

                if(categoryEntry.getValue().isJsonObject()) {
                    for(Map.Entry<String, JsonElement> entry : categoryEntry.getValue().getAsJsonObject().entrySet()) {
                        TridentUtil.ResourceLocation alias = TridentUtil.ResourceLocation.createStrict(entry.getKey());
                        TridentUtil.ResourceLocation real = TridentUtil.ResourceLocation.createStrict(entry.getValue().getAsString());
                        if(alias == null) continue;
                        if(real == null) continue;

                        module.createNamespace(alias.namespace).types.getDictionary(category).create((c, ns, n) -> new AliasInstruction.AliasType(c, ns, n, module.createNamespace(real.namespace), real.body), alias.body);
                        //Debug.log("Created alias '" + alias + "' for '" + real + "'");
                    }
                }
            }
        }
        return module;
    }

    public float progress = -1;

    private void runCompilation() {

        this.setProgress("Initializing analyzers");

        AnalyzerManager.initialize();

        this.setProgress("Reading project settings file");
        try {
            properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
        } catch(JsonSyntaxException | IOException x) {
            logException(x);
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
            resourcePack = new ResourcePackGenerator(this, new File(properties.get("resources-output").getAsString()));
        }

        this.setProgress("Importing vanilla definitions");
        try {
            module = createModuleForProject(rootDir.getName(), properties, definitionPack);
        } catch(IOException x) {
            logException(x);
            return;
        }

        typeMap = new NBTTypeMap(module);

        typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("common.nbttm"));
        typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("entities.nbttm"));
        typeMap.parsing.parseNBTTMFile(rootDir, Resources.defaults.get("block_entities.nbttm"));

        this.setProgress("Adding native methods");

        {
            symbolStack.getGlobal().put(new Symbol("new", Symbol.SymbolVisibility.GLOBAL, new DictionaryObject()));
            for(DefaultLibraryProvider lib : AnalyzerManager.getAllParsers(DefaultLibraryProvider.class)) {
                lib.populate(symbolStack, this);
            }
        }

        this.setProgress("Parsing files");
        TokenStream ts = new TokenStream();
        LazyLexer lex = new LazyLexer(ts, new TridentProductions(module).FILE);
        recursivelyParse(lex, rootDir);

        report.addNotices(lex.getNotices());
        report.addNotices(typeMap.getNotices());
        if(report.getTotal() > 0) {
            finalizeCompilation();
            return;
        }

        Path dataRoot = new File(rootDir, "datapack" + File.separator + "data").toPath();

        for(Map.Entry<File, ParsingSignature> entry : filePatterns.entrySet()) {
            Path relativePath = dataRoot.relativize(entry.getKey().toPath());
            if("functions".equals(relativePath.getName(1).toString())) {
                files.put(entry.getKey(), new TridentFile(this, relativePath, entry.getValue().getPattern()));
            }
            else {
                report.addNotice(new Notice(NoticeType.WARNING, "Found .tdn file outside of a function folder, ignoring: " + relativePath, entry.getValue().getPattern()));
            }
        }

        files.values().forEach(TridentFile::checkCircularRequires);

        if(report.hasErrors()) {
            finalizeCompilation();
            return;
        }

        ArrayList<TridentFile> sortedFiles = new ArrayList<>(files.values());

        files.values().forEach(this::getAllRequires);

        sortedFiles.sort((a,b) -> (a.isCompileOnly() != b.isCompileOnly()) ? a.isCompileOnly() ? -2 : 2 : (int) Math.signum((b.getPriority() - a.getPriority()) * 1000));

        progress = 0;
        float delta = 1f / sortedFiles.size();
        for(TridentFile file : sortedFiles) {
            this.setProgress("Analyzing " + file.getResourceLocation());
            try {
                file.resolveEntries();
            } catch(ReturnException r) {
                report.addNotice(new Notice(NoticeType.ERROR, "Return instruction outside inner function", r.getPattern()));
            } catch(BreakException b) {
                report.addNotice(new Notice(NoticeType.ERROR, "Break instruction outside loop", b.getPattern()));
            } catch(ContinueException c) {
                report.addNotice(new Notice(NoticeType.ERROR, "Continue instruction outside loop", c.getPattern()));
            }
            progress += delta;
        }

        if(report.hasErrors()) {
            finalizeCompilation();
            return;
        }

        specialFileManager.compile();

        if(report.hasErrors()) {
            finalizeCompilation();
            return;
        }

        Function initFunction = module.minecraft.functions.get("trident_start");

        Tag loadTag = module.minecraft.tags.functionTags.create("load");
        loadTag.addValue(new FunctionReference(initFunction));
        module.getObjectiveManager().setCreationFunction(initFunction);

        progress = -1;
        this.setProgress("Generating data pack");
        {
            try {
                module.compile(new File(properties.get("datapack-output").getAsString()));
            } catch(IOException x) {
                logException(x);
                finalizeCompilation();
            }
        }

        progress = 0;
        this.setProgress("Generating resource pack");
        {
            try {
                resourcePack.generate();
            } catch(IOException x) {
                logException(x);
                finalizeCompilation();
            }
        }

        finalizeCompilation();
    }

    private Collection<TridentUtil.ResourceLocation> getAllRequires(TridentFile file) {
        if(file.getCascadingRequires() == null) {
            file.addCascadingRequires(Collections.emptyList());
            file.getRequires().forEach(fl -> file.addCascadingRequires(getAllRequires(getFile(fl))));
        }
        return file.getCascadingRequires();
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

                            if(!filePatterns.containsKey(file) || filePatterns.get(file).getHashCode() != hashCode) {
                                lex.tokenizeParse(file, str, new TridentLexerProfile(module));

                                if (lex.getMatchResponse().matched) {
                                    filePatterns.put(file, new ParsingSignature(hashCode, lex.getMatchResponse().pattern));
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
                            }
                        } catch (IOException x) {
                            logException(x);
                        }
                    }
                } else if(file.toPath().startsWith(rootDir.toPath().resolve("resources"))) {
                    if(resourcePack == null) break;
                    this.setProgress("Parsing file: " + rootDir.toPath().relativize(file.toPath()));

                    try {
                        Path relPath = rootDir.toPath().resolve("resources").relativize(file.toPath());
                        byte[] data = Files.readAllBytes(file.toPath());
                        int hashCode = Arrays.hashCode(data);

                        outResourceCache.put(relPath.hashCode(), hashCode);

                        if(resourcePack.getOutputType() == ModulePackGenerator.OutputType.ZIP
                                || !Objects.equals(inResourceCache.get(relPath.hashCode()), hashCode)) {
                            resourcePack.exportables.add(new RawExportable(relPath.toString().replace(File.separator, "/"), data));
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
        Namespace namespace = module.createNamespace(namespaceName);

        Path relPath = dataPath.relativize(file.toPath());
        relPath = relPath.subpath(2, relPath.getNameCount());
        String tagDir = relPath.getName(0).toString();
        relPath = relPath.subpath(1, relPath.getNameCount());

        String tagName = relPath.toString().replace(File.separator, "/");
        tagName = tagName.substring(0, tagName.length() - ".json".length());

        for(TagGroup<?> group : namespace.tags.getGroups()) {
            if(group.getDirectoryName().equals(tagDir)) {
                Tag tag = group.create(tagName);
                Debug.log("Created tag " + tag);
                tag.setExport(true);

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
                        Tag created = module.createNamespace(loc.namespace).getTagManager().getGroup(group.getCategory()).create(loc.body);
                        tag.addValue(created);
                    } else {
                        Type created;
                        if(group.getCategory().equals(FunctionReference.CATEGORY)) {
                            created = new FunctionReference(module.createNamespace(loc.namespace), loc.body);
                        } else {
                            created = module.createNamespace(loc.namespace).getTypeManager().createDictionary(group.getCategory(), true).create(loc.body);
                        }
                        tag.addValue(created);
                    }
                }

                break;
            }
        }
    }

    private void logException(Throwable x) {
        this.report.addNotice(new Notice(NoticeType.ERROR, x.getMessage()));
        finalizeCompilation();
    }

    private void finalizeCompilation() {
        this.setProgress("Compilation " + (report.getErrors().isEmpty() ? "completed" : "interrupted") + " with " + report.getTotalsString(), false);
        completionListeners.forEach(Runnable::run);
        progressListeners.clear();
        completionListeners.clear();
    }

    public void addProgressListener(ProgressListener l) {
        progressListeners.add(l);
    }

    private void removeProgressListener(ProgressListener l) {
        progressListeners.remove(l);
    }

    public void setProgress(String message) {
        setProgress(message, true);
    }

    private void setProgress(String message, boolean includeProjectName) {
        progressListeners.forEach(l -> l.onProgress(message + (includeProjectName ? ("... [" + rootDir.getName() + "]") : ""), progress));
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

    public void addCompletionListener(Runnable r) {
        completionListeners.add(r);
    }

    public void removeCompletionListener(Runnable r) {
        completionListeners.remove(r);
    }

    public JsonObject getProperties() {
        return properties;
    }

    public TridentFile getFile(TridentUtil.ResourceLocation loc) {
        for(TridentFile file : files.values()) {
            if(file.getResourceLocation().equals(loc)) return file;
        }
        return null;
    }

    public SymbolStack getSymbolStack() {
        return symbolStack;
    }

    public CallStack getCallStack() {
        return callStack;
    }

    public int getLanguageLevel() {
        return languageLevel;
    }

    private boolean givenDefaultNamespaceNotice = false;

    public String getDefaultNamespace() {
        if(defaultNamespace == null && !givenDefaultNamespaceNotice) {
            report.addNotice(new Notice(NoticeType.WARNING, "Some language features used require a default namespace. Please specify a default namespace in the project settings"));
            defaultNamespace = "trident_temp_please_specify_default_namespace";
            givenDefaultNamespaceNotice = true;
        }
        return defaultNamespace;
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

    public void setInResourceCache(HashMap<Integer, Integer> inResourceCache) {
        if(inResourceCache != null)
        this.inResourceCache = inResourceCache;
    }

    public HashMap<Integer, Integer> getOutResourceCache() {
        return outResourceCache;
    }

    public void setResourceCache(HashMap<File, ParsingSignature> cache) {
        if(cache != null)
        this.filePatterns = cache;
    }

    public HashMap<File, ParsingSignature> getSourceCache() {
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

    private static class Resources {
        public static final HashMap<String, String> defaults = new HashMap<>();

        static {
            defaults.put("common.nbttm", read("/typemaps/common.nbttm"));
            defaults.put("entities.nbttm", read("/typemaps/entities.nbttm"));
            defaults.put("block_entities.nbttm", read("/typemaps/block_entities.nbttm"));
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
    }
}
