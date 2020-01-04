package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.DataGetCommand;
import com.energyxxer.commodore.functionlogic.commands.data.DataModifyCommand;
import com.energyxxer.commodore.functionlogic.commands.data.ModifySourceFromHolder;
import com.energyxxer.commodore.functionlogic.commands.data.ModifySourceValue;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStore;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStoreDataHolder;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStoreScore;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreAdd;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreGet;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.defaults.StorageTarget;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.Lazy;

import java.util.HashMap;
import java.util.Locale;

@AnalyzerMember(key = "set")
public class SetParser implements SimpleCommandParser {

    private interface SetOperationHandler<A extends PointerDecorator, B extends PointerDecorator> {
        Command perform(A a, B b, TokenPattern<?> pattern, ISymbolContext ctx);
    }

    private static HashMap<String, SetOperationHandler> handlers = new HashMap<>();

    private static <A extends PointerDecorator, B extends PointerDecorator> void putHandler(Class<A> classA, ScorePlayersOperation.Operation operator, Class<B> classB, SetOperationHandler<A, B> handler) {
        handlers.put(classA.getName() + " " + operator.getShorthand() + " " + classB.getName(), handler);
    }

    static {
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.ASSIGN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.ASSIGN, b.score)
        );

        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.ASSIGN, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if(b.value instanceof TagInt) {
                        return new ScoreSet(a.score, ((TagInt) b.value).getValue());
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot put non-integer values into scores", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.ASSIGN, PointerDecorator.DataHolderPointer.class,
                (a, b, pattern, ctx) -> {
                    Command get = new DataGetCommand(b.holder, b.path, b.scale);
                    return new ExecuteCommand(get, new ExecuteStoreScore(ExecuteStore.StoreValue.RESULT, a.score));
                }
        );


        putHandler(PointerDecorator.DataHolderPointer.class, ScorePlayersOperation.Operation.ASSIGN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) -> {
                    Command get = new ScoreGet(b.score);
                    return new ExecuteCommand(get, new ExecuteStoreDataHolder(ExecuteStore.StoreValue.RESULT, a.holder, a.path, a.type.getValue(), a.scale));
                }
        );
        putHandler(PointerDecorator.DataHolderPointer.class, ScorePlayersOperation.Operation.ASSIGN, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if(a.scale != 1) {
                        if(b.value instanceof NumericNBTTag) {
                            b.value = ((NumericNBTTag) b.value).scale(a.scale);
                        } else {
                            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Cannot scale a non-numerical value, found " + b.value.getType(), pattern, ctx);
                        }
                    }
                    return new DataModifyCommand(a.holder, a.path, DataModifyCommand.SET(), new ModifySourceValue(b.value));
                }
        );
        putHandler(PointerDecorator.DataHolderPointer.class, ScorePlayersOperation.Operation.ASSIGN, PointerDecorator.DataHolderPointer.class,
                (a, b, pattern, ctx) -> {
                    if(a.scale * b.scale == 1) {
                        return new DataModifyCommand(a.holder, a.path, DataModifyCommand.SET(), new ModifySourceFromHolder(b.holder, b.path));
                    }
                    Command get = new DataGetCommand(b.holder, b.path, b.scale);
                    return new ExecuteCommand(get, new ExecuteStoreDataHolder(ExecuteStore.StoreValue.RESULT, a.holder, a.path, a.type.getValue(), a.scale));
                }
        );


        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.ADD, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.ADD, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.SUBTRACT, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.SUBTRACT, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.MULTIPLY, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.MULTIPLY, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.DIVIDE, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.DIVIDE, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.MODULO, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.MODULO, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.LESS_THAN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.LESS_THAN, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.GREATER_THAN, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.GREATER_THAN, b.score)
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.SWAP, PointerDecorator.ScorePointer.class,
                (a, b, pattern, ctx) ->
                        new ScorePlayersOperation(a.score, ScorePlayersOperation.Operation.SWAP, b.score)
        );


        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.ADD, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if(b.value instanceof TagInt) {
                        return new ScoreAdd(a.score, ((TagInt) b.value).getValue());
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );
        putHandler(PointerDecorator.ScorePointer.class, ScorePlayersOperation.Operation.SUBTRACT, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if(b.value instanceof TagInt) {
                        return new ScoreAdd(a.score, -((TagInt) b.value).getValue());
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot perform operations between scores and non-integer", pattern, ctx);
                    }
                }
        );



        putHandler(PointerDecorator.DataHolderPointer.class, ScorePlayersOperation.Operation.MULTIPLY, PointerDecorator.ValuePointer.class,
                (a, b, pattern, ctx) -> {
                    if(!(b.value instanceof NumericNBTTag)) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Can only multiply by a numeric NBT type", pattern, ctx);
                    }
                    double bValue = 1.0;
                    if(b.value instanceof TagByte) bValue = ((TagByte) b.value).getValue();
                    if(b.value instanceof TagShort) bValue = ((TagShort) b.value).getValue();
                    if(b.value instanceof TagInt) bValue = ((TagInt) b.value).getValue();
                    if(b.value instanceof TagFloat) bValue = ((TagFloat) b.value).getValue();
                    if(b.value instanceof TagDouble) bValue = ((TagDouble) b.value).getValue();
                    if(b.value instanceof TagLong) bValue = ((TagLong) b.value).getValue();

                    if(a.scale != 1) {
                        bValue *= a.scale;
                    }
                    Command get = new DataGetCommand(a.holder, a.path, bValue);
                    return new ExecuteCommand(get, new ExecuteStoreDataHolder(ExecuteStore.StoreValue.RESULT, a.holder, a.path, a.type.getValue(), 1/a.scale));
                }
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {

        PointerDecorator target = parsePointer(pattern.find("POINTER"), ctx);
        ScorePlayersOperation.Operation operator = ScorePlayersOperation.Operation.getOperationForSymbol(pattern.find("OPERATOR").flatten(false));
        PointerDecorator source = parsePointer(pattern.find("VALUE"), ctx);

        String key = target.getClass().getName() + " " + operator.getShorthand() + " " + source.getClass().getName();

        try {
            if(handlers.containsKey(key)) {
                //noinspection unchecked
                return handlers.get(key).perform(target, source, pattern, ctx);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Set-command operator '" + operator.getShorthand() + " is not defined for operand types " + target.getName() + " and " + source.getName(), pattern, ctx);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern)
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    private PointerDecorator parsePointer(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "VALUE":
                return parsePointer(((TokenStructure) pattern).getContents(), ctx);
            case "POINTER":
            case "VARIABLE_POINTER":
            case "ENTITY_POINTER":
            case "BLOCK_POINTER":
            case "STORAGE_POINTER":
                return decorate(CommonParsers.parsePointer(pattern, ctx), pattern, ctx);
            case "NBT_VALUE":
                return new PointerDecorator.ValuePointer(NBTParser.parseValue(pattern, ctx));
            case "INTERPOLATION_BLOCK":
                Object value = InterpolationManager.parse(pattern, ctx, NBTTag.class, Integer.class, Double.class, PointerObject.class);
                if(value == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Unexpected null value at pointer", pattern, ctx);
                }
                if(value instanceof PointerObject) return decorate(((PointerObject) value), pattern, ctx);
                if(value instanceof Integer) value = new TagInt((int) value);
                else if(value instanceof Double) value = new TagDouble((double) value);
                return new PointerDecorator.ValuePointer((NBTTag) value);
            default:
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
        }
    }

    private PointerDecorator decorate(PointerObject pointer, TokenPattern<?> pattern, ISymbolContext ctx) {
        pointer.validate(pattern, ctx);

        if(pointer.getMember() instanceof String) {
            return new PointerDecorator.ScorePointer(new LocalScore((Entity) pointer.getTarget(), CommonParsers.parseObjective((String) pointer.getMember(), pattern, ctx)));
        } else {
            Lazy<NumericNBTType> lazyTypeInstantiator = new Lazy<>(
                    () -> pointer.getNumericType() != null ?
                            NumericNBTType.valueOf(pointer.getNumericType().toUpperCase(Locale.ENGLISH)) :
                            CommonParsers.getNumericType(pointer.getTarget(), ((NBTPath) pointer.getMember()), ctx, pattern, true)
            );
            DataHolder holder;
            if(pointer.getTarget() instanceof Entity) {
                holder = new DataHolderEntity(((Entity) pointer.getTarget()));
            } else if(pointer.getTarget() instanceof CoordinateSet) {
                holder = new DataHolderBlock(((CoordinateSet) pointer.getTarget()));
            } else if(pointer.getTarget() instanceof TridentUtil.ResourceLocation) {
                holder = new DataHolderStorage(new StorageTarget(ctx.getCompiler().getModule().getNamespace(((TridentUtil.ResourceLocation) pointer.getTarget()).namespace), ((TridentUtil.ResourceLocation) pointer.getTarget()).body));
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Got pointer target: " + pointer + "; none of the known types.", pattern, ctx);
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
    }
}
