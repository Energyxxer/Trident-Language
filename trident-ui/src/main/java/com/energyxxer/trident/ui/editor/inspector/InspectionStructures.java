package com.energyxxer.trident.ui.editor.inspector;

import java.util.ArrayList;

/**
 * Created by User on 1/1/2017.
 */
public class InspectionStructures {
    public final static InspectionStructureMatch UNREACHABLE_CODE;
    public final static InspectionStructureMatch EMPTY_CODE_BLOCK;

    private static ArrayList<InspectionStructureMatch> all = new ArrayList<>();

    static {
        UNREACHABLE_CODE = new InspectionStructureMatch("Unreachable code", InspectionType.ERROR);

        all.add(UNREACHABLE_CODE);

        EMPTY_CODE_BLOCK = new InspectionStructureMatch("Empty code block", InspectionType.SUGGESTION);

        all.add(EMPTY_CODE_BLOCK);
    }

    public static ArrayList<InspectionStructureMatch> getAll() {
        return new ArrayList<>();
    }
}
