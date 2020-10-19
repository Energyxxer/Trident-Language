package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setworldspawn.SetWorldSpawnCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class SetWorldSpawnCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"setworldspawn"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("setworldspawn"),
                optional(productions.getOrCreateStructure("COORDINATE_SET"), wrapperOptional(productions.getOrCreateStructure("ROTATION_UNIT")).setName("ANGLE")).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet pos = (CoordinateSet) pattern.findThenEvaluateLazyDefault("INNER.COORDINATE_SET", CoordinateSet::new, ctx);
        RotationUnit angle = (RotationUnit) pattern.findThenEvaluate("INNER.ANGLE", null, ctx);

        return new SetWorldSpawnCommand(pos, angle);
    }
}
