package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.recipe.RecipeCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "recipe")
public class RecipeParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        RecipeCommand.Action action = pattern.find("ACTION").flatten(false).equals("take") ? RecipeCommand.Action.TAKE : RecipeCommand.Action.GIVE;
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        String recipe = pattern.find("CHOICE").flatten(false);
        if(!recipe.equals("*")) recipe = new TridentUtil.ResourceLocation(recipe).toString();
        return new RecipeCommand(action, entity, recipe);
    }
}
