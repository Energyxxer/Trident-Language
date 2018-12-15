package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.selectors.TypeArgumentParser;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

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









    public static void defineEntity(TokenPattern<?> pattern, TridentFile file) {

        boolean global = pattern.find("LITERAL_LOCAL") == null;

        String entityName = pattern.find("ENTITY_NAME").flatten(false);
        Type defaultType = null;
        if(pattern.find("ENTITY_BASE.ENTITY_ID_TAGGED") != null) {
            defaultType = CommonParsers.parseEntityType(pattern.find("ENTITY_BASE.ENTITY_ID_TAGGED"), file.getCompiler());
        }

        CustomEntity entityDecl = null;
        if(!entityName.equals("default")) {
            if(defaultType == null || !defaultType.isStandalone()) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Cannot create a non-default entity with this type", pattern.find("ENTITY_BASE")));
                throw new EntryParsingException();
            }
            entityDecl = new CustomEntity(entityName, defaultType);
            SymbolTable table = global ? file.getCompiler().getStack().getGlobal() : file.getCompiler().getStack().peek();
            table.put(new Symbol(entityName, Symbol.SymbolAccess.GLOBAL, entityDecl));
        }

        TokenList bodyEntries = (TokenList) pattern.find("ENTITY_DECLARATION_BODY.ENTITY_BODY_ENTRIES");

        if(bodyEntries != null) {
            for(TokenPattern<?> rawEntry : bodyEntries.getContents()) {
                TokenPattern<?> entry = ((TokenStructure) rawEntry).getContents();
                switch(entry.getName()) {
                    case "DEFAULT_NBT": {
                        if(entityDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default NBT isn't allowed for default entities", entry));
                            break;
                        }
                        entityDecl.setDefaultNBT(NBTParser.parseCompound(entry.find("NBT_COMPOUND"), file.getCompiler()));
                        break;
                    }
                    case "DEFAULT_PASSENGERS": {
                        if(entityDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default passengers aren't allowed for default entities", entry));
                            break;
                        }
                        TagCompound oldNBT = entityDecl.getDefaultNBT();

                        if(oldNBT == null) oldNBT = new TagCompound();

                        TagList passengersTag = new TagList("Passengers");

                        for(TokenPattern<?> rawPassenger : ((TokenList)entry.find("PASSENGER_LIST")).getContents()) {
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
                                TokenPattern<?> auxNBT = rawPassenger.find("PASSENGER_NBT.NBT_COMPOUND");
                                if(auxNBT != null) passengerCompound = passengerCompound.merge(NBTParser.parseCompound(auxNBT, file.getCompiler()));

                                passengersTag.add(passengerCompound);
                            }
                        }

                        entityDecl.setDefaultNBT(oldNBT.merge(new TagCompound(passengersTag)));
                        break;
                    }
                    case "ENTITY_INNER_FUNCTION": {
                        boolean ticking = entry.find("LITERAL_TICKING") != null;

                        TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), file);

                        if(ticking) {
                            Entity selector = entityDecl != null ?
                                    TypeArgumentParser.getSelectorForCustomEntity(entityDecl) :
                                    defaultType != null ?
                                            new GenericEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES, new TypeArgument(defaultType))) :
                                            new GenericEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES));
                            file.getTickFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), new ExecuteAsEntity(selector), new ExecuteAtEntity(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)))));
                        }
                        break;
                    }
                    default:
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + entry.getName() + "'", entry));
                }
            }
        }

        if(entityDecl != null) entityDecl.endDeclaration();
    }
}
