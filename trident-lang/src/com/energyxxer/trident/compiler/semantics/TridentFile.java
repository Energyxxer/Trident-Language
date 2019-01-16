package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionComment;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.*;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.CompilerExtension;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.RawCommand;
import com.energyxxer.trident.compiler.commands.parsers.commands.CommandParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.instructions.Instruction;
import com.energyxxer.trident.compiler.commands.parsers.modifiers.ModifierParser;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TridentFile implements CompilerExtension {
    private final TridentCompiler compiler;
    private final CommandModule module;
    private final Namespace namespace;
    private TokenPattern<?> pattern;
    private final HashMap<TokenPattern<?>, TridentUtil.ResourceLocation> requires = new HashMap<>();
    private ArrayList<TridentUtil.ResourceLocation> cascadingRequires = null;
    private final ArrayList<TridentUtil.ResourceLocation> tags = new ArrayList<>();
    private final Path relSourcePath;

    private Function function;
    private final TridentUtil.ResourceLocation location;

    private boolean compileOnly = false;
    private boolean valid = true;

    private int languageLevel;

    private int anonymousChildren = 0;

    private SymbolTable fileSymbols;

    public TridentFile(TridentFile parent, Path relSourcePath, TokenPattern<?> filePattern) {
        this(parent.getCompiler(), relSourcePath, filePattern, parent.languageLevel);
    }

    public TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this(compiler, relSourcePath, filePattern, compiler.getLanguageLevel());
    }

    private TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern, int languageLevel) {
        this.compiler = compiler;
        this.module = compiler.getModule();
        this.namespace = module.createNamespace(relSourcePath.getName(0).toString());
        this.pattern = filePattern;
        this.relSourcePath = relSourcePath;

        this.languageLevel = languageLevel;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Pattern.quote(File.separator), "/");
        this.location = new TridentUtil.ResourceLocation(this.namespace.getName() + ":" + functionPath);

        resolveDirectives();
        if(!compileOnly && namespace.functions.contains(functionPath)) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A function by the name '" + namespace.getName() + ":" + functionPath + "' already exists", pattern));
        }

        this.function = compileOnly ? null : namespace.functions.get(functionPath);
    }

    public static TridentFile createInnerFile(TokenPattern<?> pattern, TridentFile parent) {
        TokenPattern<?> namePattern = pattern.find("INNER_FUNCTION_NAME.RESOURCE_LOCATION");
        String innerFilePathRaw = parent.getPath().toString();
        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()-".tdn".length());

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
            innerFilePathRaw = Paths.get(innerFilePathRaw).resolve("_anonymous" + parent.anonymousChildren + ".tdn").toString();
            parent.anonymousChildren++;
        }

        TokenPattern<?> innerFilePattern = pattern.find("FILE_INNER");

        TridentFile innerFile = new TridentFile(parent.getCompiler(), Paths.get(innerFilePathRaw), innerFilePattern);
        innerFile.resolveEntries();
        return innerFile;
    }

    public static void resolveInnerFileIntoSection(TokenPattern<?> pattern, TridentFile parent, FunctionSection function) {
        if(pattern.find("FILE_INNER..DIRECTIVES") != null) {
            parent.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Directives aren't allowed in this context", pattern.find("FILE_INNER..DIRECTIVES")));
            throw new EntryParsingException();
        }

        resolveEntries((TokenList) pattern.find("FILE_INNER.ENTRIES"), parent, function, false);
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
                                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-ony function may not have any tags", directiveList));
                            }
                            compileOnly = true;
                        }
                        break;
                    }
                    case "TAG_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents());
                        if(compileOnly) {
                            getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-ony function may not have any tags", directiveList));
                        }
                        tags.add(loc);
                        break;
                    }
                    case "REQUIRE_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents());
                        requires.put(directiveBody, loc);
                        break;
                    }
                    case "LANGUAGE_LEVEL_DIRECTIVE": {
                        int level = CommonParsers.parseInt(directiveBody.find("INTEGER"), this);
                        if(level < 1 || level > 3) {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Invalid language level: " + level, directiveBody.find("INTEGER")));
                        } else this.languageLevel = level;
                        break;
                    }
                    default: {
                        reportNotice(new Notice(NoticeType.DEBUG, "Unknown directive type '" + directiveBody.getName() + "'", directiveBody));
                    }
                }
            }
        }
    }

    public void addCascadingRequires(Collection<TridentUtil.ResourceLocation> locations) {
        if(cascadingRequires == null) cascadingRequires = new ArrayList<>(requires.values());
        cascadingRequires.addAll(locations);
    }

    private boolean reportedNoCommands = false;

    public void resolveEntries() {
        if(!valid) return;
        if(fileSymbols != null) return;

        if(function != null) tags.forEach(l -> module.createNamespace(l.namespace).tags.functionTags.create(l.body).addValue(new FunctionReference(this.function)));

        resolveEntries((TokenList) this.pattern.find(".ENTRIES"), this, function, compileOnly);
    }

    public TridentCompiler getCompiler() {
        return compiler;
    }

    public Function getFunction() {
        return function;
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
        return namespace;
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

    public Function getTickFunction() {
        boolean creating = !namespace.functions.contains("trident_tick");
        Function tickFunction = namespace.functions.get("trident_tick");

        if(creating) {
            Tag tickTag = compiler.getModule().minecraft.tags.functionTags.create("tick");
            tickTag.addValue(new FunctionReference(tickFunction));
        }
        return tickFunction;
    }

    @Override
    public String toString() {
        return "TDN: " + location;
    }

    public static void resolveEntries(TokenList entryList, TridentFile parent, FunctionSection appendTo, boolean compileOnly) {

        int popTimes = 0;
        parent.addCascadingRequires(Collections.emptyList());
        for (Iterator<TridentUtil.ResourceLocation> it = parent.cascadingRequires.parallelStream().collect(Collectors.toCollection(ArrayDeque::new)).descendingIterator(); it.hasNext(); ) {
            TridentUtil.ResourceLocation loc = it.next();
            TridentFile file = parent.compiler.getFile(loc);
            if(file.fileSymbols == null) {
                file.resolveEntries();
            }
            parent.compiler.getStack().push(file.fileSymbols);
            popTimes++;
        }

        TridentCompiler compiler = parent.getCompiler();
        SymbolTable symbols = new SymbolTable(parent);
        if(parent.fileSymbols == null) {
            parent.fileSymbols = symbols;
        }
        compiler.getStack().push(symbols);
        popTimes++;

        try {
            if (entryList != null) {
                TokenPattern<?>[] entries = (entryList).getContents();
                for (TokenPattern<?> pattern : entries) {
                    if (!pattern.getName().equals("LINE_PADDING")) {
                        TokenStructure entry = (TokenStructure) pattern.find("ENTRY");

                        TokenPattern<?> inner = entry.getContents();

                        try {
                            resolveEntry(inner, parent, appendTo, compileOnly);
                        } catch (EntryParsingException x) {
                            //Silently ignore; serves as a multi-scope break;
                        }
                    }
                }
            }
        } finally {
            for(int i = 0; i < popTimes; i++) {
                compiler.getStack().pop();
            }
        }
    }

    public static void resolveEntry(TokenPattern<?> inner, TridentFile parent, FunctionSection appendTo, boolean compileOnly) {
        TridentCompiler compiler = parent.getCompiler();
        boolean exportComments = compiler.getProperties().get("export-comments") == null || compiler.getProperties().get("export-comments").getAsBoolean();
        switch (inner.getName()) {
            case "COMMAND_WRAPPER":
                if (!compileOnly && appendTo != null) {

                    ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

                    TokenList modifierList = (TokenList) inner.find("MODIFIERS");
                    if(modifierList != null) {
                        for(TokenPattern<?> rawModifier : modifierList.getContents()) {
                            ModifierParser parser = ParserManager.getParser(ModifierParser.class, rawModifier.flattenTokens().get(0).value);
                            if(parser != null) {
                                ExecuteModifier modifier = parser.parse(rawModifier, parent);
                                if(modifier != null) modifiers.add(modifier);
                            }
                        }
                    }

                    TokenPattern<?> commandPattern = inner.find("COMMAND");
                    CommandParser parser = ParserManager.getParser(CommandParser.class, commandPattern.flattenTokens().get(0).value);
                    if (parser != null) {
                        Command command = parser.parse(((TokenStructure) commandPattern).getContents(), parent);
                        if (command != null) {
                            if(modifiers.isEmpty()) appendTo.append(command);
                            else appendTo.append(new ExecuteCommand(command, modifiers));
                        }
                    }
                } else if (!parent.reportedNoCommands) {
                    parent.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                    parent.reportedNoCommands = true;
                }
                break;
            case "COMMENT":
                if (exportComments && appendTo != null)
                    appendTo.append(new FunctionComment(inner.flattenTokens().get(0).value.substring(1)));
                break;
            case "VERBATIM_COMMAND":
                if (!compileOnly && appendTo != null) {
                    appendTo.append(new RawCommand(inner.flattenTokens().get(0).value.substring(1)));
                } else if (!parent.reportedNoCommands) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                    parent.reportedNoCommands = true;
                }
                break;
            case "INSTRUCTION": {
                String instructionKey = ((TokenStructure) inner).getContents().searchByName("INSTRUCTION_KEYWORD").get(0).flatten(false);
                Instruction instruction = ParserManager.getParser(Instruction.class, instructionKey);
                if (instruction != null) {
                    instruction.run(((TokenStructure) inner).getContents(), parent);
                }
                break;
            }
        }
    }
}
