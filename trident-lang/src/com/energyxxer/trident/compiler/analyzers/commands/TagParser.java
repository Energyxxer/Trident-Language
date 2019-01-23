package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.tag.TagCommand;
import com.energyxxer.commodore.functionlogic.commands.tag.TagQueryCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "tag")
public class TagParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        switch(pattern.find("CHOICE").flattenTokens().get(0).value) {
            case "list": return new TagQueryCommand(entity);
            case "add":
                return new TagCommand(TagCommand.Action.ADD, entity, pattern.find("CHOICE").flattenTokens().get(1).value);
            case "remove":
                return new TagCommand(TagCommand.Action.REMOVE, entity, pattern.find("CHOICE").flattenTokens().get(1).value);
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown command node '" + pattern.find("CHOICE").flattenTokens().get(0).value + "'", pattern.find("CHOICE")));
                return null;
            }
        }
    }
}
