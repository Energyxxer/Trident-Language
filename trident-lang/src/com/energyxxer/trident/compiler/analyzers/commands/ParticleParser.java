package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.particle.ParticleCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.ParticleType;
import com.energyxxer.commodore.util.Delta;
import com.energyxxer.commodore.util.Particle;
import com.energyxxer.commodore.util.ParticleColor;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;

@AnalyzerMember(key = "particle")
public class ParticleParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            Particle particle = parseParticle(pattern.find("PARTICLE"), ctx);
            TokenPattern<?> sub = pattern.find("");
            if(sub != null) {
                CoordinateSet pos = CoordinateParser.parse(sub.find("COORDINATE_SET"), ctx);
                TokenPattern<?> sub1 = sub.find("");
                if(sub1 != null) {
                    Delta delta = new Delta(
                            CommonParsers.parseDouble(sub1.find("DELTA.DX"), ctx),
                            CommonParsers.parseDouble(sub1.find("DELTA.DY"), ctx),
                            CommonParsers.parseDouble(sub1.find("DELTA.DZ"), ctx)
                    );
                    double speed = CommonParsers.parseDouble(sub1.find("SPEED"), ctx);
                    int count = CommonParsers.parseInt(sub1.find("COUNT"), ctx);
                    TokenPattern<?> sub2 = sub1.find("");
                    if(sub2 != null) {
                        boolean force = sub2.find("CHOICE").flatten(false).equals("force");
                        Entity entity = EntityParser.parseEntity(sub2.find(".ENTITY"), ctx);
                        return new ParticleCommand(particle, pos, delta, speed, count, force, entity);
                    } else return new ParticleCommand(particle, pos, delta, speed, count);
                } else return new ParticleCommand(particle, pos);
            } else return new ParticleCommand(particle);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("....ENTITY"))
                    .map("SPEED", pattern.find("..SPEED"))
                    .map("COUNT", pattern.find("..COUNT"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    private Particle parseParticle(TokenPattern<?> pattern, ISymbolContext ctx) {
        Type particleType = CommonParsers.parseType(pattern.find("PARTICLE_ID"), ctx, ParticleType.CATEGORY);
        TokenGroup argsGroup = (TokenGroup) pattern.find("PARTICLE_ARGUMENTS");
        ArrayList<Object> arguments = new ArrayList<>();
        if(argsGroup != null) {
            for(TokenPattern<?> arg : argsGroup.getContents()) {
                switch(arg.getName()) {
                    case "INTEGER":
                        arguments.add(CommonParsers.parseInt(arg, ctx));
                        break;
                    case "REAL":
                        arguments.add(CommonParsers.parseDouble(arg, ctx));
                        break;
                    case "COLOR": {
                        arguments.add(new ParticleColor(
                                CommonParsers.parseDouble(arg.find("RED_COMPONENT"), ctx),
                                CommonParsers.parseDouble(arg.find("GREEN_COMPONENT"), ctx),
                                CommonParsers.parseDouble(arg.find("BLUE_COMPONENT"), ctx)
                        ));
                        break;
                    }
                    case "BLOCK":
                        arguments.add(CommonParsers.parseBlock(arg, ctx));
                        break;
                    case "ITEM":
                        arguments.add(CommonParsers.parseItem(arg, ctx, NBTMode.SETTING));
                        break;
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + arg.getName() + "'", arg, ctx);
                    }
                }
            }
        }

        return new Particle(particleType, arguments.toArray());
    }
}
