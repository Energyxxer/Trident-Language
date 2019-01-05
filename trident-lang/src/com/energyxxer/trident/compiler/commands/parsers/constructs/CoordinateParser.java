package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.commodore.util.Axis;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

import java.util.List;

public class CoordinateParser {
    public static CoordinateSet parse(TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) return null;
        Coordinate x, y, z;
        switch (pattern.getName()) {
            case "TWO_COORDINATE_SET":
            case "COORDINATE_SET": {
                return parse(((TokenStructure) pattern).getContents(), file);
            }
            case "INTERPOLATION_BLOCK": {
                return InterpolationManager.parse(pattern, file, CoordinateSet.class);
            }
            case "MIXED_COORDINATE_SET":
            case "LOCAL_COORDINATE_SET":
                TokenPattern<?>[] triple = ((TokenGroup) pattern).getContents();
                x = parseCoordinate(triple[0], Axis.X);
                y = parseCoordinate(triple[1], Axis.Y);
                z = parseCoordinate(triple[2], Axis.Z);
                break;
            case "MIXED_TWO_COORDINATE_SET":
                TokenPattern<?>[] tuple = ((TokenGroup) pattern).getContents();
                x = parseCoordinate(tuple[0], Axis.X);
                y = new Coordinate(Coordinate.Type.RELATIVE, 0);
                z = parseCoordinate(tuple[1], Axis.Z);
                break;
            default:
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
        }
        return x != null && y != null && z != null ? new CoordinateSet(x, y, z) : null;
    }

    private static Coordinate parseCoordinate(TokenPattern<?> pattern, Axis axis) {
        List<Token> flattened;
        switch(pattern.getName()) {
            case "MIXABLE_COORDINATE":
                return parseCoordinate((TokenStructure)pattern.getContents(), axis);
            case "RELATIVE_COORDINATE":
                flattened = pattern.flattenTokens();
                return new Coordinate(Coordinate.Type.RELATIVE, flattened.size() >= 3 ? Double.parseDouble(flattened.get(2).value) : 0);
            case "ABSOLUTE_COORDINATE":
                return new Coordinate(Coordinate.Type.ABSOLUTE, parseAbsoluteCoordinateMagnitude(pattern.flattenTokens().get(0).value, axis));
            case "LOCAL_COORDINATE":
                flattened = pattern.flattenTokens();
                return new Coordinate(Coordinate.Type.LOCAL, flattened.size() >= 3 ? Double.parseDouble(flattened.get(2).value) : 0);
            default:
                Debug.log("What is this: " + pattern.getName());
                break;
        }
        return null;
    }

    private static double parseAbsoluteCoordinateMagnitude(String value, Axis axis) {
        double num = Double.parseDouble(value);
        if(axis != Axis.Y && !value.contains(".")) num += 0.5;
        return num;
    }

    public static Rotation parseRotation(TokenPattern<?> pattern) {
        if(pattern == null) return null;
        TokenPattern<?>[] tuple = ((TokenGroup) pattern.getContents()).getContents();
        RotationUnit yaw =  parseRotationUnit(tuple[0]);
        RotationUnit pitch = parseRotationUnit(tuple[1]);
        return new Rotation(yaw, pitch);
    }

    public static RotationUnit parseRotationUnit(TokenPattern<?> pattern) {
        List<Token> flattened;
        switch(pattern.getName()) {
            case "MIXABLE_COORDINATE":
                return parseRotationUnit((TokenStructure)pattern.getContents());
            case "RELATIVE_COORDINATE":
                flattened = pattern.flattenTokens();
                return new RotationUnit(RotationUnit.Type.RELATIVE, flattened.size() >= 3 ? Double.parseDouble(flattened.get(2).value) : 0);
            case "ABSOLUTE_COORDINATE":
                return new RotationUnit(RotationUnit.Type.ABSOLUTE, Double.parseDouble(pattern.flattenTokens().get(0).value));
            default:
                Debug.log("What is this: " + pattern.getName());
                break;
        }
        return null;
    }
}
