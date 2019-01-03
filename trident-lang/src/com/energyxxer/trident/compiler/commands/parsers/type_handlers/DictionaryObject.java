package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.Symbol;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class DictionaryObject implements VariableTypeHandler<DictionaryObject> {
    private HashMap<String, Symbol> map = new HashMap<>();

    @Override
    public Object getMember(DictionaryObject object, String member, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        Symbol elem = object.map.get(member);
        return keepSymbol || elem == null ? elem : elem.getValue();
    }

    @Override
    public Object getIndexer(DictionaryObject object, Object index, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        String key = assertOfType(index, pattern, compiler, String.class);

        Symbol elem = object.map.get(key);
        return keepSymbol || elem == null ? elem : elem.getValue();
    }

    @Override
    public Object cast(DictionaryObject object, Class targetType, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(targetType == String.class) return map.toString();
        return null;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Object get(String key) {
        return map.get(key);
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
}
