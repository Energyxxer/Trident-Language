package com.energyxxer.trident.sets.trident;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.summon.SummonCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTInspector;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.util.TridentTempFindABetterHome;
import com.energyxxer.trident.sets.DataStructureLiteralSet;
import com.energyxxer.trident.sets.MinecraftLiteralSet;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;
import com.energyxxer.trident.sets.java.commands.VerbatimCommandDefinition;

import java.util.ArrayList;
import java.util.function.Consumer;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.ARROW;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.COMPILER_OPERATOR;

public class TridentLiteralSet extends PatternProviderSet { //pointers, type_definitions

    public TridentLiteralSet() {
        super(null);
        importUnit(VerbatimCommandDefinition.class);
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure, PrismarineProjectWorker worker) {

        final Consumer<TokenPattern.SimplificationDomain> wrapperSimplification = (d) -> {
            d.pattern = ((TokenGroup) d.pattern).getContents()[2];
            d.data = null;
        };

        TokenPatternMatch scale = group(matchItem(COMPILER_OPERATOR, "*"), TridentProductions.real(productions)).setSimplificationFunctionContentIndex(1).setOptional().setName("SCALE");
        TokenPatternMatch explicitType = group(TridentProductions.brace("("), productions.getOrCreateStructure("NUMERIC_NBT_TYPE"), TridentProductions.brace(")")).setSimplificationFunctionContentIndex(1).setOptional().setName("EXPLICIT_TYPE");

        TokenGroupMatch scoreHead = (TokenGroupMatch) group(ofType(ARROW), productions.getOrCreateStructure("OBJECTIVE_NAME")).setName("SCORE_POINTER_HEAD").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
            PointerObject pointer = (PointerObject) d[0];

            String objectiveName = (String) ((TokenGroup) p).getContents()[1].evaluate(ctx, new Object[] {String.class});
            pointer.setMember(objectiveName);

            return null;
        });
        TokenGroupMatch nbtHead = (TokenGroupMatch) group(TridentProductions.dot(), productions.getOrCreateStructure("NBT_PATH"), scale, explicitType).setName("NBT_POINTER_HEAD").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
            PointerObject pointer = (PointerObject) d[0];

            NBTPath path = (NBTPath) p.find("NBT_PATH").evaluate(ctx, null);

            if(pointer.getTarget() instanceof PointerObject) {
                PointerObject basePointer = (PointerObject) pointer.getTarget();
                if(basePointer.getMember() instanceof NBTPath) {
                    NBTPath start = (NBTPath) basePointer.getMember();
                    NBTPath end = path;
                    ArrayList<NBTPathNode> nodes = new ArrayList<>();
                    do {
                        nodes.add(start.getNode());
                        start = start.getNext();
                    } while(start != null);
                    do {
                        nodes.add(end.getNode());
                        end = end.getNext();
                    } while(end != null);
                    NBTPath merged = new NBTPath(nodes.toArray(new NBTPathNode[0]));

                    pointer.setTarget(basePointer.getTarget());
                    path = merged;
                } else {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot concatenate a NBT path to a non-NBT pointer. Found member of type " + ctx.getTypeSystem().getTypeIdentifierForObject(basePointer.getMember()), p, ctx);
                }
            }
            pointer.setMember(path);
            pointer.setScale((double) p.findThenEvaluate("SCALE", 1.0, ctx, null));
            pointer.setNumericType((NumericNBTType) p.findThenEvaluate("EXPLICIT_TYPE", null, ctx, null));

            return null;
        });
        TokenGroupMatch storageHead = (TokenGroupMatch) group(TridentProductions.tilde(), productions.getOrCreateStructure("NBT_PATH"), scale, explicitType).setName("STORAGE_POINTER_HEAD").setEvaluator(nbtHead.getEvaluator());

        TokenStructureMatch anyHead = choice(scoreHead, nbtHead, storageHead).setName("POINTER_HEAD");

        TokenGroupMatch varPointer = (TokenGroupMatch) group(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), wrapper(anyHead).setOptional().setName("POINTER_HEAD_WRAPPER")).setName("VARIABLE_POINTER")
                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    TokenPattern<?> targetPattern = ((TokenGroup) p).getContents()[0];

                    Object target = targetPattern.evaluate(ctx, null);

                    if(p.find("POINTER_HEAD_WRAPPER") != null) {
                        target = PrismarineTypeSystem.assertOfClass(target, targetPattern, ctx, Entity.class, CoordinateSet.class, ResourceLocation.class, PointerObject.class);

                        PointerObject pointer = new PointerObject(ctx.getTypeSystem(), target, null);

                        p.find("POINTER_HEAD_WRAPPER").evaluate(ctx, new Object[] {pointer});
                        return pointer.validate(p, ctx);
                    } else {
                        return PrismarineTypeSystem.assertOfClass(target, targetPattern, ctx, PointerObject.class).validate(p, ctx);
                    }
                });
        TokenGroupMatch entityPointer = (TokenGroupMatch) group(productions.getOrCreateStructure("LIMITED_ENTITY"), anyHead).setName("ENTITY_POINTER")
                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    Object target = p.find("ENTITY").evaluate(ctx, null);

                    PointerObject pointer = new PointerObject(ctx.getTypeSystem(), target, null);

                    p.find("POINTER_HEAD").evaluate(ctx, new Object[] {pointer});

                    return pointer.validate(p, ctx);
                });
        TokenGroupMatch blockPointer = (TokenGroupMatch) group(TridentProductions.brace("("), productions.getOrCreateStructure("COORDINATE_SET"), TridentProductions.brace(")"), nbtHead).setName("BLOCK_POINTER").setName("ENTITY_POINTER")
                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    Object target = p.find("COORDINATE_SET").evaluate(ctx, null);

                    PointerObject pointer = new PointerObject(ctx.getTypeSystem(), target, null);

                    p.find("NBT_POINTER_HEAD").evaluate(ctx, new Object[] {pointer});

                    return pointer.validate(p, ctx);
                });
        TokenGroupMatch storagePointer = (TokenGroupMatch) group(TridentProductions.resourceLocationFixer, productions.getOrCreateStructure("RESOURCE_LOCATION"), storageHead).setName("STORAGE_POINTER")
                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    Object target = p.find("RESOURCE_LOCATION").evaluate(ctx, null);

                    PointerObject pointer = new PointerObject(ctx.getTypeSystem(), target, null);

                    p.find("STORAGE_POINTER_HEAD").evaluate(ctx, new Object[] {pointer});

                    return pointer.validate(p, ctx);
                });

        productions.getOrCreateStructure("POINTER")
                .add(entityPointer)
                .add(varPointer)
                .add(blockPointer)
                .add(storagePointer)
                .addTags("cspn:Pointer", SuggestionTags.ENABLED);

        productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE")
                .add(
                        group(literal("pointer").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("POINTER"), TridentProductions.brace(">")).setName("WRAPPED_POINTER").addTags("primitive:pointer").setSimplificationFunction(wrapperSimplification)
                )
                .add(
                        group(literal("type_definition").setName("VALUE_WRAPPER_KEY"), TridentProductions.brace("<"), productions.getOrCreateStructure("INTERPOLATION_TYPE"), TridentProductions.brace(">")).setName("WRAPPED_TYPE").addTags("primitive:type_definition").setSimplificationFunction(wrapperSimplification)
                )
                .add(
                        group(
                                literal("function").setName("VALUE_WRAPPER_KEY"),
                                choice(
                                        group(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            TridentFile innerFile = TridentFile.createInnerFile(((TokenGroup)p).getContents()[0], null);
                                            return innerFile.getResourceLocation();
                                        }),
                                        group(productions.getOrCreateStructure("DYNAMIC_FUNCTION")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            PrimitivePrismarineFunction function = new PrismarineFunction(DataStructureLiteralSet.nextFunctionName, TridentTempFindABetterHome.parseDynamicFunction(((TokenGroup) p).getContents()[0], ctx), ctx);
                                            if(DataStructureLiteralSet.nextThis != null) {
                                                function = new PrismarineFunction.FixedThisFunction(function, DataStructureLiteralSet.nextThis);
                                            }
                                            return function;
                                        })
                                ).setName("NEW_FUNCTION_SPLIT").setGreedy(true)
                        ).setName("NEW_FUNCTION").setSimplificationFunctionContentIndex(1)
                ).setGreedy(true);


        TokenPatternMatch pointerAsScore = group(productions.getOrCreateStructure("POINTER")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
            PointerObject pointer = (PointerObject) ((TokenGroup) p).getContents()[0].evaluate(ctx, null);
            if (!(pointer.getMember() instanceof String)) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected score pointer, instead got NBT pointer", p, ctx);
            }
            return new LocalScore((Entity) pointer.getTarget(), MinecraftLiteralSet.parseObjective((String) pointer.getMember(), p, ctx));
        });

        TokenPatternMatch dereferenceAsScore = group(
                literal("deref"),
                TridentProductions.sameLine(),
                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, PointerObject.class).setName("POINTER_DEREFERENCE")
        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
            PointerObject pointer = (PointerObject) p.find("POINTER_DEREFERENCE").evaluate(ctx, null);
            if (!(pointer.getMember() instanceof String)) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected score pointer, instead got NBT pointer", p, ctx);
            }
            return new LocalScore((Entity) pointer.getTarget(), MinecraftLiteralSet.parseObjective((String) pointer.getMember(), p, ctx));
        });

        productions.getOrCreateStructure("SCORE")
                .add(pointerAsScore)
                .add(dereferenceAsScore)
                .addTags("cspn:Score");

        productions.getOrCreateStructure("SCORE_OPTIONAL_OBJECTIVE")
                .add(pointerAsScore)
                .add(dereferenceAsScore)
                .addTags("cspn:Score");


        PostValidationPatternEvaluator<ISymbolContext> entityReferenceEvaluator = (value, p, ctx, d) -> {
            if(value instanceof CustomEntity) return value;
            if(value instanceof TagCompound) {
                NBTTag idTag = ((TagCompound) value).get("id");
                if(idTag == null) {
                    throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "'id' string not found in entity root compound", p, ctx);
                } else if(!(idTag instanceof TagString)) {
                    throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "'id' tag in entity root compound is not of type TAG_String", p, ctx);
                }
                return value;
            }
            return MinecraftLiteralSet.parseType(value, p, ctx, EntityType.CATEGORY, d != null && d.length > 0 && (boolean) d[0]);
        };

        productions.getOrCreateStructure("COMPONENT_LIST_BRACELESS").add(
                list(
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_VALUE"),
                                d -> null,
                                (Object value, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    ArrayList<CustomEntity> finalList = (ArrayList<CustomEntity>) d[0];

                                    collectComponent(value, finalList, p, ctx);
                                    return null;
                                }, false, CustomEntity.class, ListObject.class),
                        TridentProductions.comma()
                ).setName("COMPONENT_LIST").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    for(TokenPattern<?> inner : ((TokenList) p).getContentsExcludingSeparators()) {
                        inner.evaluate(ctx, d);
                    }
                    return null;
                })
        );

        productions.getOrCreateStructure("COMPONENT_LIST").add(
                group(
                        TridentProductions.brace("["),
                        wrapperOptional(productions.getOrCreateStructure("COMPONENT_LIST_BRACELESS")).setName("COMPONENT_LIST_BRACELESS"),
                        TridentProductions.brace("]")
                ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    ArrayList<CustomEntity> finalList = new ArrayList<>();
                    p.findThenEvaluate("COMPONENT_LIST_BRACELESS", null, ctx, new Object[] {finalList});
                    return finalList;
                })
        );

        productions.getOrCreateStructure("TRIDENT_ENTITY_ID_NBT")
                .add(
                        productions.getOrCreateStructure("ENTITY_ID")
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> null,
                                entityReferenceEvaluator,
                                false,
                                ResourceLocation.class, String.class, CustomEntity.class, TagCompound.class
                        )
                );

        productions.getOrCreateStructure("TRIDENT_ENTITY_ID")
                .add(
                        productions.getOrCreateStructure("ENTITY_ID")
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> null,
                                entityReferenceEvaluator,
                                false,
                                ResourceLocation.class, CustomEntity.class
                        )
                );

        productions.getOrCreateStructure("TRIDENT_ENTITY_ID_TAGGED")
                .add(
                        productions.getOrCreateStructure("ENTITY_ID_TAGGED")
                )
                .add(
                        group(PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> null,
                                entityReferenceEvaluator,
                                false,
                                ResourceLocation.class, CustomEntity.class
                        ))
                                .setSimplificationFunction(d -> {
                                    d.data = null;
                                    d.pattern = ((TokenGroup)d.pattern).getContents()[0];
                                })
                );

        productions.getOrCreateStructure("NEW_ENTITY_LITERAL")
                .add(
                        group(
                                TridentProductions.resourceLocationFixer,
                                productions.getOrCreateStructure("TRIDENT_ENTITY_ID_NBT"),
                                optional(
                                        TridentProductions.glue(),
                                        productions.getOrCreateStructure("COMPONENT_LIST")
                                ).setSimplificationFunctionContentIndex(1).setName("IMPLEMENTED_COMPONENTS"),
                                optional(
                                        TridentProductions.glue(),
                                        productions.getOrCreateStructure("NBT_COMPOUND")
                                ).setSimplificationFunctionContentIndex(1).setName("NEW_ENTITY_NBT")
                        ).setEvaluator(TridentLiteralSet::parseNewEntityLiteral)
                ).addTags("cspn:New Entity");



        productions.getOrCreateStructure("INTERPOLATION_BLOCK")
                .add(
                        group(
                                TridentProductions.symbol("$").setName("INTERPOLATION_HEADER").addTags(SuggestionTags.DISABLED),
                                TridentProductions.glue().addTags(SuggestionTags.ENABLED, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE).addTags("cspn:Variable"),
                                choice(TridentProductions.identifierX(), literal("this")).setName("VARIABLE_NAME").setEvaluator(ValueAccessExpressionSet::parseVariable).addProcessor((p, l) -> {
                                        if(l.getSummaryModule() != null) {
                                            ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                        }
                                }).addProcessor(ValueAccessExpressionSet.verifySymbol)
                        ).setName("VARIABLE").setSimplificationFunction(d -> {
                            d.pattern = d.pattern.find("VARIABLE_NAME");
                            d.data = null;
                        })
                )
                .add(
                        group(
                                TridentProductions.symbol("$").setName("INTERPOLATION_HEADER").addTags(SuggestionTags.DISABLED).addProcessor(startClosure),
                                TridentProductions.glue(),
                                TridentProductions.brace("{").setName("INTERPOLATION_BRACE").addTags(SuggestionTags.DISABLED),
                                productions.getOrCreateStructure("INTERPOLATION_VALUE"),
                                TridentProductions.brace("}").setName("INTERPOLATION_BRACE").addTags(SuggestionTags.DISABLED).addProcessor(endComplexValue)
                        ).addFailProcessor((p, l) -> {
                            if(((TokenGroup) p).getContents().length > 1) {
                                endComplexValue.accept(p, l);
                            }
                        }).setName("INTERPOLATION_WRAPPER").setSimplificationFunction(d -> {
                            d.pattern = d.pattern.find("INTERPOLATION_VALUE");
                            d.data = null;
                        })
                );
    }

    public static SummonData parseNewEntityLiteral(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        TagCompound nbt = (TagCompound) pattern.findThenEvaluate("NEW_ENTITY_NBT", null, ctx, null);
        Type type;
        Object reference = pattern.find("TRIDENT_ENTITY_ID_NBT").evaluate(ctx, null);

        if(reference instanceof Type) {
            type = (Type) reference;
        } else if(reference instanceof CustomEntity) {
            CustomEntity ce = (CustomEntity) reference;
            type = ce.getBaseType();

            if(type == null) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot summon an entity component: " + ce.getId(), pattern.find("ENTITY_ID"), ctx);
            }

            if(nbt == null) nbt = new TagCompound();
            try {
                nbt = ce.getDefaultNBT().merge(nbt);
            } catch(CommodoreException x) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Error while merging given NBT with custom entity's NBT: " + x.getMessage(), pattern, ctx);
            }
        } else if(reference instanceof TagCompound) {
            reference = ((TagCompound) reference).clone();
            type = MinecraftLiteralSet.parseType(((TagString) ((TagCompound) reference).get("id")).getValue(), pattern, ctx, EntityType.CATEGORY, false);
            ((TagCompound) reference).remove("id");
            if(nbt != null) {
                try {
                    nbt = ((TagCompound) reference).merge(nbt);
                } catch(CommodoreException x) {
                    throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Error while merging given NBT with custom entity's NBT: " + x.getMessage(), pattern, ctx);
                }
            } else {
                nbt = ((TagCompound) reference);
            }
            if(nbt.isEmpty()) {
                nbt = null;
            }
        } else {
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID"), ctx);
        }

        ArrayList<CustomEntity> components = (ArrayList<CustomEntity>) pattern.findThenEvaluate("IMPLEMENTED_COMPONENTS", null, ctx, null);
        if(components != null) {
            if (nbt == null) nbt = new TagCompound();
            for(CustomEntity component : components) {
                nbt = component.getDefaultNBT().merge(nbt);
            }
        } else {
            components = new ArrayList<>();
        }

        return new SummonData(type, null, nbt, components, reference);
    }

    private void collectComponent(Object value, ArrayList<CustomEntity> finalList, TokenPattern<?> p, ISymbolContext ctx) {
        if(value instanceof CustomEntity) {
            CustomEntity component = (CustomEntity) value;
            if (component.isComponent()) {
                finalList.add(component);
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Expected an entity component here, instead got an entity definition", p, ctx);
            }
        } else if(value instanceof ListObject) {
            for(Object inList : ((ListObject) value)) {
                collectComponent(inList, finalList, p, ctx);
            }
        } else {
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Expected an entity component or a list of entity components here, instead got: " + value, p, ctx);
        }
    }

    public static class SummonData {
        public Type type;
        public CoordinateSet pos;
        public TagCompound nbt;
        public ArrayList<CustomEntity> components;
        public Object reference;

        public SummonData(Type type, CoordinateSet pos, TagCompound nbt, ArrayList<CustomEntity> components, Object reference) {
            this.type = type;
            this.pos = pos;
            this.nbt = nbt;
            this.components = components;
            this.reference = reference;
        }

        public void analyzeNBT(TokenPattern<?> pattern, ISymbolContext ctx) {
            if(nbt != null) {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, type);
                NBTInspector.inspectTag(nbt, context, pattern, ctx);
            }
        }

        public Command constructSummon() {
            if(pos == null && nbt != null) pos = new CoordinateSet();
            return new SummonCommand(type, pos, nbt);
        }

        public void fillDefaults() {
            if(pos == null) pos = new CoordinateSet();
            if(nbt == null) nbt = new TagCompound();
        }

        public void mergeNBT(TagCompound nbt) {
            if(nbt == null) return;
            if(this.nbt == null) this.nbt = nbt;
            else this.nbt = this.nbt.merge(nbt);
        }
    }

}
