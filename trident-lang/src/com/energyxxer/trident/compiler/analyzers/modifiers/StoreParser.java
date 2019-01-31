package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.types.defaults.BossbarReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "store")
public class StoreParser implements SimpleModifierParser {
    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, TridentFile file) {
        ExecuteStore.StoreValue storeValue = ExecuteStore.StoreValue.valueOf(pattern.find("STORE_VALUE").flatten(false).toUpperCase());

        TokenPattern<?> inner = ((TokenStructure) pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "STORE_BLOCK": {
                CoordinateSet pos = CoordinateParser.parse(inner.find("COORDINATE_SET"), file);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), file);
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"), pos, path, file, inner, true);
                double scale = CommonParsers.parseDouble(inner.find("SCALE"), file);
                return new ExecuteStoreBlock(storeValue, pos, path, type, scale);
            }
            case "STORE_BOSSBAR": {
                TridentUtil.ResourceLocation bossbarLoc = new TridentUtil.ResourceLocation(inner.find("RESOURCE_LOCATION").flatten(false));
                BossbarReference bossbar = new BossbarReference(file.getCompiler().getModule().getNamespace(bossbarLoc.namespace), bossbarLoc.body);
                ExecuteStoreBossbar.BossbarVariable bossbarVariable = ExecuteStoreBossbar.BossbarVariable.valueOf(inner.find("BOSSBAR_VARIABLE").flatten(false).toUpperCase());
                return new ExecuteStoreBossbar(storeValue, bossbar, bossbarVariable);
            }
            case "STORE_ENTITY": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), file);
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"), entity, path, file, inner, true);
                double scale = CommonParsers.parseDouble(inner.find("SCALE"), file);
                try {
                    return new ExecuteStoreEntity(storeValue, entity, path, type, scale);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, file)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find("ENTITY"))
                            .invokeThrow();
                }
            }
            case "STORE_SCORE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                return new ExecuteStoreScore(new LocalScore(entity, objective));
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }

    public static NumericNBTType parseNumericType(TokenPattern<?> pattern, Object body, NBTPath path, TridentFile file, TokenPattern<?> outer, boolean strict) {
        if(pattern == null) {
            return CommonParsers.getNumericType(body, path, file, outer, strict);
        }
        return NumericNBTType.valueOf(pattern.flatten(false).toUpperCase());
    }
}
