package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class DictionaryObject implements VariableTypeHandler<DictionaryObject>, Iterable<Object> {
    private static Stack<DictionaryObject> toStringRecursion = new Stack<>();
    private static HashMap<String, MemberWrapper<DictionaryObject>> members = new HashMap<>();

    static {
        try {
            members.put("merge", new MethodWrapper<>(DictionaryObject.class.getMethod("merge", DictionaryObject.class)));
            members.put("remove", new MethodWrapper<>(DictionaryObject.class.getMethod("remove", String.class)));
            members.put("clear", new MethodWrapper<>(DictionaryObject.class.getMethod("clear")));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Symbol> map = new HashMap<>();

    @Override
    public Object getMember(DictionaryObject dict, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(keepSymbol && !dict.map.containsKey(member)) {
            put(member, null);
        }
        if(!keepSymbol && !dict.map.containsKey(member)) {
            if(member.equals("map")) {
                return (VariableMethod) (params, patterns, pattern1, file1) -> {
                    if (params.length < 1) {
                        throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'map' requires at least 1 parameter, instead found " + params.length, pattern1, ctx);
                    }
                    FunctionMethod func = assertOfType(params[0], patterns[0], file1, FunctionMethod.class);

                    DictionaryObject newDict = new DictionaryObject();

                    for (Map.Entry<String, Symbol> entry : dict.entrySet()) {
                        newDict.put(entry.getKey(), func.safeCall(new Object[]{entry.getKey(), entry.getValue().getValue()}, new TokenPattern[]{pattern1, pattern1}, pattern1, file1));
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
        return keepSymbol || elem == null ? elem : elem.getValue();
    }

    @Override
    public Object getIndexer(DictionaryObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        String key = assertOfType(index, pattern, ctx, String.class);
        return getMember(object, key, pattern, ctx, keepSymbol);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(DictionaryObject object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
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
        return map.containsKey(key) ? map.get(key).getValue() : null;
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
            newDict.put(entry.getKey(), entry.getValue().getValue());
        }
        for(Map.Entry<String, Symbol> entry : other.entrySet()) {
            newDict.put(entry.getKey(), entry.getValue().getValue());
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
                dict.put("value", entry.getValue().getValue());
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
        String str = "{" + map.values().stream().map((Symbol s) -> s.getName() + ": " + (s.getValue() instanceof String ? "\"" + s.getValue() + "\"" : InterpolationManager.castToString(s.getValue()))).collect(Collectors.joining(", ")) + "}";
        toStringRecursion.pop();
        return str;
    }
}
