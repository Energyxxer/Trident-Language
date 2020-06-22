package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DictionaryObject implements TypeHandler<DictionaryObject>, Iterable<Object>, ContextualToString {
    public static final DictionaryObject STATIC_HANDLER = new DictionaryObject();
    private static Stack<DictionaryObject> toStringRecursion = new Stack<>();
    private static HashMap<String, MemberWrapper<DictionaryObject>> members = new HashMap<>();

    static {
        try {
            members.put("merge", new NativeMethodWrapper<>(DictionaryObject.class.getMethod("merge", DictionaryObject.class)));
            members.put("remove", new NativeMethodWrapper<>(DictionaryObject.class.getMethod("remove", String.class)));
            members.put("clear", new NativeMethodWrapper<>(DictionaryObject.class.getMethod("clear")));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Symbol> map = new HashMap<>();

    @Override
    public Object getMember(DictionaryObject dict, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().getMember(dict, member, pattern, ctx, keepSymbol);
        if(keepSymbol && !dict.map.containsKey(member)) {
            put(member, null);
        }
        if(!keepSymbol && !dict.map.containsKey(member)) {
            if(member.equals("map")) {
                return (TridentFunction) (params, patterns, pattern1, file1) -> {
                    if (params.length < 1) {
                        throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'map' requires at least 1 parameter, instead found " + params.length, pattern1, ctx);
                    }
                    TridentUserFunction func = TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], file1, TridentUserFunction.class);

                    DictionaryObject newDict = new DictionaryObject();

                    try {
                        for (Map.Entry<String, Symbol> entry : dict.entrySet()) {
                            newDict.put(entry.getKey(), func.safeCall(new Object[]{entry.getKey(), entry.getValue().getValue(pattern, ctx)}, new TokenPattern[]{pattern1, pattern1}, pattern1, file1));
                        }
                    } catch(ConcurrentModificationException x) {
                        throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
                    }

                    return newDict;
                };
            }

            MemberWrapper<DictionaryObject> result = members.get(member);
            if (result != null) {
                return result.unwrap(dict);
            }
        }
        Symbol elem = dict.map.get(member);
        return keepSymbol || elem == null ? elem : elem.getValue(pattern, ctx);
    }

    @Override
    public Object getIndexer(DictionaryObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        String key = TridentFunction.HelperMethods.assertOfClass(index, pattern, ctx, String.class);
        return getMember(object, key, pattern, ctx, keepSymbol);
    }

    @Override
    public Object cast(DictionaryObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(DictionaryObject dict) {
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
        return map.put(key, new Symbol(key, Symbol.SymbolVisibility.GLOBAL, value));
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
        return map.putIfAbsent(key, new Symbol(key, Symbol.SymbolVisibility.GLOBAL, value));
    }

    public DictionaryObject merge(DictionaryObject other) {
        DictionaryObject newDict = new DictionaryObject();
        for(Map.Entry<String, Symbol> entry : this.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        for(Map.Entry<String, Symbol> entry : other.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        return newDict;
    }

    public DictionaryObject shallowMerge(DictionaryObject other) {
        DictionaryObject newDict = new DictionaryObject();
        for(Map.Entry<String, Symbol> entry : this.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
        }
        for(Map.Entry<String, Symbol> entry : other.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue(null, null));
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
                DictionaryObject dict = new DictionaryObject();
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
        String str = "{" + map.values().stream().map((Symbol s) -> s.getName() + ": " + (s.getValue(null, null) instanceof String ? "\"" + s.getValue(null, null) + "\"" : InterpolationManager.castToString(s.getValue(null, null)))).collect(Collectors.joining(", ")) + "}";
        toStringRecursion.pop();
        return str;
    }

    public String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(toStringRecursion.contains(this)) {
            return "{ ...circular... }";
        }
        toStringRecursion.push(this);
        String str = "{" + map.values().stream().map((Symbol s) -> s.getName() + ": " + (s.getValue(pattern, ctx) instanceof String ? "\"" + s.getValue(pattern, ctx) + "\"" : InterpolationManager.castToString(s.getValue(pattern, ctx), pattern, ctx))).collect(Collectors.joining(", ")) + "}";
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
}
