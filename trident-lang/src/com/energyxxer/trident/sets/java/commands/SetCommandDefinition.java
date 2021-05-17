package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.*;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStore;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStoreDataHolder;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStoreScore;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.defaults.StorageTarget;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTInspector;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.custom.special.ObjectiveCreationFile;
import com.energyxxer.trident.sets.MinecraftLiteralSet;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupSpecialFileManagerTask;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.util.Lazy;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.NULL;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.SCOREBOARD_OPERATOR;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class SetCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"set"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("set"),
                productions.getOrCreateStructure("POINTER"),
                ofType(SCOREBOARD_OPERATOR).setName("OPERATOR"),
                choice(
                        group(productions.getOrCreateStructure("POINTER")).setEvaluator((p, d) -> decorate((PointerObject) p.find("POINTER").evaluate((ISymbolContext) d[0]), p, (ISymbolContext) d[0])),
                        group(productions.getOrCreateStructure("NBT_VALUE")).setEvaluator((p, d) -> new PointerDecorator.ValuePointer((NBTTag) p.find("NBT_VALUE").evaluate((ISymbolContext) d[0]))),
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> new Object[] {d[0]},
                                (value, p, d) -> {
                                    if (value == null) return new PointerDecorator.NullPointer();
                                    if (value instanceof PointerObject)
                                        return decorate(((PointerObject) value), p, (ISymbolContext) d[0]);
                                    if (value instanceof Integer) value = new TagInt((int) value);
                                    else if (value instanceof Double) value = new TagDouble((double) value);
                                    return new PointerDecorator.ValuePointer((NBTTag) value);
                                },
                                true,
                                NBTTag.class,
                                Integer.class,
                                Double.class,
                                PointerObject.class
                        ),
                        ofType(NULL).setName("NULL").setEvaluator((p, d) -> new PointerDecorator.NullPointer())
                ).setName("VALUE")
        );
    }

    public enum SetOperator {
        ASSIGN("="), ADD("+="), SUBTRACT("-="), MULTIPLY("*="), DIVIDE("/="), MODULO("%="), LESS_THAN("<"), GREATER_THAN(">"), SWAP("><");

        private String shorthand;

        SetOperator(String shorthand) {
            this.shorthand = shorthand;
        }

        public String getShorthand() {
            return shorthand;
        }

        public static SetOperator getOperatorForSymbol(String symbol) {
            for (SetOperator operator : values()) {
                if (operator.shorthand.equals(symbol)) return operator;
            }
            throw new IllegalArgumentException("Invalid set-operator shorthand '" + symbol + "'");
        }
    }

    private interface SetOperationHandler<A extends PointerDecorator, B extends PointerDecorator> {
        Command perform(A a, B b, TokenPattern<?> pattern, ISymbolContext ctx);
    }

    private static HashMap<String, SetOperationHandler> handlers = new HashMap<>();

    private static <A extends PointerDecorator, B extends PointerDecorator> void putHandler(Class<A> classA, SetOperator operator, Class<B> classB, SetOperationHandler<A, B> handler) {
        handlers.put(classA.getName() + " " + operator.getShorthand() + " " + classB.getName(), handler);
    }

    static {
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.ASSIGN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.ASSIGN, b.score)
        );

        putHandler(PointerDecorator.ScorePointer.class, SetOperator.ASSIGN, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        return new ScoreSet(a.score, ((TagInt) b.value).getValue());
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot put non-integer values into scores", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.ASSIGN, PointerDecorator.DataHolderPointer.class,
                (a, b, pattern, ctx) -> {
                    Command get = new DataGetCommand(b.holder, b.path, b.scale);
                    return new ExecuteCommand(get, new ExecuteStoreScore(ExecuteStore.StoreValue.RESULT, a.score));
                }
        );


        putHandler(PointerDecorator.DataHolderPointer.class, SetOperator.ASSIGN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) -> {
                    Command get = new ScoreGet(b.score);
                    return new ExecuteCommand(get, new ExecuteStoreDataHolder(ExecuteStore.StoreValue.RESULT, a.holder, a.path, a.type.getValue(), a.scale));
                }
        );
        putHandler(PointerDecorator.DataHolderPointer.class, SetOperator.ASSIGN, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (a.scale != 1) {
                        if (b.value instanceof NumericNBTTag) {
                            b.value = ((NumericNBTTag) b.value).scale(a.scale);
                        } else {
                            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Cannot scale a non-numerical value, found " + b.value.getType(), pattern, ctx);
                        }
                    }
                    NBTInspector.inspectTag(b.value, NBTInspector.createContextForDataHolder(a.holder, ctx), a.path, pattern, ctx);
                    return new DataModifyCommand(a.holder, a.path, DataModifyCommand.SET(), new ModifySourceValue(b.value));
                }
        );
        putHandler(PointerDecorator.DataHolderPointer.class, SetOperator.ASSIGN, PointerDecorator.DataHolderPointer.class,
                (a, b, pattern, ctx) -> {
                    NBTInspector.comparePaths(a.path, NBTInspector.createContextForDataHolder(a.holder, ctx), b.path, NBTInspector.createContextForDataHolder(b.holder, ctx), pattern, ctx);

                    if (a.scale * b.scale == 1) {
                        return new DataModifyCommand(a.holder, a.path, DataModifyCommand.SET(), new ModifySourceFromHolder(b.holder, b.path));
                    }
                    Command get = new DataGetCommand(b.holder, b.path, b.scale);
                    return new ExecuteCommand(get, new ExecuteStoreDataHolder(ExecuteStore.StoreValue.RESULT, a.holder, a.path, a.type.getValue(), a.scale));
                }
        );


        putHandler(PointerDecorator.ScorePointer.class, SetOperator.ADD, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.ADD, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.SUBTRACT, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.SUBTRACT, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.MULTIPLY, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.MULTIPLY, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.DIVIDE, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.DIVIDE, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.MODULO, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.MODULO, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.LESS_THAN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.LESS_THAN, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.GREATER_THAN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.GREATER_THAN, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.SWAP, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.SWAP, b.score)
        );


        putHandler(PointerDecorator.ScorePointer.class, SetOperator.ADD, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        return new ScoreAdd(a.score, ((TagInt) b.value).getValue());
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.SUBTRACT, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        return new ScoreAdd(a.score, -((TagInt) b.value).getValue());
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.MULTIPLY, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        TridentUtil.assertLanguageLevel(ctx, 2, "Score constants in operations are ", pattern);
                        return new ScorePlayersOperation(
                                a.score,
                                ScorePlayersOperation.Operation.MULTIPLY,
                                ((ObjectiveCreationFile) ctx.get(SetupSpecialFileManagerTask.INSTANCE).get("create_objectives"))
                                        .getConstant(((TagInt) b.value).getValue())
                        );
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.DIVIDE, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        TridentUtil.assertLanguageLevel(ctx, 2, "Score constants in operations are ", pattern);
                        return new ScorePlayersOperation(
                                a.score,
                                ScorePlayersOperation.Operation.DIVIDE,
                                ((ObjectiveCreationFile) ctx.get(SetupSpecialFileManagerTask.INSTANCE).get("create_objectives"))
                                        .getConstant(((TagInt) b.value).getValue())
                        );
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.MODULO, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        TridentUtil.assertLanguageLevel(ctx, 2, "Score constants in operations are ", pattern);
                        return new ScorePlayersOperation(
                                a.score,
                                ScorePlayersOperation.Operation.MODULO,
                                ((ObjectiveCreationFile) ctx.get(SetupSpecialFileManagerTask.INSTANCE).get("create_objectives"))
                                        .getConstant(((TagInt) b.value).getValue())
                        );
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.LESS_THAN, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        TridentUtil.assertLanguageLevel(ctx, 2, "Score constants in operations are ", pattern);
                        return new ScorePlayersOperation(
                                a.score,
                                ScorePlayersOperation.Operation.LESS_THAN,
                                ((ObjectiveCreationFile) ctx.get(SetupSpecialFileManagerTask.INSTANCE).get("create_objectives"))
                                        .getConstant(((TagInt) b.value).getValue())
                        );
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, SetOperator.GREATER_THAN, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (b.value instanceof TagInt) {
                        TridentUtil.assertLanguageLevel(ctx, 2, "Score constants in operations are ", pattern);
                        return new ScorePlayersOperation(
                                a.score,
                                ScorePlayersOperation.Operation.GREATER_THAN,
                                ((ObjectiveCreationFile) ctx.get(SetupSpecialFileManagerTask.INSTANCE).get("create_objectives"))
                                        .getConstant(((TagInt) b.value).getValue())
                        );
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );


        putHandler(PointerDecorator.DataHolderPointer.class, SetOperator.MULTIPLY, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if (!(b.value instanceof NumericNBTTag)) {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Can only multiply by a numeric NBT type", pattern, ctx);
                    }
                    double bValue = 1.0;
                    if (b.value instanceof TagByte) bValue = ((TagByte) b.value).getValue();
                    if (b.value instanceof TagShort) bValue = ((TagShort) b.value).getValue();
                    if (b.value instanceof TagInt) bValue = ((TagInt) b.value).getValue();
                    if (b.value instanceof TagFloat) bValue = ((TagFloat) b.value).getValue();
                    if (b.value instanceof TagDouble) bValue = ((TagDouble) b.value).getValue();
                    if (b.value instanceof TagLong) bValue = ((TagLong) b.value).getValue();

                    if (a.scale != 1) {
                        bValue *= a.scale;
                    }
                    Command get = new DataGetCommand(a.holder, a.path, bValue);
                    return new ExecuteCommand(get, new ExecuteStoreDataHolder(ExecuteStore.StoreValue.RESULT, a.holder, a.path, a.type.getValue(), 1 / a.scale));
                }
        );


        putHandler(PointerDecorator.ScorePointer.class, SetOperator.ASSIGN, PointerDecorator.NullPointer.class,
                (a, b, pattern, ctx) -> new ScoreReset(a.score)
        );
        putHandler(PointerDecorator.DataHolderPointer.class, SetOperator.ASSIGN, PointerDecorator.NullPointer.class,
                (a, b, pattern, ctx) -> new DataRemoveCommand(a.holder, a.path)
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {

        PointerDecorator target = decorate((PointerObject) pattern.find("POINTER").evaluate(ctx), pattern.find("POINTER"), ctx);
        SetOperator operator = SetOperator.getOperatorForSymbol(pattern.find("OPERATOR").flatten(false));
        PointerDecorator source = (PointerDecorator) pattern.find("VALUE").evaluate(ctx);

        String key = target.getClass().getName() + " " + operator.getShorthand() + " " + source.getClass().getName();

        try {
            if (handlers.containsKey(key)) {
                return handlers.get(key).perform(target, source, pattern, ctx);
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Set-command operator '" + operator.getShorthand() + " is not defined for operand types " + target.getName() + " and " + source.getName(), pattern, ctx);
            }
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern)
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    private PointerDecorator decorate(PointerObject pointer, TokenPattern<?> pattern, ISymbolContext ctx) {
        pointer.validate(pattern, ctx);

        if (pointer.getMember() instanceof String) {
            return new PointerDecorator.ScorePointer(new LocalScore((Entity) pointer.getTarget(), MinecraftLiteralSet.parseObjective((String) pointer.getMember(), pattern, ctx)));
        } else {
            Lazy<NumericNBTType> lazyTypeInstantiator = new Lazy<>(
                    () -> pointer.getNumericType() != null ?
                            pointer.getNumericType() :
                            CommonParsers.getNumericType(pointer.getTarget(), ((NBTPath) pointer.getMember()), ctx, pattern, true)
            );
            DataHolder holder;
            if (pointer.getTarget() instanceof Entity) {
                holder = new DataHolderEntity(((Entity) pointer.getTarget()));
            } else if (pointer.getTarget() instanceof CoordinateSet) {
                holder = new DataHolderBlock(((CoordinateSet) pointer.getTarget()));
            } else if (pointer.getTarget() instanceof ResourceLocation) {
                holder = new DataHolderStorage(new StorageTarget(ctx.get(SetupModuleTask.INSTANCE).getNamespace(((ResourceLocation) pointer.getTarget()).namespace), ((ResourceLocation) pointer.getTarget()).body));
            } else {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Got pointer target: " + pointer + "; none of the known types.", pattern, ctx);
            }
            return new PointerDecorator.DataHolderPointer(holder, ((NBTPath) pointer.getMember()), pointer.getScale(), lazyTypeInstantiator);
        }
    }

    interface PointerDecorator {

        String getName();

        class ScorePointer implements PointerDecorator {
            LocalScore score;

            ScorePointer(LocalScore score) {
                this.score = score;
            }

            @Override
            public String getName() {
                return "Score";
            }
        }

        class DataHolderPointer implements PointerDecorator {
            DataHolder holder;
            NBTPath path;
            double scale;
            Lazy<NumericNBTType> type;

            DataHolderPointer(DataHolder holder, NBTPath path, double scale, Lazy<NumericNBTType> type) {
                this.holder = holder;
                this.path = path;
                this.scale = scale;
                this.type = type;
            }

            @Override
            public String getName() {
                return "Data Holder";
            }
        }

        class ValuePointer implements PointerDecorator {
            NBTTag value;

            ValuePointer(NBTTag value) {
                this.value = value;
            }

            @Override
            public String getName() {
                return "Value";
            }
        }

        class NullPointer implements PointerDecorator {
            @Override
            public String getName() {
                return "Null";
            }
        }
    }
}
