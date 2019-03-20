package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
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
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.commands.SummonParser;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.constructs.selectors.TypeArgumentParser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.ALL_ENTITIES;
import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;
import static com.energyxxer.nbtmapper.tags.PathProtocol.ENTITY;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class CustomEntity implements VariableTypeHandler<CustomEntity> {
    private final String id;
    @Nullable
    private final Type baseType;
    @NotNull
    private TagCompound defaultNBT;
    private CustomEntity superEntity = null;
    private String idTag;
    private boolean fullyDeclared = false;
    private HashMap<String, Symbol> members = new HashMap<>();

    public CustomEntity(String id, @Nullable Type baseType, ISymbolContext ctx) {
        this.id = id;
        this.baseType = baseType;
        this.idTag = "trident-" + (baseType == null ? "component" : "entity") + "." + ctx.getStaticParentFile().getNamespace().getName() + "." + id;
        this.defaultNBT = getBaseNBT();
    }

    public String getId() {
        return id;
    }

    @Nullable
    public Type getBaseType() {
        return baseType;
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
    public Object getMember(CustomEntity object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(members.containsKey(member)) {
            Symbol sym = members.get(member);
            return keepSymbol ? sym : sym.getValue();
        }
        switch (member) {
            case "getSettingNBT":
                return (VariableMethod) (params, patterns, pattern1, file1) -> {
                    TagCompound nbt = new TagCompound();
                    if (this.getBaseType() != null) {
                        nbt.add(new TagString("id", this.getBaseType().toString()));
                    }
                    nbt = this.getDefaultNBT().merge(nbt);
                    return nbt;
                };
            case "getMatchingNBT":
                return (VariableMethod) (params, patterns, pattern1, file1) -> new TagCompound(new TagList("Tags", new TagString(idTag)));
            case "idTag":
                return idTag;
            case "baseType":
                return baseType != null ? new TridentUtil.ResourceLocation(baseType.toString()) : null;
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomEntity object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        String indexStr = assertOfType(index, pattern, ctx, String.class);
        if(members.containsKey(indexStr)) {
            Symbol sym = members.get(indexStr);
            return keepSymbol ? sym : sym.getValue();
        } else if(keepSymbol) {
            Symbol sym;
            members.put(indexStr, sym = new Symbol(indexStr, Symbol.SymbolVisibility.LOCAL, null));
            return sym;
        } else return null;
    }

    @Override
    public <F> F cast(CustomEntity object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext file) {
        throw new ClassCastException();
    }

    public static void defineEntity(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);

        String entityName;
        Type defaultType = null;
        CustomEntity superEntity = null;

        TokenPattern<?> headerDeclaration = ((TokenStructure) pattern.find("ENTITY_DECLARATION_HEADER")).getContents();

        switch (headerDeclaration.getName()) {
            case "CONCRETE_ENTITY_DECLARATION": {
                entityName = headerDeclaration.find("ENTITY_NAME").flatten(false);

                if (headerDeclaration.find("ENTITY_BASE.ENTITY_ID_TAGGED") != null) {
                    Object referencedType = CommonParsers.parseEntityReference(headerDeclaration.find("ENTITY_BASE.ENTITY_ID_TAGGED"), ctx);
                    if (referencedType instanceof Type) {
                        defaultType = ((Type) referencedType);
                    } else if (referencedType instanceof CustomEntity) {
                        superEntity = ((CustomEntity) referencedType);
                        if (!superEntity.isComponent()) {
                            defaultType = superEntity.baseType;
                        }
                    } else {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", headerDeclaration, ctx);
                    }
                } else if(!entityName.equals("default")) {
                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "The wildcard entity base may only be used on default entities, found name '" + entityName + "'", headerDeclaration, ctx);
                }
                break;
            }
            case "ABSTRACT_ENTITY_DECLARATION": {
                entityName = headerDeclaration.find("ENTITY_NAME").flatten(false);
                break;
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + headerDeclaration.getName() + "'", headerDeclaration, ctx);
            }
        }

        ArrayList<CustomEntity> implemented = new ArrayList<>();
        TokenList rawComponentList = ((TokenList) pattern.find("IMPLEMENTED_COMPONENTS.COMPONENT_LIST"));
        if (rawComponentList != null) {
            for (TokenPattern<?> rawComponent : rawComponentList.searchByName("INTERPOLATION_VALUE")) {
                CustomEntity component = InterpolationManager.parse(rawComponent, ctx, CustomEntity.class);
                if (component.isComponent()) {
                    implemented.add(component);
                } else {
                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Expected an entity component here, instead got an entity", rawComponent, ctx);
                }
            }
        }

        CustomEntity entityDecl = null;
        if (!entityName.equals("default")) {
            if (defaultType != null && !defaultType.isStandalone()) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot create a non-default entity with this type: " + defaultType, pattern.find("ENTITY_DECLARATION_HEADER.ENTITY_BASE"), ctx);
            }
            entityDecl = new CustomEntity(entityName, defaultType, ctx);
            entityDecl.superEntity = superEntity;
            ctx.putInContextForVisibility(visibility, new Symbol(entityName, visibility, entityDecl));

            if (superEntity != null) {
                entityDecl.mergeNBT(superEntity.getDefaultNBT());
                entityDecl.members.putAll(superEntity.members);
            }
            for (CustomEntity component : implemented) {
                entityDecl.mergeNBT(component.getDefaultNBT());
                entityDecl.members.putAll(component.members);
            }
        } else {
            if (superEntity != null) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default entities may not inherit from custom entities", pattern.find("ENTITY_DECLARATION_HEADER.ENTITY_BASE.ENTITY_ID_TAGGED"), ctx);
            }
        }

        ctx = new SymbolContext(ctx);
        if (entityDecl != null) ctx.put(new Symbol("this", Symbol.SymbolVisibility.LOCAL, entityDecl));

        ExceptionCollector collector = new ExceptionCollector(ctx);
        collector.begin();

        TokenList bodyEntries = (TokenList) pattern.find("ENTITY_DECLARATION_BODY.ENTITY_BODY_ENTRIES");

        try {
            if (bodyEntries != null) {
                for (TokenPattern<?> rawEntry : bodyEntries.getContents()) {
                    try {
                        TokenPattern<?> entry = ((TokenStructure) rawEntry).getContents();
                        switch (entry.getName()) {
                            case "DEFAULT_NBT": {
                                if (entityDecl == null) {
                                    collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default entities", entry, ctx));
                                    break;
                                }

                                TagCompound newNBT = NBTParser.parseCompound(entry.find("NBT_COMPOUND"), ctx);
                                if (newNBT != null) {
                                    PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY);
                                    NBTParser.analyzeTag(newNBT, context, entry.find("NBT_COMPOUND"), ctx);
                                }

                                entityDecl.mergeNBT(newNBT);
                                break;
                            }
                            case "DEFAULT_PASSENGERS": {
                                if (entityDecl == null) {
                                    collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default passengers aren't allowed for default entities", entry, ctx));
                                    break;
                                }

                                TagList passengersTag = new TagList("Passengers");

                                for (TokenPattern<?> rawPassenger : ((TokenList) entry.find("PASSENGER_LIST")).getContents()) {
                                    if (rawPassenger.getName().equals("PASSENGER")) {

                                        TagCompound passengerCompound;

                                        SummonParser.SummonData passengerData = new SummonParser.SummonData(rawPassenger, ctx,
                                                rawPassenger.find("ENTITY_ID"),
                                                null,
                                                rawPassenger.find("PASSENGER_NBT.NBT_COMPOUND"),
                                                ((TokenList) rawPassenger.find("IMPLEMENTED_COMPONENTS.COMPONENT_LIST"))
                                        );

                                        passengerData.fillDefaults();

                                        passengerCompound = passengerData.nbt.merge(new TagCompound(new TagString("id", passengerData.type.toString())));

                                        passengersTag.add(passengerCompound);
                                    }
                                }

                                entityDecl.mergeNBT(new TagCompound(passengersTag));
                                break;
                            }
                            case "DEFAULT_HEALTH": {
                                if (entityDecl == null) {
                                    collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default health isn't allowed for default entities", entry, ctx));
                                    break;
                                }

                                double health = CommonParsers.parseDouble(entry.find("HEALTH"), ctx);
                                if (health < 0) {
                                    collector.log(new TridentException(TridentException.Source.COMMAND_ERROR, "Health must be non-negative", entry.find("HEALTH"), ctx));
                                    break;
                                }

                                TagCompound healthNBT = new TagCompound();
                                healthNBT.add(new TagFloat("Health", (float) health));
                                healthNBT.add(new TagList("Attributes", new TagCompound(new TagString("Name", Attribute.MAX_HEALTH), new TagDouble("Base", health))));

                                entityDecl.mergeNBT(healthNBT);
                                break;
                            }
                            case "DEFAULT_NAME": {
                                if (entityDecl == null) {
                                    collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default entities", entry, ctx));
                                    break;
                                }

                                entityDecl.defaultNBT = entityDecl.defaultNBT.merge(new TagCompound(new TagString("CustomName", TextParser.parseTextComponent(entry.find("TEXT_COMPONENT"), ctx).toString())));
                                break;
                            }
                            case "ENTITY_INNER_FUNCTION": {
                                TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), ctx,
                                        entityDecl != null ?
                                                ctx.getParent() instanceof TridentFile &&
                                                        ((TridentFile) ctx.getParent()).getPath().endsWith(entityDecl.id + ".tdn") ?
                                                        null :
                                                        entityDecl.id
                                                :
                                                defaultType != null ?
                                                        "default_" + defaultType.getName() :
                                                        null
                                );
                                TokenPattern<?> namePattern = entry.find("OPTIONAL_NAME_INNER_FUNCTION.INNER_FUNCTION_NAME.RESOURCE_LOCATION");
                                if (namePattern != null) {
                                    String name = namePattern.flatten(false);
                                    Symbol sym = new Symbol(name, Symbol.SymbolVisibility.LOCAL, innerFile.getResourceLocation());
                                    if (entityDecl != null) {
                                        entityDecl.members.put(name, sym);
                                    } else {
                                        ctx.put(sym);
                                    }
                                }

                                TokenPattern<?> functionModifier = entry.find("ENTITY_FUNCTION_MODIFIER");
                                if (functionModifier != null) {
                                    functionModifier = ((TokenStructure) functionModifier).getContents();
                                    switch (functionModifier.getName()) {
                                        case "TICKING_ENTITY_FUNCTION": {

                                            ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList(((TokenList) functionModifier.find("TICKING_MODIFIERS")), ctx, collector);

                                            Entity selector = entityDecl != null ?
                                                    TypeArgumentParser.getSelectorForCustomEntity(entityDecl) :
                                                    defaultType != null ?
                                                            new Selector(ALL_ENTITIES, new TypeArgument(defaultType)) :
                                                            new Selector(ALL_ENTITIES);

                                            modifiers.add(0, new ExecuteAsEntity(selector));
                                            modifiers.add(1, new ExecuteAtEntity(new Selector(SENDER)));

                                            ctx.getWritingFile().getTickFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), modifiers));
                                        }
                                    }
                                }
                                break;
                            }
                            case "ENTITY_FIELD": {
                                String fieldName = entry.find("FIELD_NAME").flatten(false);
                                Object value = InterpolationManager.parse(((TokenStructure) entry.find("FIELD_VALUE")).getContents(), ctx);
                                Symbol sym = new Symbol(fieldName, Symbol.SymbolVisibility.LOCAL, value);
                                if (entityDecl != null) {
                                    entityDecl.members.put(fieldName, sym);
                                } else {
                                    ctx.put(sym);
                                }
                            }
                            case "COMMENT": {
                                break;
                            }
                            default: {
                                collector.log(new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + entry.getName() + "'", entry, ctx));
                                break;
                            }
                        }
                    } catch (TridentException | TridentException.Grouped x) {
                        collector.log(x);
                    }
                }
            }
        } finally {
            collector.end();
            if (entityDecl != null) entityDecl.endDeclaration();
        }
    }

    public boolean isComponent() {
        return baseType == null;
    }

    @Override
    public String toString() {
        return "[Custom Entity: " + id + "]";
    }
}
