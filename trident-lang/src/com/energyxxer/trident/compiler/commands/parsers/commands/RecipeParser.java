package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.recipe.RecipeCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "recipe")
public class RecipeParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        RecipeCommand.Action action = pattern.find("ACTION").flatten(false).equals("take") ? RecipeCommand.Action.TAKE : RecipeCommand.Action.GIVE;
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file.getCompiler());
        String recipe = pattern.find("CHOICE").flatten(false);
        if(!recipe.equals("*")) recipe = new TridentUtil.ResourceLocation(recipe).toString();
        return new RecipeCommand(action, entity, recipe);
    }
}
