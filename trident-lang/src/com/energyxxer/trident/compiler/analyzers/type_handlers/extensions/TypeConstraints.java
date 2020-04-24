package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class TypeConstraints {
    private TypeHandler<?> handler = null;
    private boolean nullable = true;

    public TypeConstraints(TypeHandler<?> handler, boolean nullable) {
        this.handler = handler;
        this.nullable = nullable;
    }

    public static TypeConstraints parseConstraintsInfer(TokenPattern<?> pattern, ISymbolContext ctx, Object value) {
        TypeConstraints constraints = parseConstraints(pattern, ctx);
        if(constraints == null) {
            if(value != null) {
                constraints = new TypeConstraints(TridentTypeManager.getHandlerForObject(value), true);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot infer type constraints for null", pattern, ctx);
            }
        }
        return constraints;
    }

    public static TypeConstraints parseConstraints(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern != null) {
            boolean inferConstraints = pattern.find("TYPE_CONSTRAINTS_INNER") == null;
            if(!inferConstraints) {
                return new TypeConstraints(
                        InterpolationManager.parseType(pattern.find("TYPE_CONSTRAINTS_INNER.INTERPOLATION_TYPE"), ctx),
                        pattern.find("TYPE_CONSTRAINTS_INNER.VARIABLE_NULLABLE") != null
                );
            } else {
                return null;
            }
        }
        return new TypeConstraints(null, true);
    }

    public void validate(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(value == null && !nullable) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected a non-null value, Found null", pattern, ctx);
        }
        if(value != null && handler != null && !handler.isInstance(value)) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Incompatible types. Expected '" + handler.getTypeIdentifier() + "', Found '" + TridentTypeManager.getTypeIdentifierForObject(value) + "'", pattern, ctx);
        }
    }

    public boolean verify(Object value) {
        return (value != null || nullable) && (value == null || handler == null || handler.isInstance(value));
    }

    public TypeHandler<?> getHandler() {
        return handler;
    }

    public boolean isNullable() {
        return nullable;
    }

    @Override
    public String toString() {
        return (handler != null ? TridentTypeManager.getTypeIdentifierForType(handler) : "*") + (nullable ? "?" : "");
    }
}
