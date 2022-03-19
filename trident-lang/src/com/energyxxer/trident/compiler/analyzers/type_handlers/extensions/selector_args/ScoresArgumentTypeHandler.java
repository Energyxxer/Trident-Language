package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

import java.util.Map;

import static com.energyxxer.prismarine.typesystem.PrismarineTypeSystem.assertOfClass;

public class ScoresArgumentTypeHandler implements TypeHandler<ScoreArgument> {
    private TypeHandlerMemberCollection<ScoreArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public ScoresArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", this::toDict);

        try {
            members.setConstructor(ScoresArgumentTypeHandler.class.getMethod("construct", DictionaryObject.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(ScoreArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(ScoreArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(ScoreArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<ScoreArgument> getHandledClass() {
        return ScoreArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_scores";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(SelectorArgumentTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }

    private DictionaryObject toDict(ScoreArgument arg) {
        DictionaryObject dict = new DictionaryObject(typeSystem);
        for(Map.Entry<Objective, ScoreArgument.Entry> entry : arg.scores.entrySet()) {
            dict.put(entry.getKey().getName(), entry.getValue().range);
        }
        return dict;
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static ScoreArgument construct(DictionaryObject obj, TokenPattern<?> p, ISymbolContext ctx) {
        ScoreArgument scores = new ScoreArgument();
        for(Map.Entry<String, Symbol> rawEntry : obj.entrySet()) {
            Object rawValue = rawEntry.getValue().getValue(p, ctx);
            if(rawValue == null) continue;
            IntegerRange range = assertOfClass(rawValue, p, ctx, IntegerRange.class);

            String objectiveName = rawEntry.getKey();
            Objective objective = ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().getOrCreate(objectiveName);

            ScoreArgument.Entry entry = new ScoreArgument.Entry(range);
            scores.put(objective, entry);
        }
        return scores;
    }
}
