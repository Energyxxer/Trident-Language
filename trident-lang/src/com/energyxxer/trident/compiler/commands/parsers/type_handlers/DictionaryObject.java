package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class DictionaryObject implements VariableTypeHandler<DictionaryObject>, Iterable<Object> {
    private static Stack<DictionaryObject> toStringRecursion = new Stack<>();

    private HashMap<String, Symbol> map = new HashMap<>();

    @Override
    public Object getMember(DictionaryObject dict, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(keepSymbol && !dict.map.containsKey(member)) {
            put(member, null);
        }
        if(!keepSymbol && !dict.map.containsKey(member)) {
            try {
                switch (member) {
                    case "map":
                        return (VariableMethod) (params, patterns, pattern1, file1) -> {
                            if (params.length < 1) {
                                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'map' requires at least 1 parameter, instead found " + params.length, pattern1, file);
                            }
                            FunctionMethod func = assertOfType(params[0], patterns[0], file1, FunctionMethod.class);

                            DictionaryObject newDict = new DictionaryObject();

                            for (Map.Entry<String, Symbol> entry : dict.entrySet()) {
                                newDict.put(entry.getKey(), func.call(new Object[]{entry.getKey(), entry.getValue().getValue()}, new TokenPattern[]{pattern1, pattern1}, pattern1, file1));
                            }

                            return newDict;
                        };
                    case "merge":
                        return new MethodWrapper<>(DictionaryObject.class.getMethod("merge", DictionaryObject.class)).createForInstance(dict);
                    case "remove":
                        return new MethodWrapper<>(DictionaryObject.class.getMethod("remove", String.class)).createForInstance(dict);
                    case "clear":
                        return new MethodWrapper<>(DictionaryObject.class.getMethod("clear")).createForInstance(dict);
                    case "debug":
                        Debug.log(file.getCompiler().getCallStack().getView());
                        return null;
                }
            } catch(NoSuchMethodException x) {
                Debug.log(x.getMessage());
            }
        }
        Symbol elem = dict.map.get(member);
        return keepSymbol || elem == null ? elem : elem.getValue();
    }

    @Override
    public Object getIndexer(DictionaryObject object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        String key = assertOfType(index, pattern, file, String.class);
        return getMember(object, key, pattern, file, keepSymbol);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(DictionaryObject object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
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
        return map.put(key, new Symbol(key, Symbol.SymbolAccess.GLOBAL, value));
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
        return map.putIfAbsent(key, new Symbol(key, Symbol.SymbolAccess.GLOBAL, value));
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
        String str = "{" + map.values().stream().map((Symbol s) -> s.getName() + ": " + (s.getValue() instanceof String ? "\"" + s.getValue() + "\"" : String.valueOf(s.getValue()))).collect(Collectors.joining(", ")) + "}";
        toStringRecursion.pop();
        return str;
    }
}
