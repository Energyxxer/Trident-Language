package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.selectors.TypeArgumentParser;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.nio.file.Path;

public class CustomEntity {
    private final String id;
    private final Type defaultType;
    private TagCompound defaultNBT;
    private String idTag;
    private boolean fullyDeclared = false;

    public CustomEntity(String id, Type defaultType) {
        this.id = id;
        this.defaultType = defaultType;
        this.idTag = "trident-entity." + id.replace(':', '.').replace('/','.');
        this.defaultNBT = getBaseNBT();
    }

    public String getId() {
        return id;
    }

    public Type getDefaultType() {
        return defaultType;
    }

    public TagCompound getDefaultNBT() {
        return defaultNBT;
    }

    public void setDefaultNBT(TagCompound defaultNBT) {
        this.defaultNBT = defaultNBT.merge(getBaseNBT());
    }

    public String getIdTag() {
        return idTag;
    }

    private TagCompound getBaseNBT() {
        return new TagCompound(new TagList("Tags", new TagString(idTag)));
    }

    public boolean isFullyDeclared() {
        return fullyDeclared;
    }

    public void endDeclaration() {
        fullyDeclared = true;
    }









    public static void registerEntity(TokenPattern<?> pattern, TridentFile file) {
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
                            file.getTickFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), new ExecuteAsEntity(TypeArgumentParser.getSelectorForCustomEntity(entityDecl)), new ExecuteAtEntity(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)))));
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
