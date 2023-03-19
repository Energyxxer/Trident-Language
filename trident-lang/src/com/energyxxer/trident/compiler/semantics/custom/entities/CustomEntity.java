package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.TimeSpan;
import com.energyxxer.commodore.versioning.JavaEditionVersion;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.DataType;
import com.energyxxer.nbtmapper.tags.DataTypeQueryResponse;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTInspector;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.TypeAwareNBTMerger;
import com.energyxxer.trident.compiler.semantics.custom.special.TickingFunction;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.java.selector_arguments.TypeArgumentParser;
import com.energyxxer.trident.sets.trident.TridentLiteralSet;
import com.energyxxer.trident.sets.trident.instructions.VariableInstruction;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupSpecialFileManagerTask;
import com.energyxxer.trident.worker.tasks.SetupTypeMapTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import static com.energyxxer.nbtmapper.tags.PathProtocol.ENTITY;
import static com.energyxxer.trident.compiler.semantics.custom.TypeAwareNBTMerger.REPLACE;
import static com.energyxxer.trident.sets.trident.instructions.VariableInstruction.parseSymbolDeclaration;

public class CustomEntity implements TypeHandler<CustomEntity> {
    private final PrismarineTypeSystem typeSystem;
    private final boolean isStaticHandler;
    private final String id;
    @Nullable
    private final Type baseType;
    @NotNull
    private TagCompound defaultNBT;
    private String idTag;
    private boolean fullyDeclared = false;
    private HashMap<String, Symbol> members = new HashMap<>();

    //EMPTY OBJECT FOR STATIC HANDLER
    private CustomEntity(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
        id = null;
        baseType = null;
        defaultNBT = new TagCompound();

        isStaticHandler = true;
    }

    public CustomEntity(String id, @Nullable Type baseType, ISymbolContext ctx) {
        this.typeSystem = ctx.getTypeSystem();
        this.id = id;
        this.baseType = baseType;
        this.idTag = "trident-" + (baseType == null ? "component" : "entity") + "." + ((TridentFile) ctx.getStaticParentUnit()).getNamespace().getName() + "." + id;
        this.defaultNBT = getBaseNBT();

        isStaticHandler = false;
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

    public void mergeNBT(TagCompound newNBT, ISymbolContext ctx) {
        PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY).setProtocolMetadata(baseType);
        this.defaultNBT = ((TypeAwareNBTMerger) (path, cls) -> {
            DataTypeQueryResponse response = ctx.get(SetupTypeMapTask.INSTANCE).collectTypeInformation(path, context);
            if (!response.isEmpty()) {
                for (DataType type : new ArrayList<>(response.getPossibleTypes())) {
                    if (type.getFlags() != null && type.getFlags().hasFlag("fixed") && cls.equals(type.getCorrespondingTagType()))
                        return TypeAwareNBTMerger.REPLACE;
                }
            }
            return TypeAwareNBTMerger.MERGE;
        }).merge(this.defaultNBT, newNBT);
    }

