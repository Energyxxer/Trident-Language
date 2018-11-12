package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.util.Axis;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.util.logger.Debug;

import java.util.List;

public class CoordinateParser {
    public static CoordinateSet parse(TokenPattern<?> pattern) {
        if(pattern == null) return null;
        TokenPattern<?>[] triple = ((TokenGroup) pattern.getContents()).getContents();
        Coordinate x =  parseCoordinate(triple[0], Axis.X);
        Coordinate y =  parseCoordinate(triple[1], Axis.Y);
        Coordinate z =  parseCoordinate(triple[2], Axis.Z);
        return new CoordinateSet(x, y, z);
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
}
