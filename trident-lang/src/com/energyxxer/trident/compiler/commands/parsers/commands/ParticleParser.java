package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.particle.ParticleCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.Delta;
import com.energyxxer.commodore.util.Particle;
import com.energyxxer.commodore.util.ParticleColor;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.ArrayList;

@ParserMember(key = "particle")
public class ParticleParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Particle particle = parseParticle(pattern.find("PARTICLE"), file.getCompiler());
        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            CoordinateSet pos = CoordinateParser.parse(sub.find("COORDINATE_SET"), file.getCompiler());
            TokenPattern<?> sub1 = sub.find("");
            if(sub1 != null) {
                Delta delta = new Delta(
                        Double.parseDouble(sub1.find("DELTA.DX").flatten(false)),
                        Double.parseDouble(sub1.find("DELTA.DY").flatten(false)),
                        Double.parseDouble(sub1.find("DELTA.DZ").flatten(false))
                );
                double speed = Double.parseDouble(sub1.find("SPEED").flatten(false));
                int count = CommonParsers.parseInt(sub1.find("COUNT"), file.getCompiler());
                TokenPattern<?> sub2 = sub1.find("");
                if(sub2 != null) {
                    boolean force = sub2.find("CHOICE").flatten(false).equals("force");
                    Entity entity = EntityParser.parseEntity(sub2.find(".ENTITY"), file.getCompiler());
                    return new ParticleCommand(particle, pos, delta, speed, count, force, entity);
                } else return new ParticleCommand(particle, pos, delta, speed, count);
            } else return new ParticleCommand(particle, pos);
        } else return new ParticleCommand(particle);
    }

    private Particle parseParticle(TokenPattern<?> pattern, TridentCompiler compiler) {
        Type particleType = CommonParsers.parseType(pattern.find("PARTICLE_ID"), compiler, d -> d.particle);
        TokenGroup argsGroup = (TokenGroup) pattern.find("PARTICLE_ARGUMENTS");
        ArrayList<Object> arguments = new ArrayList<>();
        if(argsGroup != null) {
            for(TokenPattern<?> arg : argsGroup.getContents()) {
                switch(arg.getName()) {
                    case "INTEGER":
                        arguments.add(CommonParsers.parseInt(arg, compiler));
                        break;
                    case "REAL":
                        arguments.add(Double.parseDouble(arg.flatten(false)));
                        break;
                    case "COLOR": {
                        arguments.add(new ParticleColor(
                                Double.parseDouble(arg.find("RED_COMPONENT").flatten(false)),
                                Double.parseDouble(arg.find("GREEN_COMPONENT").flatten(false)),
                                Double.parseDouble(arg.find("BLUE_COMPONENT").flatten(false))
                        ));
                        break;
                    }
                    case "BLOCK":
                        arguments.add(CommonParsers.parseBlock(arg, compiler));
                        break;
                    case "ITEM":
                        arguments.add(CommonParsers.parseItem(arg, compiler));
                        break;
                    default: {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + arg.getName() + "'", arg));
                        return null;
                    }
                }
            }
        }

        return new Particle(particleType, arguments.toArray());
    }
}
