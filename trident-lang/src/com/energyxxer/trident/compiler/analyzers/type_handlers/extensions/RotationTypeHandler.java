package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

import java.util.HashMap;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class RotationTypeHandler implements TypeHandler<Rotation> {
    private HashMap<String, PrismarineFunction> members;

    private CustomClassObject ROTATION_TYPE_ABSOLUTE;
    private CustomClassObject ROTATION_TYPE_RELATIVE;

    private final PrismarineTypeSystem typeSystem;

    public RotationTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        if(members != null) return;
        members = new HashMap<>();

        typeSystem.registerUserDefinedTypeListener("trident-util:native@RotationType", customClass -> {
            ROTATION_TYPE_ABSOLUTE = (CustomClassObject) ((CustomClass) customClass).forceGetMember("ABSOLUTE");
            ROTATION_TYPE_RELATIVE = (CustomClassObject) ((CustomClass) customClass).forceGetMember("RELATIVE");
        });

        try {
            members.put("getMagnitude", nativeMethodsToFunction(this.typeSystem, null, RotationTypeHandler.class.getMethod("getMagnitude", CustomClassObject.class, Rotation.class)));
            members.put("getRotationType", nativeMethodsToFunction(this.typeSystem, null, RotationTypeHandler.class.getMethod("getRotationType", CustomClassObject.class, Rotation.class, ISymbolContext.class)));
            members.put("deriveMagnitude", nativeMethodsToFunction(this.typeSystem, null, RotationTypeHandler.class.getMethod("deriveMagnitude", double.class, CustomClassObject.class, Rotation.class)));
            members.put("deriveRotationType", nativeMethodsToFunction(this.typeSystem, null, RotationTypeHandler.class.getMethod("deriveRotationType", CustomClassObject.class, CustomClassObject.class, Rotation.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(Rotation object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        PrismarineFunction result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return new PrismarineFunction.FixedThisFunction(result, object);
    }

    @Override
    public Object getIndexer(Rotation object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Rotation object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }


    public static double getMagnitude(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeFunctionAnnotations.ThisArg Rotation rot) {
        int index = (int)axis.forceGetMember("index");
        switch(index) {
            case 0: return rot.getPitch().getMagnitude();
            case 1: return rot.getYaw().getMagnitude();
            case 2: throw new IllegalArgumentException("Invalid rotation axis argument 'Axis.Z'");
        }
        throw new IllegalArgumentException("Impossible Internal Exception: Invalid index for Axis object: " + index + ". Please report as soon as possible");
    }

    @NativeFunctionAnnotations.NotNullReturn
    @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@RotationType")
    public static CustomClassObject getRotationType(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeFunctionAnnotations.ThisArg Rotation rot, ISymbolContext ctx) {
        int index = (int)axis.forceGetMember("index");
        RotationTypeHandler staticHandler = ctx.getTypeSystem().getHandlerForHandlerClass(RotationTypeHandler.class);
        switch(index) {
            case 0: return staticHandler.rotTypeToConstant(rot.getPitch().getType());
            case 1: return staticHandler.rotTypeToConstant(rot.getYaw().getType());
            case 2: throw new IllegalArgumentException("Invalid rotation axis argument 'Axis.Z'");
        }
        throw new IllegalArgumentException("Impossible Internal Exception: Invalid index for Axis object: " + index + ". Please report as soon as possible");
    }

    public static Rotation deriveMagnitude(
            double newMagnitude,
            @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") @NativeFunctionAnnotations.NullableArg CustomClassObject axis,
            @NativeFunctionAnnotations.ThisArg Rotation rot
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
            @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@RotationType") CustomClassObject type,
            @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") @NativeFunctionAnnotations.NullableArg CustomClassObject axis,
            @NativeFunctionAnnotations.ThisArg Rotation rot
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

    private CustomClassObject rotTypeToConstant(RotationUnit.Type type) {
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
