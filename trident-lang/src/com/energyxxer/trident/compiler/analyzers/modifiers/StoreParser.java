package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.DataHolderStorage;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.defaults.BossbarReference;
import com.energyxxer.commodore.types.defaults.StorageTarget;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "store")
public class StoreParser implements SimpleModifierParser {
    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        ExecuteStore.StoreValue storeValue = ExecuteStore.StoreValue.valueOf(pattern.find("STORE_VALUE").flatten(false).toUpperCase());

        TokenPattern<?> inner = ((TokenStructure) pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "STORE_STORAGE": {
                TridentUtil.ResourceLocation loc = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), ctx);
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"), null, path, ctx, inner, true);
                double scale = CommonParsers.parseDouble(inner.find("SCALE"), ctx);
                return new ExecuteStoreDataHolder(storeValue, new DataHolderStorage(new StorageTarget(ctx.getCompiler().getModule().getNamespace(loc.namespace), loc.body)), path, type, scale);
            }
            case "STORE_BLOCK": {
                CoordinateSet pos = CoordinateParser.parse(inner.find("COORDINATE_SET"), ctx);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), ctx);
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"), pos, path, ctx, inner, true);
                double scale = CommonParsers.parseDouble(inner.find("SCALE"), ctx);
                return new ExecuteStoreBlock(storeValue, pos, path, type, scale);
            }
            case "STORE_BOSSBAR": {
                TridentUtil.ResourceLocation bossbarLoc = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
                bossbarLoc.assertStandalone(inner.find("RESOURCE_LOCATION"), ctx);
                BossbarReference bossbar = new BossbarReference(ctx.getCompiler().getModule().getNamespace(bossbarLoc.namespace), bossbarLoc.body);
                ExecuteStoreBossbar.BossbarVariable bossbarVariable = ExecuteStoreBossbar.BossbarVariable.valueOf(inner.find("BOSSBAR_VARIABLE").flatten(false).toUpperCase());
                return new ExecuteStoreBossbar(storeValue, bossbar, bossbarVariable);
            }
            case "STORE_ENTITY": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), ctx);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), ctx);
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"), entity, path, ctx, inner, true);
                double scale = CommonParsers.parseDouble(inner.find("SCALE"), ctx);
                try {
                    return new ExecuteStoreEntity(storeValue, entity, path, type, scale);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.tryFind("ENTITY"))
                            .invokeThrow();
                }
            }
            case "STORE_SCORE": {
                LocalScore score = CommonParsers.parseScore(inner.find("SCORE"), ctx);
                return new ExecuteStoreScore(score);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    public static NumericNBTType parseNumericType(TokenPattern<?> pattern, Object body, NBTPath path, ISymbolContext ctx, TokenPattern<?> outer, boolean strict) {
        if(pattern == null) {
            return CommonParsers.getNumericType(body, path, ctx, outer, strict);
        }
        return NumericNBTType.valueOf(pattern.flatten(false).toUpperCase());
    }
}
