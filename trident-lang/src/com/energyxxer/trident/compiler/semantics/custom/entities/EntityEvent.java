package com.energyxxer.trident.compiler.semantics.custom.entities;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class EntityEvent implements TypeHandler<EntityEvent> {
    private final PrismarineTypeSystem typeSystem;
    private final boolean isStaticHandler;

    private ResourceLocation location;
    private Function function;

    private EntityEvent(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
        this.isStaticHandler = true;
    }

    public EntityEvent(PrismarineTypeSystem typeSystem, ResourceLocation location, Function function) {
        this.typeSystem = typeSystem;
        this.location = location;
        this.function = function;
        this.isStaticHandler = false;
    }

    @Override
    public Object getMember(EntityEvent object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return ctx.getTypeSystem().getMetaTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
        if("function".equals(member)) return location;
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(EntityEvent object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return ctx.getTypeSystem().getMetaTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(EntityEvent object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(isStaticHandler) return ctx.getTypeSystem().getMetaTypeHandler().cast(object, targetType, pattern, ctx);
        return null;
    }

    @Override
    public String toString() {
        return "[Entity Event: " + location + "]";
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public Function getFunction() {
        return function;
    }

    public static void defineEvent(TokenPattern<?> pattern, ISymbolContext ctx) {
        String eventName = pattern.find("EVENT_NAME").flatten(false);
        String functionPath = ((TridentFile) ctx.getStaticParentUnit()).getResourceLocation().toString() + "/tdn_dispatch_event_" + eventName.toLowerCase();
        ResourceLocation functionLoc = new ResourceLocation(functionPath);
        if(ctx.get(SetupModuleTask.INSTANCE).getNamespace(functionLoc.namespace).functions.exists(functionLoc.body)) {
            throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "A function by the name '" + functionLoc + "' already exists.", pattern, ctx);
        }
        Function function = ctx.get(SetupModuleTask.INSTANCE).getNamespace(functionLoc.namespace).functions.create(functionLoc.body);
        if(pattern.find("EVENT_INITIALIZATION") != null) {
            ISymbolContext innerCtx = new SymbolContext(ctx);
            TridentFile.resolveInnerFileIntoSection(pattern.find("EVENT_INITIALIZATION.ANONYMOUS_INNER_FUNCTION"), innerCtx, function);
        }
        EntityEvent event = new EntityEvent(ctx.getTypeSystem(), functionLoc, function);
        ctx.put(new Symbol(eventName, TridentSymbolVisibility.LOCAL, event));
    }

    @Override
    public Class<EntityEvent> getHandledClass() {
        return EntityEvent.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "entity_event";
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    public static EntityEvent createStaticHandler(PrismarineTypeSystem typeSystem) {
        return new EntityEvent(typeSystem);
    }

    @Override
    public boolean isStaticHandler() {
        return isStaticHandler;
    }
}
