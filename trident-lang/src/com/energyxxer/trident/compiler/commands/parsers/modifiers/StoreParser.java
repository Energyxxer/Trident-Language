package com.energyxxer.trident.compiler.commands.parsers.modifiers;

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
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "store")
public class StoreParser implements ModifierParser {
    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        ExecuteStore.StoreValue storeValue = ExecuteStore.StoreValue.valueOf(pattern.find("STORE_VALUE").flatten(false).toUpperCase());

        TokenPattern<?> inner = ((TokenStructure) pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "STORE_BLOCK": {
                CoordinateSet pos = CoordinateParser.parse(inner.find("COORDINATE_SET"), compiler);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"));
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"));
                double scale = Double.parseDouble(inner.find("SCALE").flatten(false));
                return new ExecuteStoreBlock(storeValue, pos, path, type, scale);
            }
            case "STORE_BOSSBAR": {
                TridentUtil.ResourceLocation bossbarLoc = new TridentUtil.ResourceLocation(inner.find("RESOURCE_LOCATION").flatten(false));
                BossbarReference bossbar = new BossbarReference(compiler.getModule().getNamespace(bossbarLoc.namespace), bossbarLoc.body);
                ExecuteStoreBossbar.BossbarVariable bossbarVariable = ExecuteStoreBossbar.BossbarVariable.valueOf(inner.find("BOSSBAR_VARIABLE").flatten(false).toUpperCase());
                return new ExecuteStoreBossbar(storeValue, bossbar, bossbarVariable);
            }
            case "STORE_ENTITY": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), compiler);
                NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"));
                NumericNBTType type = parseNumericType(inner.find("NUMERIC_TYPE"));
                double scale = Double.parseDouble(inner.find("SCALE").flatten(false));
                return new ExecuteStoreEntity(storeValue, entity, path, type, scale);
            }
            case "STORE_SCORE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), compiler);
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), compiler);
                return new ExecuteStoreScore(new LocalScore(entity, objective));
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }

    private static NumericNBTType parseNumericType(TokenPattern<?> pattern) {
        return NumericNBTType.valueOf(pattern.flatten(false).toUpperCase());
    }
}
