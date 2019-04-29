package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.default_libs.CoordinatesLib;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet")
public class CoordinateTypeHandler implements VariableTypeHandler<CoordinateSet> {
    private static HashMap<String, MemberWrapper<CoordinateSet>> members = new HashMap<>();

    static {
        members.put("getMagnitude", new MethodWrapper<>("getMagnitude", (instance, params) -> {
            switch((int)params[0]) {
                case CoordinatesLib.AXIS_X: {
                    return instance.getX().getCoord();
                }
                case CoordinatesLib.AXIS_Y: {
                    return instance.getY().getCoord();
                }
                case CoordinatesLib.AXIS_Z: {
                    return instance.getZ().getCoord();
                }
                default: {
                    throw new IllegalArgumentException("Invalid axis argument '" + params[0] + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
                }
            }
        }, Integer.class));
        members.put("getCoordinateType", new MethodWrapper<>("getCoordinateType", (instance, params) -> {
            switch((int)params[0]) {
                case CoordinatesLib.AXIS_X: {
                    return CoordinatesLib.coordTypeToConstant(instance.getX().getType());
                }
                case CoordinatesLib.AXIS_Y: {
                    return CoordinatesLib.coordTypeToConstant(instance.getY().getType());
                }
                case CoordinatesLib.AXIS_Z: {
                    return CoordinatesLib.coordTypeToConstant(instance.getZ().getType());
                }
                default: {
                    throw new IllegalArgumentException("Invalid axis argument '" + params[0] + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
                }
            }
        }, Integer.class));
        members.put("deriveMagnitude", new MethodWrapper<CoordinateSet>("deriveMagnitude", (instance, params) -> {
            double magnitude = ((double) params[0]);
            if(params[1] == null) {
                return new CoordinateSet(
                        new Coordinate(instance.getX().getType(), magnitude),
                        new Coordinate(instance.getY().getType(), magnitude),
                        new Coordinate(instance.getZ().getType(), magnitude)
                );
            }
            switch((int)params[1]) {
                case CoordinatesLib.AXIS_X: {
                    return new CoordinateSet(new Coordinate(instance.getX().getType(), magnitude), instance.getY(), instance.getZ());
                }
                case CoordinatesLib.AXIS_Y: {
                    return new CoordinateSet(instance.getX(), new Coordinate(instance.getY().getType(), magnitude), instance.getZ());
                }
                case CoordinatesLib.AXIS_Z: {
                    return new CoordinateSet(instance.getX(), instance.getY(), new Coordinate(instance.getZ().getType(), magnitude));
                }
                default: {
                    throw new IllegalArgumentException("Invalid axis argument '" + params[0] + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
                }
            }
        }, Double.class, Integer.class).setNullable(1));
        members.put("deriveCoordinateType", new MethodWrapper<CoordinateSet>("deriveCoordinateType", (instance, params) -> {
            Coordinate.Type type = CoordinatesLib.constantToCoordType((String) params[0]);
            if(params[1] == null || type == Coordinate.Type.LOCAL || instance.getX().getType() == Coordinate.Type.LOCAL) {
                return new CoordinateSet(
                        new Coordinate(type, instance.getX().getCoord()),
                        new Coordinate(type, instance.getY().getCoord()),
                        new Coordinate(type, instance.getZ().getCoord())
                );
            }
            switch((int)params[1]) {
                case CoordinatesLib.AXIS_X: {
                    return new CoordinateSet(new Coordinate(type, instance.getX().getCoord()), instance.getY(), instance.getZ());
                }
                case CoordinatesLib.AXIS_Y: {
                    return new CoordinateSet(instance.getX(), new Coordinate(type, instance.getY().getCoord()), instance.getZ());
                }
                case CoordinatesLib.AXIS_Z: {
                    return new CoordinateSet(instance.getX(), instance.getY(), new Coordinate(type, instance.getZ().getCoord()));
                }
                default: {
                    throw new IllegalArgumentException("Invalid axis argument '" + params[0] + "'. Use constants Axis.X, Axis.Y and Axis.Z to specify an axis.");
                }
            }
        }, String.class, Integer.class).setNullable(1));
    }

    @Override
    public Object getMember(CoordinateSet coords, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        MemberWrapper<CoordinateSet> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(coords);
    }

    @Override
    public Object getIndexer(CoordinateSet object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(CoordinateSet object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }
}
