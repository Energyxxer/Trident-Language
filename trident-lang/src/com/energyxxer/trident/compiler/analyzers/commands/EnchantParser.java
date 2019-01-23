package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.enchant.EnchantCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "enchant")
public class EnchantParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        Type enchantment = CommonParsers.parseType(pattern.find("ENCHANTMENT_ID"), file, d -> d.enchantment);
        int level = 1;
        TokenPattern<?> rawLevel = pattern.find("LEVEL");
        if(rawLevel != null) {
            level = CommonParsers.parseInt(rawLevel, file);
        }
        try {
            return new EnchantCommand(entity, enchantment, level);
        } catch(IllegalArgumentException x) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
            return null;
        }
    }
}