    public void overrideNBT(TagCompound newNBT, ISymbolContext ctx) {
        this.defaultNBT = ((TypeAwareNBTMerger) (path, cls) -> REPLACE).merge(this.defaultNBT, newNBT);
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
        if(isStaticHandler) return ctx.getTypeSystem().getMetaTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
        if(members.containsKey(member)) {
            Symbol sym = members.get(member);
            return keepSymbol ? sym : sym.getValue(pattern, ctx);
        }
        switch (member) {
            case "getSettingNBT":
                return (PrimitivePrismarineFunction) (params, ctx1, thisObject) -> {
                    TagCompound nbt = new TagCompound();
                    if (this.getBaseType() != null) {
                        nbt.add(new TagString("id", this.getBaseType().toString()));
                    }
                    nbt = this.getDefaultNBT().merge(nbt);
                    return nbt;
                };
            case "getMatchingNBT":
                return (PrimitivePrismarineFunction) (params, ctx1, thisObject) -> new TagCompound(new TagList("Tags", new TagString(idTag)));
            case "idTag":
                return idTag;
            case "baseType":
                return baseType != null ? new ResourceLocation(baseType.toString()) : null;
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomEntity object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return ctx.getTypeSystem().getMetaTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        String indexStr = PrismarineTypeSystem.assertOfClass(index, pattern, ctx, String.class);
        if(members.containsKey(indexStr)) {
            Symbol sym = members.get(indexStr);
            return keepSymbol ? sym : sym.getValue(pattern, ctx);
        } else if(keepSymbol) {
            Symbol sym;
            members.put(indexStr, sym = new Symbol(indexStr, TridentSymbolVisibility.LOCAL, null));
            return sym;
        } else return null;
    }

    @Override
    public Object cast(CustomEntity object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(isStaticHandler) return ctx.getTypeSystem().getMetaTypeHandler().cast(object, targetType, pattern, ctx);
        throw new ClassCastException();
    }

    public static void defineEntity(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, SymbolVisibility.GLOBAL);

        String entityName;
        Type defaultType = null;
        CustomEntity superEntity = null;

        TokenPattern<?> headerDeclaration = ((TokenStructure) pattern.find("ENTITY_DECLARATION_HEADER")).getContents();

        switch (headerDeclaration.getName()) {
            case "CONCRETE_ENTITY_DECLARATION": {
                entityName = (String) headerDeclaration.findThenEvaluate("ENTITY_NAME.IDENTIFIER_A", null, ctx, null);
                if(entityName == null) { //Is not an IDENTIFIER_A
                    entityName = headerDeclaration.find("ENTITY_NAME").flatten(false);
                }

                if (headerDeclaration.find("ENTITY_BASE.TRIDENT_ENTITY_ID_TAGGED") != null) {
                    Object referencedType = headerDeclaration.find("ENTITY_BASE.TRIDENT_ENTITY_ID_TAGGED").evaluate(ctx, null);
                    if (referencedType instanceof Type) {
                        defaultType = ((Type) referencedType);

                        if(!entityName.equals("default") && defaultType.getProperty("spawnable").equals("false")) {
                            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "This entity type is not summonable", headerDeclaration.find("ENTITY_BASE.ENTITY_ID_TAGGED"), ctx);
                        }
                    } else if (referencedType instanceof CustomEntity) {
                        superEntity = ((CustomEntity) referencedType);
                        if (!superEntity.isComponent()) {
                            defaultType = superEntity.baseType;
                        }
                    } else {
                        throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", headerDeclaration, ctx);
                    }
                } else if(!entityName.equals("default")) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "The wildcard entity base may only be used on default entities, found name '" + entityName + "'", headerDeclaration, ctx);
                }
                break;
            }
            case "ABSTRACT_ENTITY_DECLARATION": {
                entityName = (String) headerDeclaration.findThenEvaluate("ENTITY_NAME.IDENTIFIER_A", null, ctx, null);
                if(entityName == null) { //Is not an IDENTIFIER_A
                    entityName = headerDeclaration.find("ENTITY_NAME").flatten(false);
                }
                break;
            }
            default: {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + headerDeclaration.getName() + "'", headerDeclaration, ctx);
            }
        }

        ArrayList<CustomEntity> implemented = new ArrayList<>();
        TokenPattern<?> rawComponentList = pattern.find("IMPLEMENTED_COMPONENTS.COMPONENT_LIST");
        if (rawComponentList != null) {
            if(!entityName.equals("default")) {
                rawComponentList.evaluate(ctx, new Object[] {implemented});
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Default entities may not implement components", rawComponentList, ctx);
            }
        }



        final CustomEntity entityDecl;
        if (!entityName.equals("default")) {
            if (defaultType != null && !defaultType.isStandalone()) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot create a non-default entity with this type: " + defaultType, pattern.find("ENTITY_DECLARATION_HEADER.ENTITY_BASE"), ctx);
            }
            entityDecl = new CustomEntity(entityName, defaultType, ctx);
            ctx.putInContextForVisibility(visibility, new Symbol(entityName, visibility, entityDecl));

            if (superEntity != null) {
                entityDecl.mergeNBT(superEntity.getDefaultNBT(), ctx);
                entityDecl.members.putAll(superEntity.members);
            }
            for (CustomEntity component : implemented) {
                entityDecl.mergeNBT(component.getDefaultNBT(), ctx);
                entityDecl.members.putAll(component.members);
            }
        } else {
            if (superEntity != null) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Default entities may not inherit from custom entities", pattern.find("ENTITY_DECLARATION_HEADER.ENTITY_BASE.ENTITY_ID_TAGGED"), ctx);
            }
            entityDecl = null;
        }

        ctx = new SymbolContext(ctx);
        final ISymbolContext finalCtx = ctx;
        final Type finalDefaultType = defaultType;
        if (entityDecl != null) ctx.put(new Symbol("this", TridentSymbolVisibility.LOCAL, entityDecl));

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
                                    collector.log(new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default entities", entry, ctx));
                                    break;
                                }

                                TagCompound newNBT = (TagCompound) entry.find("NBT_COMPOUND").evaluate(ctx, null);
                                if (newNBT != null) {
                                    PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY).setProtocolMetadata(defaultType);
                                    NBTInspector.inspectTag(newNBT, context, entry.find("NBT_COMPOUND"), ctx);
                                }

                                entityDecl.mergeNBT(newNBT, ctx);
                                break;
                            }
                            case "DEFAULT_PASSENGERS": {
                                if (entityDecl == null) {
                                    collector.log(new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Default passengers aren't allowed for default entities", entry, ctx));
                                    break;
                                }

                                TagList passengersTag = new TagList("Passengers");

                                for (TokenPattern<?> rawPassenger : ((TokenList) entry.find("PASSENGER_LIST")).getContents()) {
                                    if (rawPassenger.getName().equals("NEW_ENTITY_LITERAL")) {

                                        TagCompound passengerCompound;

                                        TridentLiteralSet.SummonData passengerData = (TridentLiteralSet.SummonData) rawPassenger.evaluate(ctx, null);

                                        passengerData.fillDefaults();

                                        passengerCompound = passengerData.nbt.merge(new TagCompound(new TagString("id", passengerData.type.toString())));

                                        passengersTag.add(passengerCompound);
                                    }
                                }

                                entityDecl.mergeNBT(new TagCompound(passengersTag), ctx);
                                break;
                            }
                            case "DEFAULT_HEALTH": {
                                if (entityDecl == null) {
                                    collector.log(new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Default health isn't allowed for default entities", entry, ctx));
                                    break;
                                }

                                double health = (double) entry.find("HEALTH").evaluate(ctx, null);
                                if (health < 0) {
                                    collector.log(new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Health must be non-negative", entry.find("HEALTH"), ctx));
                                    break;
                                }

                                String attributeName;

                                if(ctx.get(SetupModuleTask.INSTANCE).getSettingsManager().getTargetVersion().compare(new JavaEditionVersion(1, 16, 0)) >= 0) {
                                    attributeName = "minecraft:generic.max_health";
                                } else {
                                    attributeName = "generic.maxHealth";
                                }

                                TagCompound healthNBT = new TagCompound();
                                healthNBT.add(new TagFloat("Health", (float) health));
                                healthNBT.add(new TagList("Attributes", new TagCompound(new TagString("Name", attributeName), new TagDouble("Base", health))));

                                entityDecl.mergeNBT(healthNBT, ctx);
                                break;
                            }
                            case "DEFAULT_NAME": {
                                if (entityDecl == null) {
                                    collector.log(new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default entities", entry, ctx));
                                    break;
                                }

                                entityDecl.defaultNBT = entityDecl.defaultNBT.merge(new TagCompound(new TagString("CustomName", entry.find("TEXT_COMPONENT").evaluate(ctx, null).toString())));
                                break;
                            }
                            case "ENTITY_INNER_FUNCTION": {
                                TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), ctx,
                                        entityDecl != null ?
                                                ctx.getParent() instanceof TridentFile &&
                                                        ctx.getParent().getPathFromRoot().endsWith(entityDecl.id + Trident.FUNCTION_EXTENSION) ?
                                                        null :
                                                        entityDecl.id
                                                :
                                                defaultType != null ?
                                                        "default_" + defaultType.getName() :
                                                        null
                                        , false);
                                TokenPattern<?> namePattern = entry.find("OPTIONAL_NAME_INNER_FUNCTION.INNER_FUNCTION_NAME.RESOURCE_LOCATION");
                                if (namePattern != null) {
                                    String name = namePattern.flatten(false);
                                    Symbol sym = new Symbol(name, TridentSymbolVisibility.LOCAL, innerFile.getResourceLocation());
                                    if (entityDecl != null) {
                                        entityDecl.members.put(name, sym);
                                    } else {
                                        finalCtx.put(sym);
                                    }
                                }

                                ((TridentFile) ctx.getStaticParentUnit()).schedulePostResolutionAction(() -> {
                                    innerFile.resolveEntries();

                                    TokenPattern<?> functionModifier = entry.find("ENTITY_FUNCTION_MODIFIER");
                                    if (functionModifier != null) {
                                        functionModifier = ((TokenStructure) functionModifier).getContents();
                                        switch (functionModifier.getName()) {
                                            case "TICKING_ENTITY_FUNCTION": {
                                                ArrayList<ExecuteModifier> modifiers = (ArrayList<ExecuteModifier>) functionModifier.findThenEvaluateLazyDefault("TICKING_MODIFIERS.MODIFIER_LIST", ArrayList::new, finalCtx, null);

                                                int interval = 1;
                                                if(functionModifier.find("TICKING_INTERVAL") != null) {
                                                    interval = ((TimeSpan)functionModifier.find("TICKING_INTERVAL").evaluate(finalCtx, null)).getTicks();
                                                    if(interval <= 0) {
                                                        throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Ticking interval must be greater than zero", functionModifier.find("TICKING_INTERVAL"), finalCtx);
                                                    }
                                                }

                                                SelectorArgument[] filter = new SelectorArgument[0];
                                                if(entityDecl != null) {
                                                    filter = TypeArgumentParser.getFilterForCustomEntity(entityDecl);
                                                } else if(finalDefaultType != null) {
                                                    filter = new SelectorArgument[] {new TypeArgument(finalDefaultType)};
                                                }

                                                finalCtx.get(SetupSpecialFileManagerTask.INSTANCE).getTickingFunction(interval).addEntityTickEvent(
                                                        new TickingFunction.EntityEvent(
                                                                filter,
                                                                new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), modifiers)
                                                        )
                                                );
                                            }
                                        }
                                    }
                                });
                                break;
                            }
                            case "ENTITY_EVENT_IMPLEMENTATION": {
                                TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), ctx,
                                        entityDecl != null ?
                                                ctx.getParent() instanceof TridentFile &&
                                                        ctx.getParent().getPathFromRoot().endsWith(entityDecl.id + Trident.FUNCTION_EXTENSION) ?
                                                        null :
                                                        entityDecl.id
                                                :
                                                defaultType != null ?
                                                        "default_" + defaultType.getName() :
                                                        null
                                        , false);
                                TokenPattern<?> namePattern = entry.find("OPTIONAL_NAME_INNER_FUNCTION.INNER_FUNCTION_NAME.RESOURCE_LOCATION");
                                if (namePattern != null) {
                                    String name = namePattern.flatten(false);
                                    Symbol sym = new Symbol(name, TridentSymbolVisibility.LOCAL, innerFile.getResourceLocation());
                                    if (entityDecl != null) {
                                        entityDecl.members.put(name, sym);
                                    } else {
                                        finalCtx.put(sym);
                                    }
                                }

                                EntityEvent event = (EntityEvent) entry.find("EVENT_NAME").evaluate(ctx, null);

                                ((TridentFile) ctx.getStaticParentUnit()).schedulePostResolutionAction(innerFile::resolveEntries);

                                SelectorArgument[] filter = new SelectorArgument[0];
                                if(entityDecl != null) {
                                    filter = TypeArgumentParser.getFilterForCustomEntity(entityDecl);
                                } else if(finalDefaultType != null) {
                                    filter = new SelectorArgument[] {new TypeArgument(finalDefaultType)};
                                }

                                ArrayList<ExecuteModifier> modifiers = (ArrayList<ExecuteModifier>) entry.find("EVENT_MODIFIERS").evaluate(ctx, null);
                                modifiers.add(0, new ExecuteConditionEntity(ExecuteCondition.ConditionType.IF, new Selector(Selector.BaseSelector.SENDER, filter)));

                                event.getFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), modifiers));
                                break;
                            }
                            case "ENTITY_FIELD": {
                                VariableInstruction.SymbolDeclaration decl = parseSymbolDeclaration(entry, ctx);

                                if (entityDecl != null) {
                                    entityDecl.members.put(decl.getName(), decl.getSupplier().get());
                                } else {
                                    ctx.put(decl.getSupplier().get());
                                }
                                break;
                            }
                            case "ENTITY_EVAL": {
                                ((TokenStructure) entry.find("INTERPOLATION_VALUE")).getContents().evaluate(ctx, null);
                                break;
                            }
                            case "COMMENT": {
                                break;
                            }
                            default: {
                                collector.log(new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + entry.getName() + "'", entry, ctx));
                                break;
                            }
                        }
                    } catch (PrismarineException | PrismarineException.Grouped x) {
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

    @Override
    public Class<CustomEntity> getHandledClass() {
        return CustomEntity.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "custom_entity";
    }



    public static CustomEntity createStaticHandler(PrismarineTypeSystem typeSystem) {
        return new CustomEntity(typeSystem);
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public boolean isStaticHandler() {
        return isStaticHandler;
    }
}
