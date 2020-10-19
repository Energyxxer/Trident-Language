package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.ContextualToString;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DictionaryObject implements TypeHandler<DictionaryObject>, Iterable<Object>, ContextualToString {
    private final PrismarineTypeSystem typeSystem;
    private final boolean isStaticHandler;

    private static Stack<DictionaryObject> toStringRecursion = new Stack<>();
    private TypeHandlerMemberCollection<DictionaryObject> members;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.RETURN_NULL);

        try {
            members.putMethod(DictionaryObject.class.getMethod("map", PrimitivePrismarineFunction.class, TokenPattern.class, ISymbolContext.class));
            members.putMethod(DictionaryObject.class.getMethod("merge", DictionaryObject.class));
            members.putMethod(DictionaryObject.class.getMethod("remove", String.class));
            members.putMethod(DictionaryObject.class.getMethod("clear"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public DictionaryObject(PrismarineTypeSystem typeSystem) {
        this(typeSystem, false);
    }

    private DictionaryObject(PrismarineTypeSystem typeSystem, boolean isStaticHandler) {
        this.typeSystem = typeSystem;
        this.isStaticHandler = isStaticHandler;
    }

    private HashMap<String, Symbol> map = new HashMap<>();

    @Override
    public Object getMember(DictionaryObject dict, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().getMember(dict, member, pattern, ctx, keepSymbol);
        if(keepSymbol && !dict.map.containsKey(member)) {
            put(member, null);
        }
        if(!keepSymbol && !dict.map.containsKey(member)) {
            Object result = ((DictionaryObject) typeSystem.getStaticHandlerForObject(dict)).members.getMember(dict, member, pattern, ctx, keepSymbol);
            if (result != null) {
                return result;
            }
        }
        Symbol elem = dict.map.get(member);
        return keepSymbol || elem == null ? elem : elem.getValue(pattern, ctx);
    }

    @Override
    public Object getIndexer(DictionaryObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        String key = PrismarineTypeSystem.assertOfClass(index, pattern, ctx, String.class);
        return getMember(object, key, pattern, ctx, keepSymbol);
    }

    @Override
    public Object cast(DictionaryObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().cast(object, targetType, pattern, ctx);
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(DictionaryObject dict, ISymbolContext ctx) {
        return dict.iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Object get(String key) {
        return map.containsKey(key) ? map.get(key).getValue(null, null) : null;
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public Object put(String key, Object value) {
        return map.put(key, new Symbol(key, SymbolVisibility.GLOBAL, value));
    }

    public Object remove(String key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<Symbol> values() {
        return map.values();
    }

    public Set<Map.Entry<String, Symbol>> entrySet() {
        return map.entrySet();
    }

    public Object putIfAbsent(String key, Object value) {
        return map.putIfAbsent(key, new Symbol(key, SymbolVisibility.GLOBAL, value));
    }

    public DictionaryObject merge(DictionaryObject other) {
        DictionaryObject newDict = new DictionaryObject(typeSystem);
        for(Map.Entry<String, Symbol> entry : this.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        for(Map.Entry<String, Symbol> entry : other.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        return newDict;
    }

    public DictionaryObject shallowMerge(DictionaryObject other) {
        DictionaryObject newDict = new DictionaryObject(typeSystem);
        for(Map.Entry<String, Symbol> entry : this.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        for(Map.Entry<String, Symbol> entry : other.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        return newDict;
    }

    public DictionaryObject map(PrimitivePrismarineFunction function, TokenPattern<?> pattern, ISymbolContext ctx) {
        DictionaryObject newDict = new DictionaryObject(typeSystem);

        try {
            for (Map.Entry<String, Symbol> entry : this.entrySet()) {
                newDict.put(entry.getKey(), function.safeCall(new Object[]{entry.getKey(), entry.getValue().getValue(pattern, ctx)}, new TokenPattern[]{pattern, pattern}, pattern, ctx, null));
            }
        } catch(ConcurrentModificationException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
        }

        return newDict;
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private Iterator<Map.Entry<String, Symbol>> it = map.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                Map.Entry<String, Symbol> entry = it.next();
                DictionaryObject dict = new DictionaryObject(typeSystem);
                dict.put("key", entry.getKey());
                dict.put("value", entry.getValue().getValue(null, null));
                return dict;
            }
        };
    }

    @Override
    public String toString() {
        if(toStringRecursion.contains(this)) {
            return "{ ...circular... }";
        }
        toStringRecursion.push(this);
        String str = "{" + map.values().stream().map((Symbol s) -> s.getName() + ": " + (s.getValue(null, null) instanceof String ? "\"" + s.getValue(null, null) + "\"" : typeSystem.castToString(s.getValue(null, null)))).collect(Collectors.joining(", ")) + "}";
        toStringRecursion.pop();
        return str;
    }

    public String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(toStringRecursion.contains(this)) {
            return "{ ...circular... }";
        }
        toStringRecursion.push(this);
        String str = "{" + map.values().stream().map((Symbol s) -> s.getName() + ": " + (s.getValue(pattern, ctx) instanceof String ? "\"" + s.getValue(pattern, ctx) + "\"" : typeSystem.castToString(s.getValue(pattern, ctx), pattern, ctx))).collect(Collectors.joining(", ")) + "}";
        toStringRecursion.pop();
        return str;
    }

    @Override
    public Class<DictionaryObject> getHandledClass() {
        return DictionaryObject.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "dictionary";
    }

    public static DictionaryObject createStaticHandler(PrismarineTypeSystem typeSystem) {
        return new DictionaryObject(typeSystem, true);
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public boolean isStaticHandler() {
        return isStaticHandler;
    }
}
