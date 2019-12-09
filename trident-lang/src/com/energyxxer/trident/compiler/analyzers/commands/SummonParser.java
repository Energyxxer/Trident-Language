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
        SummonData data = parseNewEntityLiteral(pattern.find("NEW_ENTITY_LITERAL"), ctx);
        data.pos = CoordinateParser.parse(pattern.find(".COORDINATE_SET"), ctx);
        data.mergeNBT(NBTParser.parseCompound(pattern.find("..NBT_COMPOUND"), ctx));
        data.analyzeNBT(pattern, ctx);

        try {
            return data.constructSummon();
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("NEW_ENTITY_LITERAL"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    public static SummonData parseNewEntityLiteral(TokenPattern<?> pattern, ISymbolContext ctx) {
        TagCompound nbt = NBTParser.parseCompound(pattern.find("NEW_ENTITY_NBT.NBT_COMPOUND"), ctx);
        Type type;
        Object reference = CommonParsers.parseEntityReference(pattern.find("ENTITY_ID"), ctx);

        if(reference instanceof Type) {
            type = (Type) reference;
        } else if(reference instanceof CustomEntity) {
            CustomEntity ce = (CustomEntity) reference;
            type = ce.getBaseType();

            if(type == null) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot summon an entity component: " + ce.getId(), pattern.find("ENTITY_ID"), ctx);
            }

            if(nbt == null) nbt = new TagCompound();
            try {
                nbt = ce.getDefaultNBT().merge(nbt);
            } catch(CommodoreException x) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Error while merging given NBT with custom entity's NBT: " + x.getMessage(), pattern, ctx);
            }
        } else {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID"), ctx);
        }

        TokenList componentList = ((TokenList) pattern.find("IMPLEMENTED_COMPONENTS.COMPONENT_LIST"));
        ArrayList<CustomEntity> components = new ArrayList<>();
        if(componentList != null) {
            if (nbt == null) nbt = new TagCompound();
            for (TokenPattern<?> rawComponent : componentList.searchByName("INTERPOLATION_VALUE")) {
                CustomEntity component = InterpolationManager.parse(rawComponent, ctx, CustomEntity.class);
                if (component.isComponent()) {
                    nbt = component.getDefaultNBT().merge(nbt);
                    components.add(component);
                } else {
                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Expected an entity component here, instead got an entity", rawComponent, ctx);
                }
            }
        }

        return new SummonData(type, null, nbt, components, reference);
    }

    public static class SummonData {
        public Type type;
        public CoordinateSet pos;
        public TagCompound nbt;
        public ArrayList<CustomEntity> components;
        public Object reference;

        public SummonData(Type type, CoordinateSet pos, TagCompound nbt, ArrayList<CustomEntity> components, Object reference) {
            this.type = type;
            this.pos = pos;
            this.nbt = nbt;
            this.components = components;
            this.reference = reference;
        }

        public void analyzeNBT(TokenPattern<?> pattern, ISymbolContext ctx) {
            if(nbt != null) {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, type);
                NBTParser.analyzeTag(nbt, context, pattern, ctx);
            }
        }

        public Command constructSummon() {
            if(pos == null && nbt != null) pos = new CoordinateSet();
            return new SummonCommand(type, pos, nbt);
        }

        public void fillDefaults() {
            if(pos == null) pos = new CoordinateSet();
            if(nbt == null) nbt = new TagCompound();
        }

        public void mergeNBT(TagCompound nbt) {
            if(nbt == null) return;
            if(this.nbt == null) this.nbt = nbt;
            else this.nbt = this.nbt.merge(nbt);
        }
    }
}