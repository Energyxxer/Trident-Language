package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.commodore.util.Axis;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;

import java.util.List;

public class CoordinateParser {
    public static CoordinateSet parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        while (true) {
            if (pattern == null) return null;
            Coordinate x, y, z;
            switch (pattern.getName()) {
                case "TWO_COORDINATE_SET":
                case "COORDINATE_SET": {
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                }
                case "INTERPOLATION_BLOCK": {
                    CoordinateSet result = InterpolationManager.parse(pattern, ctx, CoordinateSet.class);
                    EObject.assertNotNull(result, pattern, ctx);
                    return result;
                }
                case "MIXED_COORDINATE_SET":
                case "LOCAL_COORDINATE_SET":
                    TokenPattern<?>[] triple = ((TokenGroup) pattern).getContents();
                    x = parseCoordinate(triple[0], Axis.X, ctx);
                    y = parseCoordinate(triple[1], Axis.Y, ctx);
                    z = parseCoordinate(triple[2], Axis.Z, ctx);
                    break;
                case "MIXED_TWO_COORDINATE_SET":
                    TokenPattern<?>[] tuple = ((TokenGroup) pattern).getContents();
                    x = parseCoordinate(tuple[0], Axis.X, ctx);
                    y = new Coordinate(Coordinate.Type.RELATIVE, 0);
                    z = parseCoordinate(tuple[1], Axis.Z, ctx);
                    break;
                default:
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
            return new CoordinateSet(x, y, z);
        }
    }

    private static Coordinate parseCoordinate(TokenPattern<?> pattern, Axis axis, ISymbolContext ctx) {
        while (true) {
            List<Token> flattened;
            switch (pattern.getName()) {
                case "MIXABLE_COORDINATE":
                    pattern = (TokenStructure) pattern.getContents();
                    continue;
                case "RELATIVE_COORDINATE":
                    flattened = pattern.flattenTokens();
                    return new Coordinate(Coordinate.Type.RELATIVE, flattened.size() >= 3 ? Double.parseDouble(flattened.get(2).value) : 0);
                case "ABSOLUTE_COORDINATE":
                    return new Coordinate(Coordinate.Type.ABSOLUTE, parseAbsoluteCoordinateMagnitude(pattern.flattenTokens().get(0).value, axis));
                case "LOCAL_COORDINATE":
                    flattened = pattern.flattenTokens();
                    return new Coordinate(Coordinate.Type.LOCAL, flattened.size() >= 3 ? Double.parseDouble(flattened.get(2).value) : 0);
                default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
                }
            }
        }
    }

    private static double parseAbsoluteCoordinateMagnitude(String value, Axis axis) {
        double num = Double.parseDouble(value);
        if(axis != Axis.Y && !value.contains(".")) num += 0.5;
        return num;
    }

    public static Rotation parseRotation(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        pattern = ((TokenStructure) pattern).getContents();
        if(pattern.getName().equals("INTERPOLATION_BLOCK")) {
            return InterpolationManager.parse(pattern, ctx, Rotation.class);
        }
        TokenPattern<?>[] tuple = ((TokenGroup) pattern).getContents();
        RotationUnit yaw =  parseRotationUnit(tuple[0], ctx);
        RotationUnit pitch = parseRotationUnit(tuple[1], ctx);
        return new Rotation(yaw, pitch);
    }

    public static RotationUnit parseRotationUnit(TokenPattern<?> pattern, ISymbolContext ctx) {
        while (true) {
            try {
                switch (pattern.getName()) {
                    case "MIXABLE_COORDINATE":
                        pattern = (TokenStructure) pattern.getContents();
                        continue;
                    case "RELATIVE_COORDINATE":
                        List<Token> flattened = pattern.flattenTokens();
                        return new RotationUnit(RotationUnit.Type.RELATIVE, flattened.size() >= 3 ? Double.parseDouble(flattened.get(2).value) : 0);
                    case "ABSOLUTE_COORDINATE":
                        return new RotationUnit(RotationUnit.Type.ABSOLUTE, Double.parseDouble(pattern.flattenTokens().get(0).value));
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
                    }
                }
            } catch (CommodoreException x) {
                TridentException.handleCommodoreException(x, pattern, ctx)
                        .invokeThrow();
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
            }
        }
    }
}
