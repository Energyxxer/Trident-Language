package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.summon.SummonCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;

@AnalyzerMember(key = "summon")
public class SummonParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        SummonData data = new SummonData(pattern, ctx,
                pattern.find("ENTITY_ID"),
                pattern.find(".COORDINATE_SET"),
                pattern.find("..NBT_COMPOUND"),
                ((TokenList) pattern.find("IMPLEMENTED_COMPONENTS.COMPONENT_LIST")));
        try {
            return data.constructSummon();
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("ENTITY_ID"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    public static class SummonData {
        public Type type;
        public CoordinateSet pos;
        public TagCompound nbt;
        public ArrayList<CustomEntity> components = new ArrayList<>();
        public Object reference;

        public SummonData(Type type, CoordinateSet pos, TagCompound nbt, ArrayList<CustomEntity> components, Object reference) {
            this.type = type;
            this.pos = pos;
            this.nbt = nbt;
            this.components = components;
            this.reference = reference;
        }

        public SummonData(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> idPattern, TokenPattern<?> posPattern, TokenPattern<?> nbtPattern, TokenList componentList) {
            this.pos = CoordinateParser.parse(posPattern, ctx);
            this.nbt = NBTParser.parseCompound(nbtPattern, ctx);
            this.reference = CommonParsers.parseEntityReference(idPattern, ctx);

            if(reference instanceof Type) {
                type = (Type) reference;
            } else if(reference instanceof CustomEntity) {
                CustomEntity ce = (CustomEntity) reference;
                type = ce.getDefaultType();

                if(type == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot summon an entity component: " + ce.getId(), idPattern, ctx);
                }

                if(nbt == null) nbt = new TagCompound();
                try {
                    nbt = ce.getDefaultNBT().merge(nbt);
                } catch(CommodoreException x) {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "Error while merging given NBT with custom entity's NBT: " + x.getMessage(), pattern, ctx);
                }
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), idPattern, ctx);
            }

            if(componentList != null) {
                if (nbt == null) nbt = new TagCompound();
                for (TokenPattern<?> rawComponent : componentList.searchByName("INTERPOLATION_VALUE")) {
                    CustomEntity component = InterpolationManager.parse(rawComponent, ctx, CustomEntity.class);
                    if (component.isComponent()) {
                        nbt = component.getDefaultNBT().merge(nbt);
                    } else {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Expected an entity component here, instead got an entity", rawComponent, ctx);
                    }
                }
            }

            if(nbt != null) {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, type);
                if(nbtPattern == null) nbtPattern = idPattern;
                NBTParser.analyzeTag(nbt, context, nbtPattern, ctx);
            }

            if(pos == null && nbt != null) pos = new CoordinateSet();
        }

        public Command constructSummon() {
            return new SummonCommand(type, pos, nbt);
        }

        public void fillDefaults() {
            if(pos == null) pos = new CoordinateSet();
            if(nbt == null) nbt = new TagCompound();
        }
    }
}