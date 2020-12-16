package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagLong;
import com.energyxxer.commodore.functionlogic.nbt.TagLongArray;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;

public class TagLongArrayTypeHandler implements TypeHandler<TagLongArray> {
    private static final PrimitivePrismarineFunction CONSTRUCTOR = (params, ctx, thisObject) -> constructTagLongArray(params, ctx);

    private final PrismarineTypeSystem typeSystem;

    public TagLongArrayTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagLongArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagLongArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagLongArray object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if ("primitive(list)".equals(typeSystem.getInternalTypeIdentifierForType(targetType))) {
            return new ListObject(typeSystem, object.getAllTags());
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagLongArray> getHandledClass() {
        return TagLongArray.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_long_array";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return CONSTRUCTOR;
    }

    private static TagLongArray constructTagLongArray(ActualParameterList params, ISymbolContext ctx) {
        if(params.size() == 0 || params.getValue(0) == null) return new TagLongArray();
        ListObject list = PrismarineTypeSystem.assertOfClass(params.getValue(0), params.getPattern(0), ctx, ListObject.class);

        TagLongArray arr = new TagLongArray();

        for(Object obj : list) {
            if(obj instanceof TagLong) {
                arr.add((TagLong) obj);
            } else {
                arr.add((TagLong) TagLongTypeHandler.CONSTRUCTOR.call(new ActualParameterList(new Object[] {obj}, null, params.getPattern()), ctx, null));
            }
        }

        return arr;
    }
}
