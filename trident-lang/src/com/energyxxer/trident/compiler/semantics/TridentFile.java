package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionComment;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.*;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.commands.CommandParser;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.instructions.Instruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.plugin.CommandDefinition;
import com.energyxxer.trident.compiler.plugin.PluginCommandParser;
import com.energyxxer.trident.compiler.plugin.TridentPlugin;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.ImportedSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class TridentFile extends SymbolContext {
    private String namespace;
    private TokenPattern<?> pattern;
    private final HashMap<TokenPattern<?>, TridentUtil.ResourceLocation> requires = new HashMap<>();
    private ArrayList<TridentUtil.ResourceLocation> cascadingRequires = null;
    private final ArrayList<TridentUtil.ResourceLocation> tags = new ArrayList<>();
    private final ArrayList<TridentUtil.ResourceLocation> metaTags = new ArrayList<>();
    private final Path relSourcePath;

    private Function function;
    private final ArrayList<ExecuteModifier> writingModifiers = new ArrayList<>();
    private final TridentUtil.ResourceLocation location;

    private boolean compileOnly = false;
    private boolean breaking = false;
    private float priority = 0;
    private boolean valid = true;

    private int languageLevel;

    private int anonymousChildren = 0;

    private DictionaryObject metadata;

    private boolean shouldExportFunction = true;

    private ArrayList<Runnable> postProcessingActions = new ArrayList<>();

    public TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this(compiler, null, relSourcePath, filePattern, compiler.getLanguageLevel());
    }

    private TridentFile(TridentCompiler compiler, ISymbolContext parentContext, Path relSourcePath, TokenPattern<?> filePattern, int languageLevel) {
        super(compiler);
        this.parentScope = parentContext;
        this.namespace = relSourcePath.getName(0).toString();
        this.pattern = filePattern;
        this.relSourcePath = relSourcePath;

        this.languageLevel = languageLevel;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Pattern.quote(File.separator), "/");
        this.location = new TridentUtil.ResourceLocation(this.namespace + ":" + functionPath);

        resolveDirectives();
        if(!compileOnly && getNamespace().functions.exists(functionPath)) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A function by the name '" + namespace + ":" + functionPath + "' already exists", pattern));
            valid = false;
        }

        this.function = compileOnly ? null : getNamespace().functions.getOrCreate(functionPath);
        setShouldExportFunction(parentContext == null || parentContext.getStaticParentFile().shouldExportFunction);
    }

    private void resolveDirectives() {
        TokenPattern<?> directiveList = pattern.find("..DIRECTIVES");
        if(directiveList != null) {
            TokenPattern<?>[] directives = ((TokenList) directiveList).getContents();
            for(TokenPattern<?> rawDirective : directives) {
                TokenGroup directiveBody = (TokenGroup) (((TokenStructure) ((TokenGroup) rawDirective).getContents()[1]).getContents());

                switch(directiveBody.getName()) {
                    case "ON_DIRECTIVE": {
                        String on = ((TokenItem) (directiveBody.getContents()[1])).getContents().value;
                        if(on.equals("compile")) {
                            if(!tags.isEmpty()) {
                                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have any tags", directiveList));
                            }
                            compileOnly = true;
                        }
                        break;
                    }
                    case "TAG_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents().value);
                        if(compileOnly) {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have any tags", directiveList));
                        }
                        tags.add(loc);
                        break;
                    }
                    case "META_TAG_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents().value);
                        metaTags.add(loc);
                        break;
                    }
                    case "REQUIRE_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents().value);
                        requires.put(directiveBody, loc);
                        break;
                    }
                    case "LANGUAGE_LEVEL_DIRECTIVE": {
                        int level = CommonParsers.parseInt(directiveBody.find("INTEGER"), this);
                        if(level < 1 || level > 3) {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Invalid language level: " + level, directiveBody.find("INTEGER")));
                        } else this.languageLevel = level;
                        break;
                    }
                    case "PRIORITY_DIRECTIVE": {
                        this.priority = (float) CommonParsers.parseDouble(directiveBody.find("REAL"), this);
                        break;
                    }
                    case "BREAKING_DIRECTIVE": {
                        this.breaking = true;
                        break;
                    }
                    case "METADATA_DIRECTIVE": {
                        if (this.metadata == null) {
                            this.metadata = InterpolationManager.parse(directiveBody.find("DICTIONARY"), this, DictionaryObject.class);
                        } else {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Duplicate metadata directive", directiveList));
                        }
                        break;
                    }
                    default: {
                        getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown directive type '" + directiveBody.getName() + "'", directiveBody));
                    }
                }
            }
        }
        if(metadata == null) {
            metadata = new DictionaryObject();
        }
    }

    public static Function createAnonymousSubFunction(ISymbolContext parent) {
        String innerFilePathRaw = parent.getStaticParentFile().getPath().toString();
        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()-".tdn".length());

        Path relSourcePath = Paths.get(innerFilePathRaw).resolve(parent.getCompiler().createAnonymousFunctionName(parent.getStaticParentFile().anonymousChildren) + ".tdn");
        parent.getStaticParentFile().anonymousChildren++;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Pattern.quote(File.separator), "/");

        return parent.getCompiler().getModule().getNamespace(relSourcePath.getName(0).toString()).functions.create(functionPath);
    }

    //Sub context automatically created
    public static TridentFile createInnerFile(TokenPattern<?> pattern, ISymbolContext parent) {
        return createInnerFile(pattern, parent, null);
    }

    //Sub context automatically created
    public static TridentFile createInnerFile(TokenPattern<?> pattern, ISymbolContext parent, String subPath) {
        return createInnerFile(pattern, parent, subPath, true);
    }

    //Sub context automatically created
    public static TridentFile createInnerFile(TokenPattern<?> pattern, ISymbolContext parent, String subPath, boolean autoResolve) {
        TokenPattern<?> namePattern = pattern.find("INNER_FUNCTION_NAME.RESOURCE_LOCATION");
        String innerFilePathRaw = parent.getStaticParentFile().getPath().toString();
        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()-".tdn".length());
        if(subPath != null) {
            innerFilePathRaw = Paths.get(innerFilePathRaw).resolve(subPath).toString();
        }

        if(namePattern != null) {
            TridentUtil.ResourceLocation suggestedLoc = CommonParsers.parseResourceLocation(namePattern, parent);
            suggestedLoc.assertStandalone(namePattern, parent);
            if(!suggestedLoc.namespace.equals("minecraft")) {
                innerFilePathRaw = suggestedLoc.namespace + File.separator + "functions" + File.separator + suggestedLoc.body + ".tdn";
            } else {
                String functionName = suggestedLoc.body;
                while(functionName.startsWith("/")) {
                    functionName = functionName.substring(1);
                }
                innerFilePathRaw = Paths.get(innerFilePathRaw).resolve(functionName + ".tdn").toString();
            }
        } else {
            innerFilePathRaw = Paths.get(innerFilePathRaw).resolve(parent.getCompiler().createAnonymousFunctionName(parent.getStaticParentFile().anonymousChildren) + ".tdn").toString();
            parent.getStaticParentFile().anonymousChildren++;
        }

        TokenPattern<?> innerFilePattern = pattern.find("FILE_INNER");

        TridentFile innerFile = new TridentFile(parent.getCompiler(), parent, Paths.get(innerFilePathRaw), innerFilePattern, parent.getStaticParentFile().languageLevel);
        if(autoResolve) innerFile.resolveEntries();
        return innerFile;
    }

    //Sub context NOT automatically created
    public static void resolveInnerFileIntoSection(TokenPattern<?> pattern, ISymbolContext parentCtx, FunctionSection function) {
        if(pattern.find("FILE_INNER..DIRECTIVES") != null) {
            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Directives aren't allowed in this context", pattern.find("FILE_INNER..DIRECTIVES"), parentCtx);
        }

        resolveEntries((TokenList) pattern.find("FILE_INNER.ENTRIES"), parentCtx, function, false);
    }

    //Sub context NOT automatically created
    public static void resolveFileIntoSection(TokenPattern<?> pattern, ISymbolContext parentCtx, FunctionSection function) {
        if(pattern.find("DIRECTIVES") != null) {
            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Directives aren't allowed in this context", pattern.find("DIRECTIVES"), parentCtx);
        }

        resolveEntries((TokenList) pattern.find("ENTRIES"), parentCtx, function, false);
    }

    //Sub context NOT automatically created
    public static void resolveEntryListIntoSection(TokenList list, ISymbolContext parentCtx, FunctionSection function) {
        resolveEntries(list, parentCtx, function, false);
    }

    public void addCascadingRequires(Collection<TridentUtil.ResourceLocation> locations) { //TODO clean up; this lazy initialization is misleading
        if(cascadingRequires == null) cascadingRequires = new ArrayList<>(requires.values());
        cascadingRequires.addAll(locations);
    }

    private boolean reportedNoCommands = false;

    public void resolveEntries() {
        if(!valid) return;
        if(entriesResolved) return;

        if(function != null) tags.forEach(l -> {
            Tag tag = getCompiler().getModule().getNamespace(l.namespace).tags.functionTags.getOrCreate(l.body);
            tag.setExport(true);
            tag.addValue(new FunctionReference(this.function));
        });

        resolveEntries((TokenList) this.pattern.find(".ENTRIES"), this, function, compileOnly);
    }

    public Function getFunction() {
        return function;
    }

    public ArrayList<ExecuteModifier> getWritingModifiers() {
        return writingModifiers;
    }

    public Collection<TridentUtil.ResourceLocation> getRequires() {
        return requires.values();
    }

    public Collection<TridentUtil.ResourceLocation> getCascadingRequires() {
        return cascadingRequires;
    }

    public void checkCircularRequires() {
        this.checkCircularRequires(new ArrayList<>());
    }

    private void checkCircularRequires(ArrayList<TridentUtil.ResourceLocation> previous) {
        previous.add(this.location);
        for(Map.Entry<TokenPattern<?>, TridentUtil.ResourceLocation> entry : requires.entrySet()) {
            if(previous.contains(entry.getValue())) {
                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Circular requirement with function '" + entry.getValue() + "'", entry.getKey()));
                Debug.log("Previous (for file '" + this.getResourceLocation() + "'): " + previous);
            } else {
                TridentFile next = getCompiler().getFile(entry.getValue());
                if(next != null) {
                    next.checkCircularRequires(previous);
                } else {
                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required Trident function '" + entry.getValue() + "' does not exist", entry.getKey()));
                }
            }
        }
        previous.remove(this.location);
    }

    public TridentUtil.ResourceLocation getResourceLocation() {
        return location;
    }

    public Namespace getNamespace() {
        return getCompiler().getModule().getNamespace(namespace);
    }

    public Path getPath() {
        return relSourcePath;
    }

    public boolean isCompileOnly() {
        return compileOnly;
    }

    public int getLanguageLevel() {
        return languageLevel;
    }

    public float getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "TDN: " + location;
    }

    private static void resolveEntries(TokenList entryList, ISymbolContext parent, FunctionSection appendTo, boolean compileOnly) {
        TridentFile parentFile = parent.getStaticParentFile();

        boolean popCall = false;
        int postProcessingActionStartIndex = parentFile.postProcessingActions.size();
        parentFile.addCascadingRequires(Collections.emptyList());
        TridentCompiler compiler = parent.getCompiler();

        if(!parentFile.entriesResolved) {
            ImportedSymbolContext imports = new ImportedSymbolContext(parent.getCompiler());
            imports.setParentScope(parentFile.parentScope);
            for (Iterator<TridentUtil.ResourceLocation> it = new ArrayDeque<>(parentFile.cascadingRequires).descendingIterator(); it.hasNext(); ) {
                TridentUtil.ResourceLocation loc = it.next();
                TridentFile file = parent.getCompiler().getFile(loc);
                file.resolveEntries();
                imports.addContext(file);
            }
            parentFile.parentScope = imports;

            compiler.getCallStack().push(new CallStack.Call("<body>", entryList, parentFile, entryList));
            compiler.pushWritingFile(parentFile);
            popCall = true;
        }
        parentFile.entriesResolved = true;
        ArrayList<TridentException> queuedExceptions = new ArrayList<>();

        try {
            if (entryList != null) {
                TokenPattern<?>[] entries = (entryList).getContents();
                for (TokenPattern<?> pattern : entries) {
                    if (!pattern.getName().equals("LINE_PADDING")) {
                        TokenStructure entry = (TokenStructure) pattern.find("ENTRY");

                        TokenPattern<?> inner = entry.getContents();

                        try {
                            resolveEntry(inner, parent, appendTo, compileOnly);
                        } catch(TridentException x) {
                            if(compiler.getTryStack().isEmpty()) {
                                if(!popCall) throw x;
                                x.expandToUncaught();
                                compiler.getReport().addNotice(x.getNotice());
                                if(x.isBreaking() || parentFile.breaking) break;
                            } else if(compiler.getTryStack().isRecovering()) {
                                queuedExceptions.add(x);
                            } else if(compiler.getTryStack().isBreaking()) {
                                throw x;
                            }
                        } catch(TridentException.Grouped gx) {
                            queuedExceptions.addAll(gx.getExceptions());
                        }
                    }
                }
            }
            if(!queuedExceptions.isEmpty()) {
                TridentException.Grouped ex = new TridentException.Grouped(queuedExceptions);
                queuedExceptions = null;
                throw ex;
            }
        } finally {
            while(parentFile.postProcessingActions.size() > postProcessingActionStartIndex) {
                try {
                    parentFile.postProcessingActions.remove(postProcessingActionStartIndex).run();
                }  catch(TridentException x) {
                    if(compiler.getTryStack().isEmpty()) {
                        x.expandToUncaught();
                        compiler.getReport().addNotice(x.getNotice());
                        if(x.isBreaking() || parentFile.breaking) {
                            break;
                        }
                    } else {
                        if(queuedExceptions == null) queuedExceptions = new ArrayList<>();
                        queuedExceptions.add(x);
                    }
                } catch(TridentException.Grouped gx) {
                    if(queuedExceptions == null) queuedExceptions = new ArrayList<>();
                    queuedExceptions.addAll(gx.getExceptions());
                }
            }

            if(queuedExceptions != null && !queuedExceptions.isEmpty()) {
                for(TridentException x : queuedExceptions) {
                    x.expandToUncaught();
                    compiler.getReport().addNotice(x.getNotice());
                }
            }
            if(popCall) {
                compiler.getCallStack().pop();
                compiler.popWritingFile();
            }
        }
    }

    private boolean entriesResolved = false;

    public static void resolveEntry(TokenPattern<?> inner, ISymbolContext parent, FunctionSection appendTo, boolean compileOnly) {
        TridentCompiler compiler = parent.getCompiler();
        boolean exportComments = compiler.getProperties().get("export-comments") == null || compiler.getProperties().get("export-comments").getAsBoolean();
        try {
            switch (inner.getName()) {
                case "COMMAND_WRAPPER":
                    if (!compileOnly && appendTo != null) {

                        ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList((TokenList) inner.find("MODIFIERS"), parent);

                        TokenPattern<?> commandPattern = inner.find("COMMAND");
                        String commandName = ((TokenGroup) ((TokenStructure) commandPattern).getContents()).getContents()[0].flattenTokens().get(0).value;
                        CommandParser parser = AnalyzerManager.getAnalyzer(CommandParser.class, commandName);
                        if (parser != null) {
                            Collection<Command> commands = parser.parse(((TokenStructure) commandPattern).getContents(), parent, modifiers);
                            modifiers.addAll(0, parent.getWritingFile().getWritingModifiers());
                            for(Command command : commands) {
                                if (modifiers.isEmpty()) appendTo.append(command);
                                else appendTo.append(new ExecuteCommand(command, modifiers));
                            }
                        } else {
                            boolean found = false;
                            for(TridentPlugin plugin : parent.getCompiler().getWorker().output.transitivePlugins) {
                                CommandDefinition def = plugin.getCommand(commandName);
                                if(def != null) {
                                    new PluginCommandParser().handleCommand(def, commandPattern, modifiers, parent, appendTo);
                                    found = true;
                                    break;
                                }
                            }
                            if(!found) throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown command analyzer for '" + commandName + "'", commandPattern, parent);
                        }
                    } else if (!parent.getStaticParentFile().reportedNoCommands) {
                        parent.getStaticParentFile().reportedNoCommands = true;
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "A compile-only function may not have commands", inner, parent);
                    }
                    break;
                case "COMMENT":
                    if (exportComments && appendTo != null)
                        appendTo.append(new FunctionComment(inner.flattenTokens().get(0).value.substring(1)));
                    break;
                case "INSTRUCTION": {
                    String instructionKey = ((TokenStructure) inner).getContents().searchByName("INSTRUCTION_KEYWORD").get(0).flatten(false);
                    Instruction instruction = AnalyzerManager.getAnalyzer(Instruction.class, instructionKey);
                    if (instruction != null) {
                        instruction.run(((TokenStructure) inner).getContents(), parent);
                    } else {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown instruction analyzer for '" + instructionKey + "'", inner, parent);
                    }
                    break;
                } default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, parent);
                }
            }
        } catch(CommodoreException x) {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Commodore Exception of type " + x.getSource() + ": " + x.getMessage(), inner, parent);
        }
    }

    public TokenPattern<?> getPattern() {
        return pattern;
    }

    public DictionaryObject getMetadata() {
        return metadata;
    }

    public Collection<TridentUtil.ResourceLocation> getTags() {
        return tags;
    }

    public Collection<TridentUtil.ResourceLocation> getMetaTags() {
        return metaTags;
    }

    public boolean shouldExportFunction() {
        return shouldExportFunction;
    }

    public void setShouldExportFunction(boolean shouldExportFunction) {
        this.shouldExportFunction = shouldExportFunction;
        if(this.function != null) {
            this.function.setExport(shouldExportFunction);
        }
    }

    public void schedulePostResolutionAction(Runnable r) {
        postProcessingActions.add(r);
    }

    public File getDeclaringFSFile() {
        return pattern.getFile();
    }

    private HashMap<String, CustomClass> definedInnerClasses = null;

    public void registerInnerClass(CustomClass cls, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(definedInnerClasses == null) definedInnerClasses = new HashMap<>();
        if(definedInnerClasses.containsKey(cls.getClassName())) {
            throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Class '" + cls.getTypeIdentifier() + "' already exists", pattern, ctx);
        }
        definedInnerClasses.put(cls.getClassName(), cls);
    }

    public CustomClass getClassForName(String className) {
        if(definedInnerClasses == null) return null;
        return definedInnerClasses.get(className);
    }
}
