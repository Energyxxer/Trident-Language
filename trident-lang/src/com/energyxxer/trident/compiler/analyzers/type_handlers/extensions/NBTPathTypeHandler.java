package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfClass;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.path.NBTPath")
public class NBTPathTypeHandler implements TypeHandler<NBTPath> {
    private static final TridentMethod CONSTRUCTOR = (params, patterns, pattern, ctx) -> constructNBTPath(params, patterns, pattern, ctx);

    private static HashMap<String, MemberWrapper<NBTPath>> members = new HashMap<>();

    static {
        members.put("resolveKey", new MethodWrapper<NBTPath>("resolveKey", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTPathKey((String) params[0], (TagCompound) params[1]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, String.class, TagCompound.class).setNullable(1));
        members.put("resolveIndex", new MethodWrapper<>("resolveIndex", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTPathIndex((int) params[0]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, Integer.class));
        members.put("resolveListMatch", new MethodWrapper<NBTPath>("resolveListMatch", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTListMatch((TagCompound) params[0]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, TagCompound.class).setNullable(0));
    }

    @Override
    public Object getMember(NBTPath path, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        MemberWrapper<NBTPath> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(path);
    }

    @Override
    public Object getIndexer(NBTPath object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(NBTPath object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<NBTPath> getHandledClass() {
        return NBTPath.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "nbt_path";
    }

    @Override
    public TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static NBTPath constructNBTPath(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new NBTPath(new NBTListMatch());

        Object obj = assertOfClass(params[0], patterns[0], ctx, String.class, Integer.class, TagCompound.class);
        if(obj instanceof TagCompound) {
            boolean wrapInList = params.length >= 2 && params[1] instanceof Boolean && ((Boolean) params[1]);
            if(wrapInList) {
                return new NBTPath(new NBTListMatch((TagCompound) obj));
            } else {
                return new NBTPath(new NBTPathCompoundRoot((TagCompound) obj));
            }
        }
        if(obj instanceof String) {
            TagCompound compoundMatch = null;
            if(params.length >= 2 && params[1] != null) {
                compoundMatch = TridentMethod.HelperMethods.assertOfClass(params[1], patterns[1], ctx, TagCompound.class);
            }
            return new NBTPath(new NBTPathKey((String) obj, compoundMatch));
        }
        if(obj instanceof Integer) return new NBTPath(new NBTPathIndex((int) obj));
        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
    }
}
