package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.AdvancementArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementArgumentEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCompletionEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCriterionEntry;
import com.energyxxer.commodore.functionlogic.selector.arguments.advancement.AdvancementCriterionGroupEntry;
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
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;

import java.util.Map;

import static com.energyxxer.prismarine.typesystem.PrismarineTypeSystem.assertOfClass;

public class AdvancementsArgumentTypeHandler implements TypeHandler<AdvancementArgument> {
    private TypeHandlerMemberCollection<AdvancementArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public AdvancementsArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", this::toDict);

        try {
            members.setConstructor(AdvancementsArgumentTypeHandler.class.getMethod("construct", DictionaryObject.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(AdvancementArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(AdvancementArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(AdvancementArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<AdvancementArgument> getHandledClass() {
        return AdvancementArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_advancements";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(SelectorArgumentTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }

    private DictionaryObject toDict(AdvancementArgument arg) {
        DictionaryObject dict = new DictionaryObject(typeSystem);
        for(AdvancementArgumentEntry entry : arg.entries) {
            if(entry instanceof AdvancementCompletionEntry) {
                dict.put(entry.getAdvancementName(), ((AdvancementCompletionEntry) entry).value);
            } else if(entry instanceof AdvancementCriterionGroupEntry) {
                DictionaryObject subDict = new DictionaryObject(typeSystem);
                for(AdvancementCriterionEntry criterion : ((AdvancementCriterionGroupEntry) entry).criteria) {
                    subDict.put(criterion.criterionName, criterion.value);
                }
                dict.put(entry.getAdvancementName(), subDict);
            }
        }
        return dict;
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static AdvancementArgument construct(DictionaryObject obj, TokenPattern<?> p, ISymbolContext ctx) {
        AdvancementArgument advancements = new AdvancementArgument();
        for(Map.Entry<String, Symbol> rawEntry : obj.entrySet()) {
            String advancementName = new ResourceLocation(rawEntry.getKey()).toString();
            Object rawValue = rawEntry.getValue().getValue(p, ctx);
            if(rawValue == null) continue;
            rawValue = assertOfClass(rawValue, p, ctx, boolean.class, DictionaryObject.class);

            if(rawValue instanceof Boolean) {
                advancements.addEntry(new AdvancementCompletionEntry(advancementName, (Boolean) rawValue));
            } else if(rawValue instanceof DictionaryObject) {
                AdvancementCriterionGroupEntry criteria = new AdvancementCriterionGroupEntry(advancementName);

                for(Map.Entry<String, Symbol> rawSubEntry : ((DictionaryObject) rawValue).entrySet()) {
                    Object rawCriterionValue = rawSubEntry.getValue().getValue(p, ctx);
                    if(rawCriterionValue == null) continue;
                    boolean value = assertOfClass(rawCriterionValue, p, ctx, boolean.class);
                    criteria.addCriteria(new AdvancementCriterionEntry(rawEntry.getKey(), value));
                }

                advancements.addEntry(criteria);
            }
        }
        return advancements;
    }
}
