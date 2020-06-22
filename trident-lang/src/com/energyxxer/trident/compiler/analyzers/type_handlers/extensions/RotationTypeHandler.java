package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.rotation.Rotation")
public class RotationTypeHandler implements TypeHandler<Rotation> {
    private static HashMap<String, TridentUserFunction> members;

    private static CustomClassObject ROTATION_TYPE_ABSOLUTE;
    private static CustomClassObject ROTATION_TYPE_RELATIVE;

    @Override
    public void staticTypeSetup() {
        if(members != null) return;
        members = new HashMap<>();

        CustomClass.registerStringIdentifiedClassListener("trident-util:native@RotationType", customClass -> {
            ROTATION_TYPE_ABSOLUTE = (CustomClassObject) customClass.forceGetMember("ABSOLUTE");
            ROTATION_TYPE_RELATIVE = (CustomClassObject) customClass.forceGetMember("RELATIVE");
        });

        try {
            members.put("getMagnitude", nativeMethodsToFunction(null, RotationTypeHandler.class.getMethod("getMagnitude", CustomClassObject.class, Rotation.class)));
            members.put("getRotationType", nativeMethodsToFunction(null, RotationTypeHandler.class.getMethod("getRotationType", CustomClassObject.class, Rotation.class)));
            members.put("deriveMagnitude", nativeMethodsToFunction(null, RotationTypeHandler.class.getMethod("deriveMagnitude", double.class, CustomClassObject.class, Rotation.class)));
            members.put("deriveRotationType", nativeMethodsToFunction(null, RotationTypeHandler.class.getMethod("deriveRotationType", CustomClassObject.class, CustomClassObject.class, Rotation.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(Rotation object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        TridentUserFunction result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return new TridentUserFunction.FixedThisFunction(result, object);
    }

    @Override
    public Object getIndexer(Rotation object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Rotation object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }


    public static double getMagnitude(@NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeMethodWrapper.TridentThisArg Rotation rot) {
        int index = (int)axis.forceGetMember("index");
        switch(index) {
            case 0: return rot.getPitch().getMagnitude();
            case 1: return rot.getYaw().getMagnitude();
            case 2: throw new IllegalArgumentException("Invalid rotation axis argument 'Axis.Z'");
        }
        throw new IllegalArgumentException("Impossible Internal Exception: Invalid index for Axis object: " + index + ". Please report as soon as possible");
    }

    public static CustomClassObject getRotationType(@NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeMethodWrapper.TridentThisArg Rotation rot) {
        int index = (int)axis.forceGetMember("index");
        switch(index) {
            case 0: return rotTypeToConstant(rot.getPitch().getType());
            case 1: return rotTypeToConstant(rot.getYaw().getType());
            case 2: throw new IllegalArgumentException("Invalid rotation axis argument 'Axis.Z'");
        }
        throw new IllegalArgumentException("Impossible Internal Exception: Invalid index for Axis object: " + index + ". Please report as soon as possible");
    }

    public static Rotation deriveMagnitude(
            double newMagnitude,
            @NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") @NativeMethodWrapper.TridentNullableArg CustomClassObject axis,
            @NativeMethodWrapper.TridentThisArg Rotation rot
    ) {
        RotationUnit newX = rot.getPitch();
        RotationUnit newY = rot.getYaw();

        int index = -1;
        if(axis != null) {
            index = (int)axis.forceGetMember("index");
        }

        if(index == -1 || index == 0) {
            newX = new RotationUnit(rot.getPitch().getType(), newMagnitude);
        }
        if(index == -1 || index == 1) {
            newY = new RotationUnit(rot.getYaw().getType(), newMagnitude);
        }
        return new Rotation(newY, newX);
    }

    public static Rotation deriveRotationType(
            @NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@RotationType") CustomClassObject type,
            @NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") @NativeMethodWrapper.TridentNullableArg CustomClassObject axis,
            @NativeMethodWrapper.TridentThisArg Rotation rot
    ) {
        RotationUnit newX = rot.getPitch();
        RotationUnit newY = rot.getYaw();

        RotationUnit.Type newRotType = constantToRotType(type);

        int index = -1;
        if(axis != null) {
            index = (int)axis.forceGetMember("index");
        }

        if(index == -1 || index == 0) {
            newX = new RotationUnit(newRotType, rot.getPitch().getMagnitude());
        }
        if(index == -1 || index == 1) {
            newY = new RotationUnit(newRotType, rot.getYaw().getMagnitude());
        }
        return new Rotation(newY, newX);
    }

    @Override
    public Class<Rotation> getHandledClass() {
        return Rotation.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "rotation";
    }

    private static CustomClassObject rotTypeToConstant(RotationUnit.Type type) {
        return type == RotationUnit.Type.RELATIVE ? ROTATION_TYPE_RELATIVE : ROTATION_TYPE_ABSOLUTE;
    }

    private static RotationUnit.Type constantToRotType(CustomClassObject cons) {
        switch((int)cons.forceGetMember("index")) {
            case 0: return RotationUnit.Type.ABSOLUTE;
            case 1: return RotationUnit.Type.RELATIVE;
            default: throw new IllegalArgumentException("Invalid rotation type argument '" + cons + "'. Use constants RotationType.ABSOLUTE, RotationType.RELATIVE and RotationType.LOCAL to specify a rotation type.");
        }
    }
}
