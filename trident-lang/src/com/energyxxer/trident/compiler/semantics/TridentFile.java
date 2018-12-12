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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class TridentFile implements CompilerExtension {
    private final TridentCompiler compiler;
    private final CommandModule module;
    private final Namespace namespace;
    private TokenPattern<?> pattern;
    private final HashMap<TokenPattern<?>, TridentUtil.ResourceLocation> requires = new HashMap<>();
    private final ArrayList<TridentUtil.ResourceLocation> tags = new ArrayList<>();
    private final Path relSourcePath;

    private Function function;
    private final TridentUtil.ResourceLocation location;

    private boolean compileOnly = false;
    private boolean valid = true;

    private int languageLevel;

    private int anonymousChildren = 0;

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
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Matcher.quoteReplacement(File.separator), "/");
        this.location = new TridentUtil.ResourceLocation(this.namespace.getName() + ":" + functionPath);

        resolveDirectives();
        if(!compileOnly && namespace.functions.contains(functionPath)) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A function by the name '" + namespace.getName() + ":" + functionPath + "' already exists", pattern));
        }

        this.function = compileOnly ? null : namespace.functions.get(functionPath);
    }

    public static TridentFile createInnerFile(TokenPattern<?> pattern, TridentFile parent) {
        TokenPattern<?> namePattern = pattern.find("INNER_FUNCTION_NAME");
        String functionName;
        if(namePattern != null) {
            functionName = new TridentUtil.ResourceLocation(namePattern.flatten(false)).body;
            while(functionName.startsWith('/')) {
                functionName = functionName.substring(1);
            }
        } else {
            functionName = "_anonymous" + parent.anonymousChildren;
            parent.anonymousChildren++;
        }

        TokenPattern<?> innerFilePattern = pattern.find("FILE_INNER");
        String innerFilePathRaw = parent.getPath().toString();
        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()-".tdn".length());

        TridentFile innerFile = new TridentFile(parent.getCompiler(), Paths.get(innerFilePathRaw).resolve(functionName + ".tdn"), innerFilePattern);
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
                        int level = CommonParsers.parseInt(directiveBody.find("INTEGER"), compiler);
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

    private boolean reportedNoCommands = false;

    public void resolveEntries() {
        if(!valid) return;

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

    public void checkCircularRequires() {
        this.checkCircularRequires(new ArrayList<>());
    }

    private void checkCircularRequires(ArrayList<TridentUtil.ResourceLocation> previous) {
        previous.add(this.location);
        for(Map.Entry<TokenPattern<?>, TridentUtil.ResourceLocation> entry : requires.entrySet()) {
            if(previous.contains(entry.getValue())) {
                getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Circular requirement with function '" + entry.getValue() + "'", entry.getKey()));
            } else {
                TridentFile next = getCompiler().getFile(entry.getValue());
                if(next != null) {
                    next.checkCircularRequires(previous);
                } else {
                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required Trident function '" + entry.getValue() + "' does not exist", entry.getKey()));
                }
            }
        }
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
        SymbolTable table = new SymbolTable(parent);
        TridentCompiler compiler = parent.getCompiler();
        compiler.getStack().push(table);

        boolean reportedNoCommands = false;

        if(entryList != null) {
            TokenPattern<?>[] entries = (entryList).getContents();
            boolean exportComments = compiler.getProperties().get("export-comments") == null || compiler.getProperties().get("export-comments").getAsBoolean();
            for (TokenPattern<?> pattern : entries) {
                if (!pattern.getName().equals("LINE_PADDING")) {
                    TokenStructure entry = (TokenStructure) pattern.find("ENTRY");

                    TokenPattern<?> inner = entry.getContents();

                    try {
                        switch (inner.getName()) {
                            case "COMMAND_WRAPPER":
                                if (!compileOnly && appendTo != null) {

                                    ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

                                    TokenList modifierList = (TokenList) inner.find("MODIFIERS");
                                    if(modifierList != null) {
                                        for(TokenPattern<?> rawModifier : modifierList.getContents()) {
                                            ModifierParser parser = ParserManager.getParser(ModifierParser.class, rawModifier.flattenTokens().get(0).value);
                                            if(parser != null) {
                                                ExecuteModifier modifier = parser.parse(rawModifier, compiler);
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
                                } else if (!reportedNoCommands) {
                                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                                    reportedNoCommands = true;
                                }
                                break;
                            case "COMMENT":
                                if (exportComments && appendTo != null)
                                    appendTo.append(new FunctionComment(inner.flattenTokens().get(0).value.substring(1)));
                                break;
                            case "VERBATIM_COMMAND":
                                if (!compileOnly && appendTo != null) {
                                    appendTo.append(new RawCommand(inner.flattenTokens().get(0).value.substring(1)));
                                } else if (!reportedNoCommands) {
                                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                                    reportedNoCommands = true;
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
                    } catch (EntryParsingException x) {
                        //Silently ignore; serves as a multi-scope break;
                    }
                }
            }
        }

        compiler.getStack().pop();
    }
}
