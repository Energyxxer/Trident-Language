package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.attribute.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.AttributeType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.UUID;

@AnalyzerMember(key = "attribute")
public class AttributeParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
        Type attributeType = CommonParsers.parseType(pattern.find("ATTRIBUTE_ID"), ctx, AttributeType.CATEGORY);

        try {
            TokenPattern<?> subCommandA = ((TokenStructure) pattern.find("SUBCOMMAND")).getContents();
            switch (subCommandA.getName()) {
                case "ATTRIBUTE_GET": {
                    double scale = 1;
                    if(subCommandA.find("SCALE") != null) {
                        scale = CommonParsers.parseInt(subCommandA.find("SCALE"), ctx);
                    }
                    return new AttributeGetCommand(entity, attributeType, scale);
                }
                case "ATTRIBUTE_BASE": {
                    TokenPattern<?> subCommandB = ((TokenStructure) subCommandA.find("SUBCOMMAND")).getContents();
                    switch(subCommandB.getName()) {
                        case "ATTRIBUTE_BASE_GET": {
                            double scale = 1;
                            if(subCommandB.find("SCALE") != null) {
                                scale = CommonParsers.parseDouble(subCommandB.find("SCALE"), ctx);
                            }
                            return new AttributeBaseGetCommand(entity, attributeType, scale);
                        }
                        case "ATTRIBUTE_BASE_SET": {
                            double value = CommonParsers.parseDouble(subCommandB.find("VALUE"), ctx);
                            return new AttributeBaseSetCommand(entity, attributeType, value);
                        }
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + subCommandB.getName() + "'", subCommandB, ctx);
                        }
                    }
                }
                case "ATTRIBUTE_MODIFIER": {
                    TokenPattern<?> subCommandB = ((TokenStructure) subCommandA.find("SUBCOMMAND")).getContents();
                    switch(subCommandB.getName()) {
                        case "ATTRIBUTE_MODIFIER_ADD": {
                            UUID uuid = CommonParsers.parseUUID(subCommandB.find("UUID"), ctx);
                            String name = CommonParsers.parseStringLiteralOrIdentifierA(subCommandB.find("ATTRIBUTE_MODIFIER_NAME.STRING_LITERAL_OR_IDENTIFIER_A"), ctx);
                            double value = CommonParsers.parseDouble(subCommandB.find("VALUE"), ctx);
                            AttributeModifierAddCommand.Operation operation = AttributeModifierAddCommand.Operation.valueOf(subCommandB.find("ATTRIBUTE_MODIFIER_OPERATION").flatten(false).toUpperCase());
                            return new AttributeModifierAddCommand(entity, attributeType, uuid, name, value, operation);
                        }
                        case "ATTRIBUTE_MODIFIER_GET": {
                            UUID uuid = CommonParsers.parseUUID(subCommandB.find("UUID"), ctx);
                            double scale = 1;
                            if(subCommandB.find("SCALE") != null) {
                                scale = CommonParsers.parseDouble(subCommandB.find("SCALE"), ctx);
                            }
                            return new AttributeModifierGetCommand(entity, attributeType, uuid, scale);
                        }
                        case "ATTRIBUTE_MODIFIER_REMOVE": {
                            UUID uuid = CommonParsers.parseUUID(subCommandB.find("UUID"), ctx);
                            return new AttributeModifierRemoveCommand(entity, attributeType, uuid);
                        }
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + subCommandB.getName() + "'", subCommandB, ctx);
                        }
                    }
                }
                default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + subCommandA.getName() + "'", subCommandA, ctx);
                }
            }
        } catch (CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("ATTRIBUTE_ID"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
