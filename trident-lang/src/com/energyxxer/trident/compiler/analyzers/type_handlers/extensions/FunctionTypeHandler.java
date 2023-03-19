package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;

import java.util.stream.Collectors;

public class FunctionTypeHandler implements TypeHandler<PrimitivePrismarineFunction> {

    private final PrismarineTypeSystem typeSystem;

    public FunctionTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(PrimitivePrismarineFunction object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(object instanceof PrismarineFunction) {
            if(member.equals("formalParameters")) return new ListObject(ctx.getTypeSystem(), ((PrismarineFunction) object).getBranch().getFormalParameters().stream().map(p -> {
                DictionaryObject entry = new DictionaryObject(ctx.getTypeSystem());
                entry.put("name", p.getName());
                entry.put("type", p.getConstraints().getHandler());
                entry.put("nullable", p.getConstraints().isNullable());
                return entry;
            }).collect(Collectors.toList()));
            if(member.equals("declaringFile")) return ((TridentFile) ((PrismarineFunction) object).getDeclaringContext().getStaticParentUnit()).getResourceLocation();
        } else {
            if(member.equals("formalParameters")) return new ListObject(getTypeSystem());
            if(member.equals("declaringFile")) return null;
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(PrimitivePrismarineFunction object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(PrimitivePrismarineFunction object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<PrimitivePrismarineFunction> getHandledClass() {
        return PrimitivePrismarineFunction.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "function";
    }
}
