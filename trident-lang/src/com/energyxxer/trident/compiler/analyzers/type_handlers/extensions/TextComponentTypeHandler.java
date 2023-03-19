package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.extensions.EObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TextComponentTypeHandler implements TypeHandler<TextComponent> {
    private static final PrimitivePrismarineFunction CONSTRUCTOR = (params, ctx, thisObject) -> constructTextComponent(params, ctx);

    private final PrismarineTypeSystem typeSystem;

    public TextComponentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TextComponent object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TextComponent object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TextComponent object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<TextComponent> getHandledClass() {
        return TextComponent.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "text_component";
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return CONSTRUCTOR;
    }

    private static TextComponent constructTextComponent(ActualParameterList params, ISymbolContext ctx) {
        if (params.size() == 0) return new StringTextComponent("");
        EObject.assertNotNull(params.getValue(0), params.getPattern(0), ctx);

        boolean skipIncompatibleTypes = false;
        if(params.size() >= 2) {
            EObject.assertNotNull(params.getValue(1), params.getPattern(1), ctx);
            skipIncompatibleTypes = PrismarineTypeSystem.assertOfClass(params.getValue(1), params.getPattern(1), ctx, Boolean.class);
        }

        try {
            JsonElement asJson = com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.JSON.toJson(params.getValue(0), e -> {
                if(e instanceof TextComponent) return new TextParser.TextComponentJsonElement((TextComponent) e);
                if(e instanceof ResourceLocation
                        || e instanceof Entity
                        || e instanceof CoordinateSet) return new JsonPrimitive(e.toString());
                if(e instanceof PointerObject) {
                    ((PointerObject) e).validate(params.getPattern(), ctx);
                    Object target = ((PointerObject) e).getTarget();
                    Object member = ((PointerObject) e).getMember();
                    JsonObject inner = new JsonObject();
                    if(member instanceof String) {
                        //is score
                        inner.addProperty("name", ((Entity) target).toString());
                        inner.addProperty("objective", member.toString());
                        JsonObject outer = new JsonObject();
                        outer.add("score", inner);
                        return outer;
                    } else {
                        //is nbt
                        inner.addProperty(target instanceof Entity ? "entity" : "block", target.toString());
                        inner.addProperty("nbt", member.toString());
                        return inner;
                    }
                }
                return null;
            }, skipIncompatibleTypes, ctx);
            if (asJson != null) {
                return TextParser.jsonToTextComponent(asJson, ctx, params.getPattern(0), TextComponentContext.CHAT);
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot turn a value of type " + ctx.getTypeSystem().getTypeIdentifierForObject(params.getValue(0)) + " into a text component", params.getPattern(0), ctx);
            }
        } catch(IllegalArgumentException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, x.getMessage(), params.getPattern(), ctx);
        }
    }
}
