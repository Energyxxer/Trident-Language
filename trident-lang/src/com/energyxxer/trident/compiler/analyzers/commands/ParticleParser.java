package com.energyxxer.trident.compiler.analyzers.commands;

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
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

import java.util.ArrayList;

@AnalyzerMember(key = "particle")
public class ParticleParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Particle particle = parseParticle(pattern.find("PARTICLE"), file);
        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            CoordinateSet pos = CoordinateParser.parse(sub.find("COORDINATE_SET"), file);
            TokenPattern<?> sub1 = sub.find("");
            if(sub1 != null) {
                Delta delta = new Delta(
                        CommonParsers.parseDouble(sub1.find("DELTA.DX"), file),
                        CommonParsers.parseDouble(sub1.find("DELTA.DY"), file),
                        CommonParsers.parseDouble(sub1.find("DELTA.DZ"), file)
                );
                double speed = CommonParsers.parseDouble(sub1.find("SPEED"), file);
                int count = CommonParsers.parseInt(sub1.find("COUNT"), file);
                TokenPattern<?> sub2 = sub1.find("");
                if(sub2 != null) {
                    boolean force = sub2.find("CHOICE").flatten(false).equals("force");
                    Entity entity = EntityParser.parseEntity(sub2.find(".ENTITY"), file);
                    return new ParticleCommand(particle, pos, delta, speed, count, force, entity);
                } else return new ParticleCommand(particle, pos, delta, speed, count);
            } else return new ParticleCommand(particle, pos);
        } else return new ParticleCommand(particle);
    }

    private Particle parseParticle(TokenPattern<?> pattern, TridentFile file) {
        Type particleType = CommonParsers.parseType(pattern.find("PARTICLE_ID"), file, d -> d.particle);
        TokenGroup argsGroup = (TokenGroup) pattern.find("PARTICLE_ARGUMENTS");
        ArrayList<Object> arguments = new ArrayList<>();
        if(argsGroup != null) {
            for(TokenPattern<?> arg : argsGroup.getContents()) {
                switch(arg.getName()) {
                    case "INTEGER":
                        arguments.add(CommonParsers.parseInt(arg, file));
                        break;
                    case "REAL":
                        arguments.add(CommonParsers.parseDouble(arg, file));
                        break;
                    case "COLOR": {
                        arguments.add(new ParticleColor(
                                CommonParsers.parseDouble(arg.find("RED_COMPONENT"), file),
                                CommonParsers.parseDouble(arg.find("GREEN_COMPONENT"), file),
                                CommonParsers.parseDouble(arg.find("BLUE_COMPONENT"), file)
                        ));
                        break;
                    }
                    case "BLOCK":
                        arguments.add(CommonParsers.parseBlock(arg, file));
                        break;
                    case "ITEM":
                        arguments.add(CommonParsers.parseItem(arg, file, NBTMode.SETTING));
                        break;
                    default: {
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + arg.getName() + "'", arg));
                        return null;
                    }
                }
            }
        }

        return new Particle(particleType, arguments.toArray());
    }
}
