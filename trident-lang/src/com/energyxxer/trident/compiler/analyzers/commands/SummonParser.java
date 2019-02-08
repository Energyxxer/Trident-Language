package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.summon.SummonCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "summon")
public class SummonParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> id = pattern.find("ENTITY_ID");
        Type type;
        CoordinateSet pos = CoordinateParser.parse(pattern.find(".COORDINATE_SET"), ctx);
        TagCompound nbt = NBTParser.parseCompound(pattern.find("..NBT_COMPOUND"), ctx);

        Object reference = CommonParsers.parseEntityReference(id, ctx);

        if(reference instanceof Type) {
            type = (Type) reference;
        } else if(reference instanceof CustomEntity) {
            CustomEntity ce = (CustomEntity) reference;
            type = ce.getDefaultType();

            if(nbt == null) nbt = new TagCompound();
            try {
                nbt = ce.getDefaultNBT().merge(nbt);
            } catch(CommodoreException x) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Error while merging given NBT with custom entity's NBT: " + x.getMessage(), pattern, ctx);
            }
        } else {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), id, ctx);
        }

        if(nbt != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, type);
            TokenPattern<?> nbtPattern = pattern.find("..NBT_COMPOUND");
            if(nbtPattern == null) nbtPattern = id;
            NBTParser.analyzeTag(nbt, context, nbtPattern, ctx);
        }

        if(pos == null && nbt != null) pos = new CoordinateSet();

        try {
            return new SummonCommand(type, pos, nbt); //TODO handle featueres
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("ENTITY_ID"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}