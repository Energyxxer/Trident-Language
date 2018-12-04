package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionComment;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
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
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.instructions.Instruction;
import com.energyxxer.trident.compiler.commands.parsers.modifiers.ModifierParser;

import java.io.File;
import java.nio.file.Path;
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

    private Function function;
    private final TridentUtil.ResourceLocation location;

    private boolean compileOnly = false;


    public TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this.compiler = compiler;
        this.module = compiler.getModule();
        this.namespace = module.createNamespace(relSourcePath.getName(0).toString());
        this.pattern = filePattern;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Matcher.quoteReplacement(File.separator), "/");
        this.location = new TridentUtil.ResourceLocation(this.namespace.getName() + ":" + functionPath);

        TokenPattern<?> directiveList = filePattern.find("..DIRECTIVES");
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
                    default: {
                        reportNotice(new Notice(NoticeType.DEBUG, "Unknown directive type '" + directiveBody.getName() + "'", directiveBody));
                    }
                }
            }
        }
        this.function = compileOnly ? null : namespace.functions.create(functionPath);

    }

    public TridentCompiler getCompiler() {
        return compiler;
    }

    private boolean reportedNoCommands = false;

    public void resolveEntries() {

        if(function != null) tags.forEach(l -> module.createNamespace(l.namespace).tags.functionTags.create(l.body).addValue(new FunctionReference(this.function)));

        TokenList entryList = (TokenList) this.pattern.find(".ENTRIES");

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
                                if (!compileOnly) {

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
                                        Command command = parser.parse(((TokenStructure) commandPattern).getContents(), this);
                                        if (command != null) {
                                            if(modifiers.isEmpty()) function.append(command);
                                            else function.append(new ExecuteCommand(command, modifiers));
                                        }
                                    }
                                } else if (!reportedNoCommands) {
                                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                                    reportedNoCommands = true;
                                }
                                break;
                            case "COMMENT":
                                if (exportComments && function != null)
                                    function.append(new FunctionComment(inner.flattenTokens().get(0).value.substring(1)));
                                break;
                            case "VERBATIM_COMMAND":
                                if (!compileOnly) {
                                    function.append(new RawCommand(inner.flattenTokens().get(0).value.substring(1)));
                                } else if (!reportedNoCommands) {
                                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "A compile-only function may not have commands", inner));
                                    reportedNoCommands = true;
                                }
                                break;
                            case "INSTRUCTION": {
                                String instructionKey = ((TokenStructure) inner).getContents().searchByName("INSTRUCTON_KEYWORD").get(0).flatten(false);
                                Instruction instruction = ParserManager.getParser(Instruction.class, instructionKey);
                                if (instruction != null) {
                                    instruction.run(((TokenStructure) inner).getContents(), this);
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
                    getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required Trident function '" + entry.getKey() + "' does not exist"));
                }
            }
        }
    }

    public TridentUtil.ResourceLocation getResourceLocation() {
        return location;
    }

    public boolean isCompileOnly() {
        return compileOnly;
    }

    @Override
    public String toString() {
        return "TDN: " + location;
    }
}
