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
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "store")
public class StoreParser implements ModifierParser {
    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentFile file) {
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
                return new ExecuteStoreEntity(storeValue, entity, path, type, scale);
            }
            case "STORE_SCORE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                return new ExecuteStoreScore(new LocalScore(entity, objective));
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
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
