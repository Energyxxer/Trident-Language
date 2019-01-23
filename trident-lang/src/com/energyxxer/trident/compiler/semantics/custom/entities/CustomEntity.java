package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.attributes.Attribute;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.constructs.selectors.TypeArgumentParser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.ALL_ENTITIES;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;
import static com.energyxxer.nbtmapper.tags.PathProtocol.ENTITY;

public class CustomEntity implements VariableTypeHandler<CustomEntity> {
    private final String id;
    private final Type defaultType;
    @NotNull
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

    @NotNull
    public TagCompound getDefaultNBT() {
        return defaultNBT;
    }

    public void mergeNBT(TagCompound newNBT) {
        this.defaultNBT = this.defaultNBT.merge(newNBT);
    }

    public void overrideNBT(TagCompound newNBT) {
        this.defaultNBT = getBaseNBT().merge(newNBT);
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


    @Override
    public Object getMember(CustomEntity object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("getSettingNBT")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                TagCompound nbt = new TagCompound(new TagString("id", ((CustomEntity) this).getDefaultType().toString()));
                nbt = ((CustomEntity) this).getDefaultNBT().merge(nbt);
                return nbt;
            };
        }
        else if(member.equals("getMatchingNBT")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> new TagCompound(new TagList("Tags", new TagString(idTag)));
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomEntity object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(CustomEntity object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }

    public static void defineEntity(TokenPattern<?> pattern, TridentFile file) {

        boolean global = pattern.find("LITERAL_LOCAL") == null;

        String entityName = pattern.find("ENTITY_NAME").flatten(false);
        Type defaultType = null;
        if(pattern.find("ENTITY_BASE.ENTITY_ID_TAGGED") != null) {
            defaultType = CommonParsers.parseEntityType(pattern.find("ENTITY_BASE.ENTITY_ID_TAGGED"), file);
        }

        CustomEntity entityDecl = null;
        if(!entityName.equals("default")) {
            if(defaultType == null || !defaultType.isStandalone()) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot create a non-default entity with this type", pattern.find("ENTITY_BASE"), file);
            }
            entityDecl = new CustomEntity(entityName, defaultType);
            SymbolTable table = global ? file.getCompiler().getSymbolStack().getGlobal() : file.getCompiler().getSymbolStack().peek();
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

                        TagCompound newNBT = NBTParser.parseCompound(entry.find("NBT_COMPOUND"), file);
                        if(newNBT != null) {
                            PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY);
                            NBTParser.analyzeTag(newNBT, context, entry.find("NBT_COMPOUND"), file);
                        }

                        entityDecl.mergeNBT(newNBT);
                        break;
                    }
                    case "DEFAULT_PASSENGERS": {
                        if(entityDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default passengers aren't allowed for default entities", entry));
                            break;
                        }

                        TagList passengersTag = new TagList("Passengers");

                        for(TokenPattern<?> rawPassenger : ((TokenList)entry.find("PASSENGER_LIST")).getContents()) {
                            if(rawPassenger.getName().equals("PASSENGER")) {

                                TagCompound passengerCompound;

                                Object reference = CommonParsers.parseEntityReference(rawPassenger.find("ENTITY_ID"), file);

                                if(reference instanceof Type) {
                                    passengerCompound = new TagCompound(new TagString("id", reference.toString()));
                                } else if(reference instanceof CustomEntity) {
                                    passengerCompound = ((CustomEntity) reference).getDefaultNBT().merge(new TagCompound(new TagString("id", ((CustomEntity) reference).getDefaultType().toString())));
                                } else {
                                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID"), file);
                                }
                                TokenPattern<?> auxNBT = rawPassenger.find("PASSENGER_NBT.NBT_COMPOUND");
                                if(auxNBT != null) {
                                    TagCompound tag = NBTParser.parseCompound(auxNBT, file);
                                    PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY);
                                    NBTParser.analyzeTag(tag, context, auxNBT, file);
                                    passengerCompound = passengerCompound.merge(tag);
                                }

                                passengersTag.add(passengerCompound);
                            }
                        }

                        entityDecl.mergeNBT(new TagCompound(passengersTag));
                        break;
                    }
                    case "DEFAULT_HEALTH": {
                        if(entityDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default health isn't allowed for default entities", entry));
                            break;
                        }

                        double health = CommonParsers.parseDouble(entry.find("HEALTH"), file);
                        if(health < 0) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Health must be non-negative", entry.find("HEALTH")));
                            break;
                        }

                        TagCompound healthNBT = new TagCompound();
                        healthNBT.add(new TagFloat("Health", (float)health));
                        healthNBT.add(new TagList("Attributes", new TagCompound(new TagString("Name", Attribute.MAX_HEALTH), new TagDouble("Base", health))));

                        entityDecl.mergeNBT(healthNBT);
                        break;
                    }
                    case "ENTITY_INNER_FUNCTION": {
                        boolean ticking = entry.find("LITERAL_TICKING") != null;

                        TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), file);

                        if(ticking) {
                            Entity selector = entityDecl != null ?
                                    TypeArgumentParser.getSelectorForCustomEntity(entityDecl) :
                                    defaultType != null ?
                                            new Selector(ALL_ENTITIES, new TypeArgument(defaultType)) :
                                            new Selector(ALL_ENTITIES);
                            file.getTickFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), new ExecuteAsEntity(selector), new ExecuteAtEntity(new Selector(SENDER))));
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

    @Override
    public String toString() {
        return "[Custom Entity: " + id + "]";
    }
}
