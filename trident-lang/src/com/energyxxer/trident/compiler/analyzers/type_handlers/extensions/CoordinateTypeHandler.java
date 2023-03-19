package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
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

public class CoordinateTypeHandler implements TypeHandler<CoordinateSet> {
    private HashMap<String, PrismarineFunction> members;

    private CustomClassObject COORDINATE_TYPE_ABSOLUTE;
    private CustomClassObject COORDINATE_TYPE_RELATIVE;
    private CustomClassObject COORDINATE_TYPE_LOCAL;

    public CustomClassObject AXIS_X;
    public CustomClassObject AXIS_Y;
    public CustomClassObject AXIS_Z;

    private final PrismarineTypeSystem typeSystem;

    public CoordinateTypeHandler(PrismarineTypeSystem typeSystem) {
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
        try {
            members.put("getMagnitude", nativeMethodsToFunction(this.typeSystem, null, CoordinateTypeHandler.class.getMethod("getMagnitude", CustomClassObject.class, CoordinateSet.class)));
            members.put("getCoordinateType", nativeMethodsToFunction(this.typeSystem, null, CoordinateTypeHandler.class.getMethod("getCoordinateType", CustomClassObject.class, CoordinateSet.class, ISymbolContext.class)));
            members.put("deriveMagnitude", nativeMethodsToFunction(this.typeSystem, null, CoordinateTypeHandler.class.getMethod("deriveMagnitude", double.class, CustomClassObject.class, CoordinateSet.class)));
            members.put("deriveCoordinateType", nativeMethodsToFunction(this.typeSystem, null, CoordinateTypeHandler.class.getMethod("deriveCoordinateType", CustomClassObject.class, CustomClassObject.class, CoordinateSet.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        this.typeSystem.registerUserDefinedTypeListener("trident-util:native@CoordinateType", customClass -> {
            COORDINATE_TYPE_ABSOLUTE = (CustomClassObject) ((CustomClass) customClass).forceGetMember("ABSOLUTE");
            COORDINATE_TYPE_RELATIVE = (CustomClassObject) ((CustomClass) customClass).forceGetMember("RELATIVE");
            COORDINATE_TYPE_LOCAL = (CustomClassObject) ((CustomClass) customClass).forceGetMember("LOCAL");
        });
        this.typeSystem.registerUserDefinedTypeListener("trident-util:native@Axis", customClass -> {
            AXIS_X = (CustomClassObject) ((CustomClass) customClass).forceGetMember("X");
            AXIS_Y = (CustomClassObject) ((CustomClass) customClass).forceGetMember("Y");
            AXIS_Z = (CustomClassObject) ((CustomClass) customClass).forceGetMember("Z");
        });
    }

    @Override
    public Object getMember(CoordinateSet coords, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        PrismarineFunction result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return new PrismarineFunction.FixedThisFunction(result, coords);
    }

    @Override
    public Object getIndexer(CoordinateSet object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(CoordinateSet object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<CoordinateSet> getHandledClass() {
        return CoordinateSet.class;
    }

    public static double getMagnitude(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeFunctionAnnotations.ThisArg CoordinateSet coords) {
        int index = (int)axis.forceGetMember("index");
        switch(index) {
            case 0: return coords.getX().getCoord();
            case 1: return coords.getY().getCoord();
            case 2: return coords.getZ().getCoord();
        }
        throw new IllegalArgumentException("Invalid axis argument '" + axis + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
    }

    @NativeFunctionAnnotations.NotNullReturn
    @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@CoordinateType")
    public static CustomClassObject getCoordinateType(@NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeFunctionAnnotations.ThisArg CoordinateSet coords, ISymbolContext ctx) {
        int index = (int)axis.forceGetMember("index");
        CoordinateTypeHandler staticHandler = ctx.getTypeSystem().getHandlerForHandlerClass(CoordinateTypeHandler.class);
        switch(index) {
            case 0: return staticHandler.coordTypeToConstant(coords.getX().getType());
            case 1: return staticHandler.coordTypeToConstant(coords.getY().getType());
            case 2: return staticHandler.coordTypeToConstant(coords.getZ().getType());
        }
        throw new IllegalArgumentException("Invalid axis argument '" + axis + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
    }

    public static CoordinateSet deriveMagnitude(
            double newMagnitude,
            @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") @NativeFunctionAnnotations.NullableArg CustomClassObject axis,
            @NativeFunctionAnnotations.ThisArg CoordinateSet coords
    ) {
        Coordinate newX = coords.getX();
        Coordinate newY = coords.getY();
        Coordinate newZ = coords.getZ();

        int index = -1;
        if(axis != null) {
            index = (int)axis.forceGetMember("index");
        }

        if(index == -1 || index == 0) {
            newX = new Coordinate(coords.getX().getType(), newMagnitude);
        }
        if(index == -1 || index == 1) {
            newY = new Coordinate(coords.getY().getType(), newMagnitude);
        }
        if(index == -1 || index == 2) {
            newZ = new Coordinate(coords.getZ().getType(), newMagnitude);
        }
        return new CoordinateSet(newX, newY, newZ);
    }

    public static CoordinateSet deriveCoordinateType(
            @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@CoordinateType") CustomClassObject type,
            @NativeFunctionAnnotations.UserDefinedTypeObjectArgument(typeIdentifier = "trident-util:native@Axis") @NativeFunctionAnnotations.NullableArg CustomClassObject axis,
            @NativeFunctionAnnotations.ThisArg CoordinateSet coords
    ) {
        Coordinate newX = coords.getX();
        Coordinate newY = coords.getY();
        Coordinate newZ = coords.getZ();

        Coordinate.Type newCoordType = constantToCoordType(type);

        int index = -1;
        if(axis != null) {
            index = (int)axis.forceGetMember("index");
        }

        if(index == -1 || index == 0) {
            newX = new Coordinate(newCoordType, coords.getX().getCoord());
        }
        if(index == -1 || index == 1) {
            newY = new Coordinate(newCoordType, coords.getY().getCoord());
        }
        if(index == -1 || index == 2) {
            newZ = new Coordinate(newCoordType, coords.getZ().getCoord());
        }
        return new CoordinateSet(newX, newY, newZ);
    }

    @Override
    public String getTypeIdentifier() {
        return "coordinates";
    }

    private CustomClassObject coordTypeToConstant(Coordinate.Type type) {
        return type == Coordinate.Type.RELATIVE ? COORDINATE_TYPE_RELATIVE : type == Coordinate.Type.LOCAL ? COORDINATE_TYPE_LOCAL : COORDINATE_TYPE_ABSOLUTE;
    }

    private static Coordinate.Type constantToCoordType(CustomClassObject cons) {
        switch((int)cons.forceGetMember("index")) {
            case 0: return Coordinate.Type.ABSOLUTE;
            case 1: return Coordinate.Type.RELATIVE;
            case 2: return Coordinate.Type.LOCAL;
            default: throw new IllegalArgumentException("Invalid axis argument '" + cons + "'. Use constants CoordinateType.ABSOLUTE, CoordinateType.RELATIVE and CoordinateType.LOCAL to specify a coordinate type.");
        }
    }
}
