package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.AutoPropertySymbol;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

public class PointerObject implements TypeHandler<PointerObject> {
    private final PrismarineTypeSystem typeSystem;
    private final boolean isStaticHandler;

    public static final int WRONG_TARGET_TYPE = 1;
    public static final int WRONG_MEMBER_TYPE = 2;
    public static final int ILLEGAL_TYPE_COMBINATION = 3;
    public static final int ILLEGAL_OBJECTIVE_NAME = 4;
    public static final int TARGET_NOT_STANDALONE = 5;

    private TypeHandlerMemberCollection<PointerObject> members = null;

    @NotNull
    private Symbol target;  // Should contain either: Entity, CoordinateSet or ResourceLocation
    @NotNull
    private Symbol member;  // Should contain either: NBTPath or String (Identifier A)
    private Symbol scale;
    @Nullable
    private NumericNBTType numericType = null;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        members.putSymbol("target", p -> p.target);
        members.putSymbol("member", p -> p.member);
        members.putSymbol("scale", p -> p.scale);
        members.put("numericType", p -> new AutoPropertySymbol<>("numericType", typeSystem, String.class, true,
                () -> p.numericType != null ? p.numericType.name().toLowerCase(Locale.ENGLISH) : null,
                v -> {
                    try {
                        if (v != null) {
                            p.numericType = NumericNBTType.valueOf(v.toUpperCase(Locale.ENGLISH));
                        } else {
                            p.numericType = null;
                        }
                    } catch (IllegalArgumentException ignore) {
                    }
                }
        ));
        try {
            members.putMethod(PointerObject.class.getMethod("isLegal"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private PointerObject(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
        this.isStaticHandler = true;
        this.target = new Symbol("target", TridentSymbolVisibility.LOCAL, null);
        this.member = new Symbol("member", TridentSymbolVisibility.LOCAL, null);
        this.scale = new Symbol("scale", TridentSymbolVisibility.LOCAL, 1.0);
    }

    public PointerObject(PrismarineTypeSystem typeSystem, Object target, Object member) {
        this(typeSystem, target, member, 1, null);
    }

    public PointerObject(PrismarineTypeSystem typeSystem, Object target, Object member, double scale, @Nullable NumericNBTType numericType) {
        this.typeSystem = typeSystem;
        this.target = new Symbol("target", TridentSymbolVisibility.LOCAL, target);
        this.member = new Symbol("member", TridentSymbolVisibility.LOCAL, member);
        this.scale = new Symbol("scale", TridentSymbolVisibility.LOCAL, scale);
        this.numericType = numericType;

        this.scale.setTypeConstraints(new TypeConstraints(typeSystem, typeSystem.getHandlerForHandledClass(Double.class), false));

        this.isStaticHandler = false;
    }

    @Override
    public Object getMember(PointerObject object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
        return ((PointerObject) typeSystem.getStaticHandlerForObject(object)).members.getMember(object, member, pattern, ctx, keepSymbol);
    }

    public boolean isLegal() {
        return getErrorCode() == 0;
    }

    public int getErrorCode() {
        if(!(target.getValue(null, null) instanceof Entity || target.getValue(null, null) instanceof CoordinateSet || target.getValue(null, null) instanceof ResourceLocation)) return WRONG_TARGET_TYPE;
        if(!(member.getValue(null, null) instanceof String || member.getValue(null, null) instanceof NBTPath)) return WRONG_MEMBER_TYPE;
        if(target.getValue(null, null) instanceof ResourceLocation && ((ResourceLocation) target.getValue(null, null)).isTag) return TARGET_NOT_STANDALONE;
        if(member.getValue(null, null) instanceof String) {
            try {
                Objective.assertNameValid((String)member.getValue(null, null));
            } catch(CommodoreException x) {
                return ILLEGAL_OBJECTIVE_NAME;
            }
        }
        if(member.getValue(null, null) instanceof String && !(target.getValue(null, null) instanceof Entity)) return ILLEGAL_TYPE_COMBINATION;
        return 0;
    }

    public void setTarget(Object newTarget) {
        target.setValue(newTarget);
    }

    public Object getTarget() {
        return target.getValue(null, null);
    }

    public void setMember(Object newMember) {
        member.setValue(newMember);
    }

    public Object getMember() {
        return member.getValue(null, null);
    }

    public void setScale(double newScale) {
        scale.setValue(newScale);
    }

    public double getScale() {
        return (double) scale.getValue(null, null);
    }

    public void setNumericType(@Nullable NumericNBTType newNumericType) {
        numericType = newNumericType;
    }

    @Nullable
    public NumericNBTType getNumericType() {
        return numericType;
    }

    @Override
    public Object getIndexer(PointerObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(PointerObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().cast(object, targetType, pattern, ctx);
        return null;
    }

    @Override
    public String toString() {
        boolean surround = target.getValue(null, null) instanceof CoordinateSet;
        StringBuilder sb = new StringBuilder();
        if(surround) sb.append('(');
        sb.append(target.getValue(null, null));
        if(surround) sb.append(')');
        sb.append(member.getValue(null, null) instanceof NBTPath ? (target.getValue(null, null) instanceof ResourceLocation ? "~" : ".") : "->");
        sb.append(member.getValue(null, null));
        if(getScale() != 1) {
            sb.append(" * ");
            sb.append(CommandUtils.numberToPlainString(getScale()));
        }
        if(member.getValue(null, null) instanceof NBTPath && numericType != null) {
            sb.append(" (");
            sb.append(numericType.name().toLowerCase(Locale.ENGLISH));
            sb.append(")");
        }
        return sb.toString();
    }

    public PointerObject validate(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(getErrorCode()) {
            case WRONG_TARGET_TYPE: throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal pointer target type: " + typeSystem.getTypeIdentifierForObject(target.getValue(pattern, ctx)), pattern, ctx);
            case WRONG_MEMBER_TYPE: throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal pointer member type: " + typeSystem.getTypeIdentifierForObject(member.getValue(pattern, ctx)), pattern, ctx);
            case ILLEGAL_OBJECTIVE_NAME: throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal objective name: '" + member.getValue(pattern, ctx) + "'", pattern, ctx);
            case ILLEGAL_TYPE_COMBINATION: throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal pointer type combination: " + (target.getValue(pattern, ctx) instanceof CoordinateSet ? "Coordinate" : "Storage") + " and Objective", pattern, ctx);
            case TARGET_NOT_STANDALONE: throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal pointer target: Storage spaces can't be tags: '" + target.getValue(pattern, ctx) + "'", pattern, ctx);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerObject that = (PointerObject) o;
        return Double.compare(that.getScale(), getScale()) == 0 &&
                Objects.equals(target.getValue(null, null), that.target.getValue(null, null)) &&
                Objects.equals(member.getValue(null, null), that.member.getValue(null, null)) &&
                Objects.equals(getScale(), that.getScale()) &&
                Objects.equals(numericType, that.numericType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target.getValue(null, null), member.getValue(null, null), getScale(), numericType);
    }

    @Override
    public Class<PointerObject> getHandledClass() {
        return PointerObject.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "pointer";
    }

    public static PointerObject createStaticHandler(PrismarineTypeSystem typeSystem) {
        return new PointerObject(typeSystem);
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public boolean isStaticHandler() {
        return isStaticHandler;
    }
}
