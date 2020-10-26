package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagLong;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;

import static com.energyxxer.prismarine.typesystem.PrismarineTypeSystem.assertOfClass;

public class TagLongTypeHandler implements TypeHandler<TagLong> {
    static final PrimitivePrismarineFunction CONSTRUCTOR = (params, ctx, thisObject) -> constructTagLong(params, ctx);

    private final PrismarineTypeSystem typeSystem;

    public TagLongTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagLong object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagLong object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagLong object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch (ctx.getTypeSystem().getInternalTypeIdentifierForType(targetType)) {
            case "primitive(int)":
                return object.getValue().intValue();
            case "primitive(real)":
                return object.getValue().doubleValue();
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagLong> getHandledClass() {
        return TagLong.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_long";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static TagLong constructTagLong(ActualParameterList params, ISymbolContext ctx) {
        if(params.size() == 0 || params.getValue(0) == null) return new TagLong(0L);
        Object param = assertOfClass(params.getValue(0), params.getPattern(0), ctx, TagLong.class, Integer.class, Double.class, String.class);
        if(param instanceof String) {
            try {
                return new TagLong(Long.parseLong((String) param));
            } catch(NumberFormatException x) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, x.getMessage(), params.getPattern(0), ctx);
            }
        } else if(param instanceof Double) {
            return new TagLong((long)(double) param);
        } else {
            return new TagLong((long)(int) param);
        }
    }
}
