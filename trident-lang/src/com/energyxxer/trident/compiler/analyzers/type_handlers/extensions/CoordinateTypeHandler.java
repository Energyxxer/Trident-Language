package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
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

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet")
public class CoordinateTypeHandler implements TypeHandler<CoordinateSet> {
    private static HashMap<String, TridentUserFunction> members;

    private static CustomClassObject COORDINATE_TYPE_ABSOLUTE;
    private static CustomClassObject COORDINATE_TYPE_RELATIVE;
    private static CustomClassObject COORDINATE_TYPE_LOCAL;

    public static CustomClassObject AXIS_X;
    public static CustomClassObject AXIS_Y;
    public static CustomClassObject AXIS_Z;

    @Override
    public void staticTypeSetup() {
        if(members != null) return;
        members = new HashMap<>();
        try {
            members.put("getMagnitude", nativeMethodsToFunction(null, CoordinateTypeHandler.class.getMethod("getMagnitude", CustomClassObject.class, CoordinateSet.class)));
            members.put("getCoordinateType", nativeMethodsToFunction(null, CoordinateTypeHandler.class.getMethod("getCoordinateType", CustomClassObject.class, CoordinateSet.class)));
            members.put("deriveMagnitude", nativeMethodsToFunction(null, CoordinateTypeHandler.class.getMethod("deriveMagnitude", double.class, CustomClassObject.class, CoordinateSet.class)));
            members.put("deriveCoordinateType", nativeMethodsToFunction(null, CoordinateTypeHandler.class.getMethod("deriveCoordinateType", CustomClassObject.class, CustomClassObject.class, CoordinateSet.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        CustomClass.registerStringIdentifiedClassListener("trident-util:native@CoordinateType", customClass -> {
            COORDINATE_TYPE_ABSOLUTE = (CustomClassObject) customClass.forceGetMember("ABSOLUTE");
            COORDINATE_TYPE_RELATIVE = (CustomClassObject) customClass.forceGetMember("RELATIVE");
            COORDINATE_TYPE_LOCAL = (CustomClassObject) customClass.forceGetMember("LOCAL");
        });
        CustomClass.registerStringIdentifiedClassListener("trident-util:native@Axis", customClass -> {
            AXIS_X = (CustomClassObject) customClass.forceGetMember("X");
            AXIS_Y = (CustomClassObject) customClass.forceGetMember("Y");
            AXIS_Z = (CustomClassObject) customClass.forceGetMember("Z");
        });
    }

    @Override
    public Object getMember(CoordinateSet coords, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        TridentUserFunction result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return new TridentUserFunction.FixedThisFunction(result, coords);
    }

    @Override
    public Object getIndexer(CoordinateSet object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(CoordinateSet object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<CoordinateSet> getHandledClass() {
        return CoordinateSet.class;
    }

    public static double getMagnitude(@NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeMethodWrapper.TridentThisArg CoordinateSet coords) {
        int index = (int)axis.forceGetMember("index");
        switch(index) {
            case 0: return coords.getX().getCoord();
            case 1: return coords.getY().getCoord();
            case 2: return coords.getZ().getCoord();
        }
        throw new IllegalArgumentException("Invalid axis argument '" + axis + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
    }

    public static CustomClassObject getCoordinateType(@NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") CustomClassObject axis, @NativeMethodWrapper.TridentThisArg CoordinateSet coords) {
        int index = (int)axis.forceGetMember("index");
        switch(index) {
            case 0: return coordTypeToConstant(coords.getX().getType());
            case 1: return coordTypeToConstant(coords.getY().getType());
            case 2: return coordTypeToConstant(coords.getZ().getType());
        }
        throw new IllegalArgumentException("Invalid axis argument '" + axis + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
    }

    public static CoordinateSet deriveMagnitude(
            double newMagnitude,
            @NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") @NativeMethodWrapper.TridentNullableArg CustomClassObject axis,
            @NativeMethodWrapper.TridentThisArg CoordinateSet coords
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
            @NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@CoordinateType") CustomClassObject type,
            @NativeMethodWrapper.TridentClassObjectArgument(classIdentifier = "trident-util:native@Axis") @NativeMethodWrapper.TridentNullableArg CustomClassObject axis,
            @NativeMethodWrapper.TridentThisArg CoordinateSet coords
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

    private static CustomClassObject coordTypeToConstant(Coordinate.Type type) {
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
