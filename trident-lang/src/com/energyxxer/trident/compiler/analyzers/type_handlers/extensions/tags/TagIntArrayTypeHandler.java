package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagInt;
import com.energyxxer.commodore.functionlogic.nbt.TagIntArray;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;

import static com.energyxxer.prismarine.typesystem.PrismarineTypeSystem.assertOfClass;

public class TagIntArrayTypeHandler implements TypeHandler<TagIntArray> {
    private static final PrimitivePrismarineFunction CONSTRUCTOR = (params, ctx, thisObject) -> constructTagIntArray(params, ctx);

    private final PrismarineTypeSystem typeSystem;

    public TagIntArrayTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagIntArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagIntArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagIntArray object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if ("primitive(list)".equals(typeSystem.getInternalTypeIdentifierForType(targetType))) {
            return new ListObject(typeSystem, object.getAllTags());
        }
        return null;
    }

    @Override
    public Class<TagIntArray> getHandledClass() {
        return TagIntArray.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_int_array";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return CONSTRUCTOR;
    }

    private static TagIntArray constructTagIntArray(ActualParameterList params, ISymbolContext ctx) {
        if(params.size() == 0 || params.getValue(0) == null) return new TagIntArray();
        ListObject list = PrismarineTypeSystem.assertOfClass(params.getValue(0), params.getPattern(0), ctx, ListObject.class);

        TagIntArray arr = new TagIntArray();

        for(Object obj : list) {
            Object checked = assertOfClass(obj, params.getPattern(0), ctx, Integer.class, TagInt.class);
            if(checked instanceof TagInt) {
                arr.add((TagInt) checked);
            } else {
                arr.add(new TagInt((int) checked));
            }
        }

        return arr;
    }
}
