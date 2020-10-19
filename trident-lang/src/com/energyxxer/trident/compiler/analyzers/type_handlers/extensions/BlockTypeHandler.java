package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.block.Blockstate;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;

import java.util.Map;

public class BlockTypeHandler implements TypeHandler<Block> {
    public final PrimitivePrismarineFunction CONSTRUCTOR = (params, patterns, pattern, ctx, thisObject) -> constructBlock(params, patterns, pattern, ctx);

    private final PrismarineTypeSystem typeSystem;

    public BlockTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public Object getMember(Block object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("blockType")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("blockType", ResourceLocation.class, () -> new ResourceLocation(object.getBlockType().toString()), value -> {
                if(ctx.get(SetupModuleTask.INSTANCE).namespaceExists(value.namespace) && ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace).types.block.exists(value.body)) {
                    object.setBlockType(ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace).types.block.get(value.body));
                } else {
                    throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, value + " is not a valid block type", pattern, ctx);
                }
            });
            return keepSymbol ? property : property.getValue(pattern, ctx);
        }
        if(member.equals("blockTag")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("blockTag", TagCompound.class, object::getNBT, object::setNbt);
            return keepSymbol ? property : property.getValue(pattern, ctx);
        }
        if(member.equals("blockState")) {
            AutoPropertySymbol<DictionaryObject> property = new AutoPropertySymbol<>("blockState", DictionaryObject.class, () -> {
                DictionaryObject dict = new DictionaryObject(ctx.getTypeSystem());
                if (object.getBlockstate() == null) return dict;

                for (Map.Entry<String, String> entry : object.getBlockstate().getProperties().entrySet()) {
                    dict.put(entry.getKey(), entry.getValue());
                }

                return dict;
            }, value -> {
                Blockstate newState = new Blockstate();
                for (Map.Entry<String, Symbol> a : value.entrySet()) {
                    if(a.getValue().getValue(pattern, ctx) != null) {
                        newState.put(a.getKey(), ctx.getTypeSystem().castToString(a.getValue().getValue(pattern, ctx), pattern, ctx));
                    }
                }
                object.setBlockstate(newState);
            });
            return keepSymbol ? property : property.getValue(pattern, ctx);
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Block object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Block object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
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
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static Block constructBlock(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        CommandModule module = ctx.get(SetupModuleTask.INSTANCE);
        if(params.length == 0 || params[0] == null) return new Block(module.minecraft.types.block.get("air"));
        ResourceLocation loc = PrismarineTypeSystem.assertOfClass(params[0], patterns[0], ctx, ResourceLocation.class);
        Namespace ns = module.getNamespace(loc.namespace);

        Type type;

        if(loc.isTag) {
            type = ns.tags.blockTags.get(loc.body);
        } else {
            type = ns.types.block.get(loc.body);
        }

        if(type == null) throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Resource location " + params[0] + " is not a valid block type", patterns[0], ctx);

        return new Block(type);
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }
}
