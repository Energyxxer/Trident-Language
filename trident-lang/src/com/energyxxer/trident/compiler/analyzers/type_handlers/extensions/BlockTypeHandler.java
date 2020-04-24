package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.block.Blockstate;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.semantics.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Map;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "com.energyxxer.commodore.block.Block")
public class BlockTypeHandler implements TypeHandler<Block> {
    public static final TridentMethod CONSTRUCTOR = BlockTypeHandler::constructBlock;

    @Override
    public Object getMember(Block object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("blockType")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("blockType", TridentUtil.ResourceLocation.class, () -> new TridentUtil.ResourceLocation(object.getBlockType().toString()), value -> {
                if(ctx.getCompiler().getModule().namespaceExists(value.namespace) && ctx.getCompiler().getModule().getNamespace(value.namespace).types.block.exists(value.body)) {
                    object.setBlockType(ctx.getCompiler().getModule().getNamespace(value.namespace).types.block.get(value.body));
                } else {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, value + " is not a valid block type", pattern, ctx);
                }
            });
            return keepSymbol ? property : property.getValue();
        }
        if(member.equals("blockTag")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("blockTag", TagCompound.class, object::getNBT, object::setNbt);
            return keepSymbol ? property : property.getValue();
        }
        if(member.equals("blockState")) {
            AutoPropertySymbol<DictionaryObject> property = new AutoPropertySymbol<>("blockState", DictionaryObject.class, () -> {
                DictionaryObject dict = new DictionaryObject();
                if (object.getBlockstate() == null) return dict;

                for (Map.Entry<String, String> entry : object.getBlockstate().getProperties().entrySet()) {
                    dict.put(entry.getKey(), entry.getValue());
                }

                return dict;
            }, value -> {
                Blockstate newState = new Blockstate();
                for (Map.Entry<String, Symbol> a : value.entrySet()) {
                    if(a.getValue().getValue() != null) {
                        newState.put(a.getKey(), InterpolationManager.cast(a.getValue().getValue(), String.class, pattern, ctx));
                    }
                }
                object.setBlockstate(newState);
            });
            return keepSymbol ? property : property.getValue();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Block object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(Block object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public String getTypeIdentifier() {
        return "block";
    }

    @Override
    public Class<Block> getHandledClass() {
        return Block.class;
    }

    @Override
    public TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static Block constructBlock(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        CommandModule module = ctx.getCompiler().getModule();
        if(params.length == 0 || params[0] == null) return new Block(module.minecraft.types.block.get("air"));
        TridentUtil.ResourceLocation loc = assertOfType(params[0], patterns[0], ctx, TridentUtil.ResourceLocation.class);
        Namespace ns = module.getNamespace(loc.namespace);

        Type type;

        if(loc.isTag) {
            type = ns.tags.blockTags.get(loc.body);
        } else {
            type = ns.types.block.get(loc.body);
        }

        if(type == null) throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Resource location " + params[0] + " is not a valid block type", patterns[0], ctx);

        return new Block(type);
    }
}
