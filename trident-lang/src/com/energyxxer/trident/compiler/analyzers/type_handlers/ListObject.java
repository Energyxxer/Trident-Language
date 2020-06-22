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

public class ListObject implements TypeHandler<ListObject>, Iterable<Object>, ContextualToString {
    public static final ListObject STATIC_HANDLER = new ListObject();
    private static Stack<ListObject> toStringRecursion = new Stack<>();

    private static HashMap<String, MemberWrapper<ListObject>> members = new HashMap<>();

    static {
        try {
            members.put("add", new NativeMethodWrapper<>(ListObject.class.getMethod("add", Object.class)));
            members.put("insert", new NativeMethodWrapper<>(ListObject.class.getMethod("insert", Object.class, Integer.class)));
            members.put("remove", new NativeMethodWrapper<>(ListObject.class.getMethod("remove", Integer.class)));
            members.put("contains", new NativeMethodWrapper<>(ListObject.class.getMethod("contains", Object.class)));
            members.put("indexOf", new NativeMethodWrapper<>(ListObject.class.getMethod("indexOf", Object.class)));
            members.put("lastIndexOf", new NativeMethodWrapper<>(ListObject.class.getMethod("lastIndexOf", Object.class)));
            members.put("isEmpty", new NativeMethodWrapper<>(ListObject.class.getMethod("isEmpty")));
            members.put("clear", new NativeMethodWrapper<>(ListObject.class.getMethod("clear")));

            members.put("length", new FieldWrapper<>(ListObject::size));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }



    private ArrayList<Symbol> content = new ArrayList<>();

    public ListObject() {
    }

    public ListObject(Object[] objects) {
        for(Object obj : objects) {
            add(obj);
        }
    }

    public <T> ListObject(Iterable<T> objects) {
        objects.forEach(this::add);
    }

    @Override
    public Object getMember(ListObject object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
        if(member.equals("map")) {
            return (TridentFunction) (params, patterns, pattern1, file1) -> {
                if(params.length < 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'map' requires at least 1 parameter, instead found " + params.length, pattern, ctx);
                }
                TridentUserFunction func = TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], file1, TridentUserFunction.class);

                ListObject newList = new ListObject();

                try {
                    int i = 0;
                    for (Symbol sym : content) {
                        newList.add(func.safeCall(new Object[]{sym.getValue(pattern, ctx), i}, new TokenPattern[]{pattern1, pattern1}, pattern1, file1));
                        i++;
                    }
                } catch(ConcurrentModificationException x) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
                }

                return newList;
            };
        }
        if(member.equals("filter")) {
            return (TridentFunction) (params, patterns, pattern1, file1) -> {
                if(params.length < 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'filter' requires at least 1 parameter, instead found " + params.length, pattern, ctx);
                }
                TridentUserFunction func = TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], file1, TridentUserFunction.class);

                ListObject newList = new ListObject();

                try {
                    int i = 0;
                    for (Symbol sym : content) {
                        Object obj = func.safeCall(new Object[]{sym.getValue(pattern, ctx), i}, new TokenPattern[]{pattern1, pattern1}, pattern1, file1);
                        if(Boolean.TRUE.equals(obj)) {
                            newList.add(sym.getValue(pattern, ctx));
                        }
                        i++;
                    }
                } catch(ConcurrentModificationException x) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
                }

                return newList;
            };
        }


        MemberWrapper<ListObject> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(ListObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        int realIndex = TridentFunction.HelperMethods.assertOfClass(index, pattern, ctx, Integer.class);
        if(realIndex < 0 || realIndex >= object.size()) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + object.size(), pattern, ctx);
        }

        Symbol elem = object.content.get(realIndex);
        return keepSymbol || elem == null ? elem : elem.getValue(pattern, ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object cast(ListObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(ListObject list) {
        return list.iterator();
    }

    public int size() {
        return content.size();
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public Object get(int index) {
        return content.get(index).getValue(null, null);
    }

    public void add(@NativeMethodWrapper.TridentNullableArg Object object) {
        content.add(new Symbol(content.size() + "", Symbol.SymbolVisibility.GLOBAL, object));
    }

    public void insert(@NativeMethodWrapper.TridentNullableArg Object object, Integer index) {
        content.add(index, new Symbol(content.size() + "", Symbol.SymbolVisibility.GLOBAL, object));
    }

    public boolean contains(@NativeMethodWrapper.TridentNullableArg Object object) {
        return content.stream().anyMatch(s -> Objects.equals(s.getValue(null, null), object));
    }

    public int indexOf(@NativeMethodWrapper.TridentNullableArg Object object) {
        int index = 0;
        for(Symbol sym : content) {
            if(Objects.equals(sym.getValue(null, null), object)) return index;
            index++;
        }
        return -1;
    }

    public int lastIndexOf(@NativeMethodWrapper.TridentNullableArg Object object) {
        int index = size()-1;
        for (Iterator<Symbol> it = new ArrayDeque<>(content).descendingIterator(); it.hasNext(); ) {
            Symbol sym = it.next();
            if(Objects.equals(sym.getValue(null, null), object)) return index;
            index--;
        }
        return -1;
    }

    public void clear() {
        content.clear();
    }

    public Symbol remove(Integer index) {
        for(int i = index+1; i < size(); i++) {
            content.get(i).setName((index - 1) + "");
        }
        return content.remove((int) index);
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private Iterator<Symbol> it = content.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return it.next().getValue(null, null);
            }
        };
    }

    @Override
    public String toString() {
        if(toStringRecursion.contains(this)) {
            return "[ ...circular... ]";
        }
        toStringRecursion.push(this);
        String str = "[" + content.stream().map((Symbol s) -> s.getValue(null, null) instanceof String ? "\"" + s.getValue(null, null) + "\"" : InterpolationManager.castToString(s.getValue(null, null))).collect(Collectors.joining(", "))  + "]";
        toStringRecursion.pop();
        return str;
    }

    public String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(toStringRecursion.contains(this)) {
            return "[ ...circular... ]";
        }
        toStringRecursion.push(this);
        String str = "[" + content.stream().map((Symbol s) -> s.getValue(pattern, ctx) instanceof String ? "\"" + s.getValue(pattern, ctx) + "\"" : InterpolationManager.castToString(s.getValue(pattern, ctx), pattern, ctx)).collect(Collectors.joining(", ")) + "]";
        toStringRecursion.pop();
        return str;
    }

    @Override
    public Class<ListObject> getHandledClass() {
        return ListObject.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "list";
    }
}
