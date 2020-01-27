package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class EntityEvent implements VariableTypeHandler<EntityEvent> {
    private TridentUtil.ResourceLocation location;
    private Function function;

    public EntityEvent(TridentUtil.ResourceLocation location, Function function) {
        this.location = location;
        this.function = function;
    }

    @Override
    public Object getMember(EntityEvent object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if("function".equals(member)) return location;
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(EntityEvent object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(EntityEvent object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return "[Entity Event: " + location + "]";
    }

    public TridentUtil.ResourceLocation getLocation() {
        return location;
    }

    public Function getFunction() {
        return function;
    }

    public static void defineEvent(TokenPattern<?> pattern, ISymbolContext ctx) {
        String eventName = pattern.find("EVENT_NAME").flatten(false);
        String functionPath = ctx.getStaticParentFile().getResourceLocation().toString() + "/trident_dispatch_event_" + eventName.toLowerCase();
        TridentUtil.ResourceLocation functionLoc = new TridentUtil.ResourceLocation(functionPath);
        if(ctx.getCompiler().getModule().getNamespace(functionLoc.namespace).functions.exists(functionLoc.body)) {
            throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "A function by the name '" + functionLoc + "' already exists.", pattern, ctx);
        }
        Function function = ctx.getCompiler().getModule().getNamespace(functionLoc.namespace).functions.create(functionLoc.body);
        EntityEvent event = new EntityEvent(functionLoc, function);
        ctx.put(new Symbol(eventName, Symbol.SymbolVisibility.LOCAL, event));
    }
}
