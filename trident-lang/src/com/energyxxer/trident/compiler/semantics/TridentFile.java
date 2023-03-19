package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.CommodoreException;
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
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.PrismarineLanguageUnit;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.state.CallStack;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.TridentFileUnitConfiguration;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ImportedSymbolContext;
import com.energyxxer.trident.worker.tasks.SetupBuildConfigTask;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;
import com.energyxxer.trident.worker.tasks.ValidatePropertiesTask;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class TridentFile extends PrismarineLanguageUnit {
    private final TridentFile rootFile;
    private String namespace;
    private TokenPattern<?> pattern;
    private final HashMap<TokenPattern<?>, ResourceLocation> requires = new HashMap<>();
    private ArrayList<ResourceLocation> cascadingRequires = null;
    private final ArrayList<ResourceLocation> tags = new ArrayList<>();
    private final ArrayList<ResourceLocation> metaTags = new ArrayList<>();

    private Function function;
    private final ArrayList<ExecuteModifier> writingModifiers = new ArrayList<>();
    private final ResourceLocation location;

    private boolean compileOnly = false;
    private boolean breaking = false;
    private float priority = 0;
    private boolean valid = true;

    private int languageLevel;

    private int anonymousChildren = 0;

    private DictionaryObject metadata;

    private boolean shouldExportFunction = true;

    private ArrayList<Runnable> postProcessingActions = new ArrayList<>();

    public TridentFile(PrismarineCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this(compiler, null, relSourcePath, filePattern, compiler.get(ValidatePropertiesTask.INSTANCE).languageLevel);
    }

    private TridentFile(PrismarineCompiler compiler, ISymbolContext parentContext, Path relSourcePath, TokenPattern<?> filePattern, int languageLevel) {
        super(compiler, relSourcePath);
        this.parentScope = parentContext;
        this.pattern = filePattern;
        if(parentContext == null) {
            rootFile = null;
        } else {
            rootFile = ((TridentFile) parentContext.getStaticParentUnit()).rootFile;
        }

        this.languageLevel = languageLevel;

        this.location = TridentFileUnitConfiguration.functionPathToResourceLocation(relSourcePath);
        this.namespace = this.location.namespace;

        resolveDirectives();
        /*if(!compileOnly && getNamespace().functions.exists(functionPath)) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A function by the name '" + namespace + ":" + functionPath + "' already exists", pattern));
            valid = false;
        }*/

        if(!compileOnly && getNamespace().functions.exists(this.location.body)) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A function by the name '" + this.location + "' already exists", pattern));
            valid = false;
        }

        this.function = compileOnly ? null : getNamespace().functions.getOrCreate(this.location.body);
        setShouldExportFunction(parentContext == null || ((TridentFile) parentContext.getStaticParentUnit()).shouldExportFunction);

        if(function != null) tags.forEach(l -> {
            Tag tag = get(SetupModuleTask.INSTANCE).getNamespace(l.namespace).tags.functionTags.getOrCreate(l.body);
            tag.setExport(true);
            tag.addValue(new FunctionReference(this.function));
        });
    }

    private void resolveDirectives() {
        TokenPattern<?> directiveList = pattern.find("DIRECTIVES");
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
                        ResourceLocation loc = new ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents().value);
                        if(compileOnly) {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have any tags", directiveList));
                        }
                        tags.add(loc);
                        break;
                    }
                    case "META_TAG_DIRECTIVE": {
                        ResourceLocation loc = new ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents().value);
                        metaTags.add(loc);
                        break;
                    }
                    case "REQUIRE_DIRECTIVE": {
                        ResourceLocation loc = new ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents().value);
                        requires.put(directiveBody, loc);
                        break;
                    }
                    case "LANGUAGE_LEVEL_DIRECTIVE": {
                        int level = (int) directiveBody.find("INTEGER").evaluate(this, null);
                        if(level < 1 || level > 3) {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Invalid language level: " + level, directiveBody.find("INTEGER")));
                        } else this.languageLevel = level;
                        break;
                    }
                    case "PRIORITY_DIRECTIVE": {
                        this.priority = (float) (double) directiveBody.find("REAL").evaluate(this, null);
                        break;
                    }
                    case "BREAKING_DIRECTIVE": {
                        this.breaking = true;
                        break;
                    }
                    case "METADATA_DIRECTIVE": {
                        if (this.metadata == null) {
                            this.metadata = (DictionaryObject) directiveBody.find("DICTIONARY").evaluate(this, null);
                        } else {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Duplicate metadata directive", directiveList));
                        }
                        break;
                    }
                    case "USING_PLUGIN_DIRECTIVE":
                        break;
                    default: {
                        getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown directive type '" + directiveBody.getName() + "'", directiveBody));
                    }
                }
            }
        }
        if(metadata == null) {
            metadata = new DictionaryObject(this.getTypeSystem());
        }
    }

    public static Function createAnonymousSubFunction(ISymbolContext parent) {
        String innerFilePathRaw = parent.getStaticParentUnit().getPathFromRoot().toString();
        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()- Trident.FUNCTION_EXTENSION.length());

        Path relSourcePath = Paths.get(innerFilePathRaw).resolve(parent.get(ValidatePropertiesTask.INSTANCE).createAnonymousFunctionName(((TridentFile) parent.getStaticParentUnit()).anonymousChildren) + Trident.FUNCTION_EXTENSION);
        ((TridentFile) parent.getStaticParentUnit()).anonymousChildren++;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()- Trident.FUNCTION_EXTENSION.length()).replaceAll(Pattern.quote(File.separator), "/");

        return parent.get(SetupModuleTask.INSTANCE).getNamespace(relSourcePath.getName(0).toString()).functions.create(functionPath);
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
        String innerFilePathRaw = parent.getStaticParentUnit().getPathFromRoot().toString();
        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()- Trident.FUNCTION_EXTENSION.length());
        if(subPath != null) {
            innerFilePathRaw = Paths.get(innerFilePathRaw).resolve(subPath).toString();
        }

        if(namePattern != null) {
            ResourceLocation suggestedLoc = (ResourceLocation) namePattern.evaluate(parent, null);
            suggestedLoc.assertStandalone(namePattern, parent);
            if(!suggestedLoc.namespace.equals("minecraft")) {
                innerFilePathRaw = TridentFileUnitConfiguration.resourceLocationToFunctionPath(suggestedLoc).toString();
            } else {
                String functionName = suggestedLoc.body;
                while(functionName.startsWith("/")) {
                    functionName = functionName.substring(1);
                }
                innerFilePathRaw = Paths.get(innerFilePathRaw).resolve(functionName + Trident.FUNCTION_EXTENSION).toString();
            }
        } else {
            innerFilePathRaw = Paths.get(innerFilePathRaw).resolve(
                    parent.get(ValidatePropertiesTask.INSTANCE)
                            .createAnonymousFunctionName(
                                    ((TridentFile) parent.getStaticParentUnit()).anonymousChildren
                            ) + Trident.FUNCTION_EXTENSION
            ).toString();
            ((TridentFile) parent.getStaticParentUnit()).anonymousChildren++;
        }

        TokenPattern<?> innerFilePattern = pattern.find("FILE_INNER");

        TridentFile innerFile = new TridentFile(parent.getCompiler(), parent, Paths.get(innerFilePathRaw), innerFilePattern, ((TridentFile) parent.getStaticParentUnit()).languageLevel);
        if(autoResolve) innerFile.resolveEntries();
        return innerFile;
    }

    //Sub context NOT automatically created
    public static void resolveInnerFileIntoSection(TokenPattern<?> pattern, ISymbolContext parentCtx, FunctionSection function) {
        if(pattern.find("FILE_INNER..DIRECTIVES") != null) {
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Directives aren't allowed in this context", pattern.find("FILE_INNER..DIRECTIVES"), parentCtx);
        }

        resolveFunctionEntries((TokenList) pattern.find("FILE_INNER.ENTRIES"), parentCtx, function, false);
    }

    //Sub context NOT automatically created
    public static void resolveFileIntoSection(TokenPattern<?> pattern, ISymbolContext parentCtx, FunctionSection function) {
        if(pattern.find("DIRECTIVES") != null) {
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Directives aren't allowed in this context", pattern.find("DIRECTIVES"), parentCtx);
        }

        resolveFunctionEntries((TokenList) pattern.find("ENTRIES"), parentCtx, function, false);
    }

    //Sub context NOT automatically created
    public static void resolveEntryListIntoSection(TokenList list, ISymbolContext parentCtx, FunctionSection function) {
        resolveEntries(list, parentCtx, function, false);
    }

    public void addCascadingRequires(Collection<ResourceLocation> locations) { //TODO clean up; this lazy initialization is misleading
        if(cascadingRequires == null) cascadingRequires = new ArrayList<>(requires.values());
        cascadingRequires.addAll(locations);
    }

    public boolean reportedNoCommands = false;

    public void reportNoCommands(TokenPattern<?> p, ISymbolContext ctx) {
        if(!reportedNoCommands) {
            reportedNoCommands = true;
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "A compile-only function may not have commands", p, ctx);
        }
    }

    public void resolveEntries() {
        if(!valid) return;
        if(entriesResolved) return;

        resolveEntries((TokenList) this.pattern.find("ENTRIES"), this, this.function, compileOnly);
    }

    public Function getFunction() {
        return this.function;
    }

    public ArrayList<ExecuteModifier> getWritingModifiers() {
        return writingModifiers;
    }

    public Collection<ResourceLocation> getRequires() {
        return requires.values();
    }

    public Collection<ResourceLocation> getCascadingRequires() {
        return cascadingRequires;
    }

    public void checkCircularRequires() {
        this.checkCircularRequires(new ArrayList<>());
    }

    private void checkCircularRequires(ArrayList<ResourceLocation> previous) {
        previous.add(this.location);
        for(Map.Entry<TokenPattern<?>, ResourceLocation> entry : requires.entrySet()) {
            if(previous.contains(entry.getValue())) {
                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Circular requirement with function '" + entry.getValue() + "'", entry.getKey()));
                Debug.log("Previous (for file '" + this.getResourceLocation() + "'): " + previous);
            } else {
                TridentFile next = getCompiler().getUnit(TridentFileUnitConfiguration.INSTANCE, TridentFileUnitConfiguration.resourceLocationToFunctionPath(entry.getValue()));
                if(next != null) {
                    next.checkCircularRequires(previous);
                } else {
                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required Trident Function '" + entry.getValue().toString().replace(File.separatorChar,'/') + "' does not exist", entry.getKey()));
                }
            }
        }
        previous.remove(this.location);
    }

    public ResourceLocation getResourceLocation() {
        return location;
    }

    public Namespace getNamespace() {
        return get(SetupModuleTask.INSTANCE).getNamespace(namespace);
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
        TridentFile parentFile = (TridentFile) parent.getStaticParentUnit();

        boolean popCall = false;
        int postProcessingActionStartIndex = parentFile.postProcessingActions.size();
        parentFile.addCascadingRequires(Collections.emptyList());
        PrismarineCompiler compiler = parent.getCompiler();

        if(!parentFile.entriesResolved) {
            ImportedSymbolContext imports = new ImportedSymbolContext(parent.getCompiler());
            imports.setParentScope(parentFile.parentScope);
            for (Iterator<ResourceLocation> it = new ArrayDeque<>(parentFile.cascadingRequires).descendingIterator(); it.hasNext(); ) {
                ResourceLocation loc = it.next();
                TridentFile file = parent.getCompiler().getUnit(TridentFileUnitConfiguration.INSTANCE, TridentFileUnitConfiguration.resourceLocationToFunctionPath(loc));
                file.resolveEntries();
                imports.addContext(file);
            }
            parentFile.parentScope = imports;

            compiler.getCallStack().push(new CallStack.Call("<body>", entryList, parentFile, entryList));
            compiler.get(SetupWritingStackTask.INSTANCE).pushWritingFile(parentFile);
            popCall = true;
        }
        parentFile.entriesResolved = true;
        ArrayList<PrismarineException> queuedExceptions = new ArrayList<>();

        try {
            if (entryList != null) {
                TokenPattern<?>[] entries = (entryList).getContents();
                if(parentFile.breaking) compiler.getTryStack().pushBreaking();
                try {
                    for (TokenPattern<?> pattern : entries) {
                        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();

                        try {
                            resolveEntries(inner, parent, appendTo, compileOnly);
                        } catch(PrismarineException x) {
                            if(compiler.getTryStack().isEmpty() || (parentFile.breaking && compiler.getTryStack().isBreaking())) {
                                if(!popCall) throw x;
                                x.expandToUncaught();
                                compiler.getReport().addNotice(x.getNotice());
                                if(x.isBreaking() || parentFile.breaking) break;
                            } else if(compiler.getTryStack().isRecovering()) {
                                queuedExceptions.add(x);
                            } else if(compiler.getTryStack().isBreaking()) {
                                throw x;
                            }
                        } catch(PrismarineException.Grouped gx) {
                            queuedExceptions.addAll(gx.getExceptions());
                        }
                    }
                } finally {
                    if(parentFile.breaking) compiler.getTryStack().pop();
                }
            }
            if(!queuedExceptions.isEmpty()) {
                PrismarineException.Grouped ex = new PrismarineException.Grouped(queuedExceptions);
                queuedExceptions = null;
                throw ex;
            }
        } finally {
            while(parentFile.postProcessingActions.size() > postProcessingActionStartIndex) {
                try {
                    parentFile.postProcessingActions.remove(postProcessingActionStartIndex).run();
                }  catch(PrismarineException x) {
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
                } catch(PrismarineException.Grouped gx) {
                    if(queuedExceptions == null) queuedExceptions = new ArrayList<>();
                    queuedExceptions.addAll(gx.getExceptions());
                }
            }

            if(queuedExceptions != null && !queuedExceptions.isEmpty()) {
                for(PrismarineException x : queuedExceptions) {
                    x.expandToUncaught();
                    compiler.getReport().addNotice(x.getNotice());
                }
            }
            if(popCall) {
                compiler.getCallStack().pop();
                compiler.get(SetupWritingStackTask.INSTANCE).popWritingFile();
            }
        }
    }

    private boolean entriesResolved = false;

    public static void resolveEntries(TokenPattern<?> inner, ISymbolContext parent, FunctionSection appendTo, boolean compileOnly) {
        try {
            switch (inner.getName()) {
                case "COMMENT":
                    if (parent.get(SetupBuildConfigTask.INSTANCE).exportComments && appendTo != null)
                        appendTo.append(new FunctionComment(inner.flatten(false).substring(1)));
                    break;
                default: {
                    inner.evaluate(parent, new Object[] {appendTo});
                    break;
                }
            }
        } catch(CommodoreException x) {
            if(x.getSource() == CommodoreException.Source.VERSION_ERROR) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, x.getSource() + ": " + x.getMessage(), inner, parent);
            } else {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Commodore Exception of type " + x.getSource() + ": " + x.getMessage(), inner, parent);
            }
        }
    }

    private static void resolveFunctionEntries(TokenList entryList, ISymbolContext parent, FunctionSection appendTo, boolean compileOnly) {
        TridentFile parentFile = (TridentFile) parent.getStaticParentUnit();

        boolean popCall = false;
        int postProcessingActionStartIndex = parentFile.postProcessingActions.size();
        parentFile.addCascadingRequires(Collections.emptyList());
        PrismarineCompiler compiler = parent.getCompiler();

        if(!parentFile.entriesResolved) {
            ImportedSymbolContext imports = new ImportedSymbolContext(parent.getCompiler());
            imports.setParentScope(parentFile.parentScope);
            for (Iterator<ResourceLocation> it = new ArrayDeque<>(parentFile.cascadingRequires).descendingIterator(); it.hasNext(); ) {
                ResourceLocation loc = it.next();
                TridentFile file = parent.getCompiler().getUnit(TridentFileUnitConfiguration.INSTANCE, TridentFileUnitConfiguration.resourceLocationToFunctionPath(loc));
                file.resolveEntries();
                imports.addContext(file);
            }
            parentFile.parentScope = imports;

            compiler.getCallStack().push(new CallStack.Call("<body>", entryList, parentFile, entryList));
            compiler.get(SetupWritingStackTask.INSTANCE).pushWritingFile(parentFile);
            popCall = true;
        }
        parentFile.entriesResolved = true;
        ArrayList<PrismarineException> queuedExceptions = new ArrayList<>();

        try {
            if (entryList != null) {
                TokenPattern<?>[] entries = (entryList).getContents();
                if(parentFile.breaking) compiler.getTryStack().pushBreaking();
                try {
                    for (TokenPattern<?> pattern : entries) {
                        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();

                        try {
                            resolveEntry(inner, parent, appendTo, compileOnly);
                        } catch(PrismarineException x) {
                            if(compiler.getTryStack().isEmpty() || (parentFile.breaking && compiler.getTryStack().isBreaking())) {
                                if(!popCall) throw x;
                                x.expandToUncaught();
                                compiler.getReport().addNotice(x.getNotice());
                                if(x.isBreaking() || parentFile.breaking) break;
                            } else if(compiler.getTryStack().isRecovering()) {
                                queuedExceptions.add(x);
                            } else if(compiler.getTryStack().isBreaking()) {
                                throw x;
                            }
                        } catch(PrismarineException.Grouped gx) {
                            queuedExceptions.addAll(gx.getExceptions());
                        }
                    }
                } finally {
                    if(parentFile.breaking) compiler.getTryStack().pop();
                }
            }
            if(!queuedExceptions.isEmpty()) {
                PrismarineException.Grouped ex = new PrismarineException.Grouped(queuedExceptions);
                queuedExceptions = null;
                throw ex;
            }
        } finally {
            while(parentFile.postProcessingActions.size() > postProcessingActionStartIndex) {
                try {
                    parentFile.postProcessingActions.remove(postProcessingActionStartIndex).run();
                }  catch(PrismarineException x) {
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
                } catch(PrismarineException.Grouped gx) {
                    if(queuedExceptions == null) queuedExceptions = new ArrayList<>();
                    queuedExceptions.addAll(gx.getExceptions());
                }
            }

            if(queuedExceptions != null && !queuedExceptions.isEmpty()) {
                for(PrismarineException x : queuedExceptions) {
                    x.expandToUncaught();
                    compiler.getReport().addNotice(x.getNotice());
                }
            }
            if(popCall) {
                compiler.getCallStack().pop();
                compiler.get(SetupWritingStackTask.INSTANCE).popWritingFile();
            }
        }
    }

    public static void resolveEntry(TokenPattern<?> inner, ISymbolContext parent, FunctionSection appendTo, boolean compileOnly) {
        try {
            inner.evaluate(parent, new Object[] {appendTo});
        } catch(CommodoreException x) {
            if(x.getSource() == CommodoreException.Source.VERSION_ERROR) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, x.getSource() + ": " + x.getMessage(), inner, parent);
            } else {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Commodore Exception of type " + x.getSource() + ": " + x.getMessage(), inner, parent);
            }
        }
    }


    public TokenPattern<?> getPattern() {
        return pattern;
    }

    public DictionaryObject getMetadata() {
        return metadata;
    }

    public Collection<ResourceLocation> getTags() {
        return tags;
    }

    public Collection<ResourceLocation> getMetaTags() {
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

    private HashMap<String, CustomClass> definedInnerClasses = null;

    public void registerInnerClass(CustomClass cls, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(definedInnerClasses == null) definedInnerClasses = new HashMap<>();
        if(definedInnerClasses.containsKey(cls.getClassName())) {
            throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Class '" + cls.getTypeIdentifier() + "' already exists", pattern, ctx);
        }
        definedInnerClasses.put(cls.getClassName(), cls);
    }

    public CustomClass getClassForName(String className) {
        if(definedInnerClasses == null) return null;
        return definedInnerClasses.get(className);
    }

    public boolean isBreaking() {
        return breaking;
    }

    public TridentFile getRootFile() {
        return rootFile;
    }
}
