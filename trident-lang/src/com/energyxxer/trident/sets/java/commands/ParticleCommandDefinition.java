package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.particle.ParticleCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.util.Delta;
import com.energyxxer.commodore.util.Particle;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ParticleCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"particle"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("particle"),
                productions.getOrCreateStructure("PARTICLE"),
                optional(
                        productions.getOrCreateStructure("COORDINATE_SET"),
                        optional(
                                group(
                                        TridentProductions.real(productions).setName("DX").addTags("cspn:dx"),
                                        TridentProductions.real(productions).setName("DY").addTags("cspn:dy"),
                                        TridentProductions.real(productions).setName("DZ").addTags("cspn:dz")
                                ).setName("DELTA"),
                                TridentProductions.real(productions).setName("SPEED").addTags("cspn:Speed"),
                                TridentProductions.integer(productions).setName("COUNT").addTags("cspn:Count"),
                                optional(
                                        choice("force", "normal"),
                                        optional(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).addTags("cspn:Viewers")
                                )
                        )
                )
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            Particle particle = (Particle) pattern.find("PARTICLE").evaluate(ctx, null);
            TokenPattern<?> sub = pattern.find("");
            if (sub != null) {
                CoordinateSet pos = (CoordinateSet) sub.find("COORDINATE_SET").evaluate(ctx, null);
                TokenPattern<?> sub1 = sub.find("");
                if (sub1 != null) {
                    Delta delta = new Delta(
                            (double) sub1.find("DELTA.DX").evaluate(ctx, null),
                            (double) sub1.find("DELTA.DY").evaluate(ctx, null),
                            (double) sub1.find("DELTA.DZ").evaluate(ctx, null)
                    );
                    double speed = (double) sub1.find("SPEED").evaluate(ctx, null);
                    int count = (int) sub1.find("COUNT").evaluate(ctx, null);
                    TokenPattern<?> sub2 = sub1.find("");
                    if (sub2 != null) {
                        boolean force = sub2.find("CHOICE").flatten(false).equals("force");
                        Entity entity = (Entity) sub2.findThenEvaluate(".ENTITY", null, ctx, null);
                        return new ParticleCommand(particle, pos, delta, speed, count, force, entity);
                    } else return new ParticleCommand(particle, pos, delta, speed, count);
                } else return new ParticleCommand(particle, pos);
            } else return new ParticleCommand(particle);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("....ENTITY"))
                    .map("SPEED", pattern.tryFind("..SPEED"))
                    .map("COUNT", pattern.tryFind("..COUNT"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
