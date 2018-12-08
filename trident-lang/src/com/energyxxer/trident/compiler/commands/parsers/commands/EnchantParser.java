package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.enchant.EnchantCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "enchant")
public class EnchantParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file.getCompiler());
        Type enchantment = CommonParsers.parseType(pattern.find("ENCHANTMENT_ID"), file.getCompiler(), d -> d.enchantment);
        int level = 1;
        TokenPattern<?> rawLevel = pattern.find("LEVEL");
        if(rawLevel != null) {
            level = CommonParsers.parseInt(rawLevel, file.getCompiler());
        }
        try {
            return new EnchantCommand(entity, enchantment, level);
        } catch(IllegalArgumentException x) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
            return null;
        }
    }
}
