package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TypeConstraints {
    public enum SpecialInferInstruction {
        NO_INSTANCE_INFER
    }

    private TypeHandler<?> handler = null;
    private boolean nullable = true;

    private String classIdentifier = null;

    public TypeConstraints(TypeHandler<?> handler, boolean nullable) {
        this.handler = handler;
        this.nullable = nullable;
    }

    public TypeConstraints(@NotNull String classIdentifier, boolean nullable) {
        this.nullable = nullable;
        this.classIdentifier = classIdentifier;

        CustomClass.registerStringIdentifiedClassListener(classIdentifier, cls -> handler = cls);
    }

    public static TypeConstraints parseConstraintsInfer(TokenPattern<?> pattern, ISymbolContext ctx, Object value) {
        TypeConstraints constraints = parseConstraints(pattern, ctx);
        if(constraints == null) {
            if(value == SpecialInferInstruction.NO_INSTANCE_INFER) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot infer type constraints for instance fields", pattern, ctx);
            } else if(value != null) {
                constraints = new TypeConstraints(TridentTypeManager.getStaticHandlerForObject(value), true);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot infer type constraints for null", pattern, ctx);
            }
        }
        return constraints;
    }

    @Contract("null, _ -> new")
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
        return new TypeConstraints((TypeHandler<?>) null, true);
    }

    public Object adjustValue(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(handler != null) value = TridentFunction.HelperMethods.convertToType(value, pattern, ctx, nullable, handler);
        return value;
    }

    public void validate(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(value == null && !nullable) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected a non-null value, Found null", pattern, ctx);
        }
        if(value != null && handler != null && !handler.isInstance(value) && !TridentTypeManager.getHandlerForObject(value).canCoerce(value, handler)) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Incompatible types. Expected '" + handler.getTypeIdentifier() + "', Found '" + TridentTypeManager.getTypeIdentifierForObject(value) + "'", pattern, ctx);
        }
    }

    public void validateExact(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(value == null && !nullable) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected a non-null value, Found null", pattern, ctx);
        }
        if(value != null && handler != null && !handler.isInstance(value)) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Incompatible types. Expected '" + handler.getTypeIdentifier() + "', Found '" + TridentTypeManager.getTypeIdentifierForObject(value) + "'", pattern, ctx);
        }
    }

    public boolean verify(Object value) {
        return (value != null || nullable) && (value == null || handler == null || handler.isInstance(value) || TridentTypeManager.getHandlerForObject(value).canCoerce(value, handler));
    }

    public TypeHandler<?> getHandler() {
        return handler;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isNullConstraint() {
        return handler == null && nullable;
    }

    @Override
    public String toString() {
        return (handler != null ? TridentTypeManager.getTypeIdentifierForType(handler) : "*") + (nullable ? "?" : "");
    }

    //4: exact match
    //3: subtype match
    //2: coercion match
    //1: null match
    //0: no match
    public int rateMatch(Object value) {
        if(value == null && nullable) return 1;
        if(handler == null && value != null) return 4;
        if(!verify(value)) return 0;
        TypeHandler objectTypeHandler = TridentTypeManager.getStaticHandlerForObject(value);
        if(objectTypeHandler == handler) return 4;
        if(handler.isInstance(value)) return 3;
        if(objectTypeHandler.canCoerce(value, handler)) return 2;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeConstraints that = (TypeConstraints) o;
        return nullable == that.nullable &&
                Objects.equals(handler, that.handler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handler, nullable);
    }

    public static boolean constraintsEqual(TypeConstraints a, TypeConstraints b) {
        if(a == b) return true;
        if(
                (a == null && b.isNullConstraint()) ||
                        (b == null && a.isNullConstraint())
        ) {
            return true;
        }
        return a != null && a.equals(b);
    }

    public static boolean constraintAContainsB(TypeConstraints a, TypeConstraints b) {
        if(constraintsEqual(a, b)) return true;

        if(a == null || a.isNullConstraint() || a == b) return true;
        if(b == null || b.isNullConstraint()) return false;

        TypeHandler<?> handlerA = a.getHandler();
        TypeHandler<?> handlerB = b.getHandler();

        return typeAContainsB(handlerA, handlerB) && (a.isNullable() || !b.isNullable());
    }

    private static boolean typeAContainsB(TypeHandler<?> a, TypeHandler<?> b) {
        if(a == null || a == b) return true;
        if(b == null) return false;

        return a.isSubType(b);
    }
}
