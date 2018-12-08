package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.TextParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.selectors.TypeArgumentParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

import java.nio.file.Path;

@ParserMember(key = "register")
public class RegisterInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "REGISTER_OBJECTIVE":
                registerObjective(inner, file);
                break;
            case "REGISTER_ENTITY":
                registerEntity(inner, file);
                break;
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
            }
        }
    }

    private void registerObjective(TokenPattern<?> pattern, TridentFile file) {
        String objectiveName = pattern.find("OBJECTIVE_NAME").flatten(false);
        String criteria = "dummy";
        TextComponent displayName = null;

        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            criteria = sub.find("CRITERIA").flatten(false);
            TokenPattern<?> rawDisplayName = sub.find(".TEXT_COMPONENT");
            if(rawDisplayName != null) {
                displayName = TextParser.parseTextComponent(rawDisplayName, file.getCompiler());
            }
        }

        if(file.getCompiler().getModule().getObjectiveManager().contains(objectiveName)) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "An objective with the name '" + objectiveName + "' has already been registered", pattern));
        } else {
            file.getCompiler().getModule().getObjectiveManager().create(objectiveName, criteria, displayName, true);
        }
    }

    private void registerEntity(TokenPattern<?> pattern, TridentFile file) {
        String entityName = pattern.find("ENTITY_NAME").flatten(false);
        Type defaultType = CommonParsers.parseEntityType(pattern.find("ENTITY_ID"), file.getCompiler());

        CustomEntity entityDecl = new CustomEntity(entityName, defaultType);

        var bodyEntries = (TokenList) pattern.find("ENTITY_DECLARATION_BODY.ENTITY_BODY_ENTRIES");

        SymbolTable table = file.getCompiler().getStack().getGlobal();
        table.put(new Symbol(entityName, Symbol.SymbolAccess.GLOBAL, entityDecl));

        if(bodyEntries != null) {
            for(var rawEntry : bodyEntries.getContents()) {
                var entry = ((TokenStructure) rawEntry).getContents();
                switch(entry.getName()) {
                    case "DEFAULT_NBT": {
                        entityDecl.setDefaultNBT(NBTParser.parseCompound(entry.find("NBT_COMPOUND"), file.getCompiler()));
                        break;
                    }
                    case "DEFAULT_PASSENGERS": {
                        TagCompound oldNBT = entityDecl.getDefaultNBT();

                        if(oldNBT == null) oldNBT = new TagCompound();

                        TagList passengersTag = new TagList("Passengers");

                        for(var rawPassenger : ((TokenList)entry.find("PASSENGER_LIST")).getContents()) {
                            if(rawPassenger.getName().equals("PASSENGER")) {

                                TagCompound passengerCompound;

                                Object reference = CommonParsers.parseEntityReference(rawPassenger.find("ENTITY_ID"), file.getCompiler());

                                if(reference instanceof Type) {
                                    passengerCompound = new TagCompound(new TagString("id", reference.toString()));
                                } else if(reference instanceof CustomEntity) {
                                    passengerCompound = ((CustomEntity) reference).getDefaultNBT().merge(new TagCompound(new TagString("id", ((CustomEntity) reference).getDefaultType().toString())));
                                } else {
                                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID")));
                                    throw new EntryParsingException();
                                }
                                var auxNBT = rawPassenger.find("PASSENGER_NBT.NBT_COMPOUND");
                                if(auxNBT != null) passengerCompound = passengerCompound.merge(NBTParser.parseCompound(auxNBT, file.getCompiler()));

                                passengersTag.add(passengerCompound);
                            }
                        }

                        entityDecl.setDefaultNBT(oldNBT.merge(new TagCompound(passengersTag)));
                        break;
                    }
                    case "INNER_FUNCTION": {
                        boolean ticking = entry.find("LITERAL_TICKING") != null;

                        String functionName = new TridentUtil.ResourceLocation(entry.find("INNER_FUNCTION_NAME").flatten(false)).body;

                        var innerFilePattern = entry.find("FILE_INNER");
                        String innerFilePathRaw = file.getPath().toString();
                        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()-".tdn".length());

                        TridentFile innerFile = new TridentFile(file.getCompiler(), Path.of(innerFilePathRaw).resolve(functionName + ".tdn"), innerFilePattern);
                        innerFile.resolveEntries();

                        if(ticking) {
                            Function entityTickFunction = file.getNamespace().functions.get("trident_entity_tick");
                            var tickTag = file.getCompiler().getModule().minecraft.tags.functionTags.create("tick");
                            var functionReference = new FunctionReference(entityTickFunction);
                            if (!tickTag.getValues().contains(functionReference))
                                tickTag.addValue(new FunctionReference(entityTickFunction));
                            entityTickFunction.append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), new ExecuteAsEntity(TypeArgumentParser.getSelectorForCustomEntity(entityDecl)), new ExecuteAtEntity(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)))));
                        }
                        break;
                    }
                    default:
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + entry.getName() + "'", entry));
                }
            }
        }

        entityDecl.endDeclaration();
    }
}
