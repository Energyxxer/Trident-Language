package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.semantics.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

public class PointerObject implements VariableTypeHandler<PointerObject> {

    public static final int WRONG_TARGET_TYPE = 1;
    public static final int WRONG_MEMBER_TYPE = 2;
    public static final int ILLEGAL_TYPE_COMBINATION = 3;
    public static final int ILLEGAL_OBJECTIVE_NAME = 4;

    @NotNull
    private Symbol target;  // Should contain either: Entity or CoordinateSet
    @NotNull
    private Symbol member;  // Should contain either: NBTPath or String (Identifier A)
    private double scale;
    @Nullable
    private String numericType = null;

    public PointerObject(Object target, Object member) {
        this(target, member, 1, null);
    }

    public PointerObject(Object target, Object member, double scale, @Nullable String numericType) {
        this.target = new Symbol("target", Symbol.SymbolVisibility.LOCAL, target);
        this.member = new Symbol("member", Symbol.SymbolVisibility.LOCAL, member);
        this.scale = scale;
        this.numericType = numericType;
    }

    @Override
    public Object getMember(PointerObject object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        boolean valid = false;
        Object returnValue = null;
        switch(member) {
            case "target": {
                returnValue = this.target;
                valid = true;
                break;
            }
            case "member": {
                returnValue = this.member;
                valid = true;
                break;
            }
            case "scale": {
                returnValue = new AutoPropertySymbol<>("scale", Double.class, () -> scale, v -> {
                    if (v != null) scale = v;
                });
                valid = true;
                break;
            }
            case "numericType": {
                returnValue = new AutoPropertySymbol<>("numericType", String.class, () -> numericType, v -> {
                    try {
                        if (v != null) {
                            NumericNBTType.valueOf(v.toUpperCase(Locale.ENGLISH));
                            numericType = v.toLowerCase(Locale.ENGLISH);
                        } else {
                            numericType = null;
                        }
                    } catch(IllegalArgumentException ignore) {
                    }
                });
                valid = true;
                break;
            }
            case "isLegal": {
                return new MethodWrapper<>("isLegal", (instance, params) -> isLegal());
            }
        }
        if(valid && !keepSymbol) {
            returnValue = ((Symbol) returnValue).getValue();
        }
        if(valid) return returnValue;
        throw new MemberNotFoundException();
    }

    public boolean isLegal() {
        return getErrorCode() == 0;
    }

    public int getErrorCode() {
        if(!(target.getValue() instanceof Entity || target.getValue() instanceof CoordinateSet)) return WRONG_TARGET_TYPE;
        if(!(member.getValue() instanceof String || member.getValue() instanceof NBTPath)) return WRONG_MEMBER_TYPE;
        if(member.getValue() instanceof String &&
                (((String) member.getValue()).length() > 16 ||
                        !TridentLexerProfile.IDENTIFIER_A_REGEX.matcher((String) member.getValue()).matches()))
            return ILLEGAL_OBJECTIVE_NAME;
        if(target.getValue() instanceof CoordinateSet && member.getValue() instanceof String) return ILLEGAL_TYPE_COMBINATION;
        return 0;
    }

    public void setTarget(Object newTarget) {
        target.setValue(newTarget);
    }

    public Object getTarget() {
        return target.getValue();
    }

    public void setMember(Object newMember) {
        member.setValue(newMember);
    }

    public Object getMember() {
        return member.getValue();
    }

    public void setScale(double newScale) {
        scale = newScale;
    }

    public double getScale() {
        return scale;
    }

    public void setNumericType(@Nullable String newNumericType) {
        numericType = newNumericType;
    }

    @Nullable
    public String getNumericType() {
        return numericType;
    }

    @Override
    public Object getIndexer(PointerObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(PointerObject object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        boolean surround = target.getValue() instanceof CoordinateSet;
        StringBuilder sb = new StringBuilder();
        if(surround) sb.append('(');
        sb.append(target.getValue());
        if(surround) sb.append(')');
        else sb.append(' ');
        sb.append(member.getValue() instanceof NBTPath ? "." : "->");
        sb.append(member.getValue());
        if(scale != 1) {
            sb.append(" * ");
            sb.append(CommandUtils.numberToPlainString(scale));
        }
        if(member.getValue() instanceof NBTPath) {
            sb.append(" (");
            sb.append(numericType);
            sb.append(")");
        }
        return sb.toString();
    }

    public PointerObject validate(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(getErrorCode()) {
            case WRONG_TARGET_TYPE: throw new TridentException(TridentException.Source.TYPE_ERROR, "Illegal pointer target type: " + VariableTypeHandler.Static.getShorthandForObject(target.getValue()), pattern, ctx);
            case WRONG_MEMBER_TYPE: throw new TridentException(TridentException.Source.TYPE_ERROR, "Illegal pointer member type: " + VariableTypeHandler.Static.getShorthandForObject(member.getValue()), pattern, ctx);
            case ILLEGAL_OBJECTIVE_NAME: throw new TridentException(TridentException.Source.TYPE_ERROR, "Illegal objective name: '" + member.getValue() + "'", pattern, ctx);
            case ILLEGAL_TYPE_COMBINATION: throw new TridentException(TridentException.Source.TYPE_ERROR, "Illegal pointer type combination: Coordinate and Objective", pattern, ctx);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerObject that = (PointerObject) o;
        return Double.compare(that.scale, scale) == 0 &&
                Objects.equals(target.getValue(), that.target.getValue()) &&
                Objects.equals(member.getValue(), that.member.getValue()) &&
                Objects.equals(numericType, that.numericType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target.getValue(), member.getValue(), scale, numericType);
    }
}
