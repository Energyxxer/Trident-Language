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
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ListObject implements TypeHandler<ListObject>, Iterable<Object>, ContextualToString {
    private final PrismarineTypeSystem typeSystem;
    private final boolean isStaticHandler;
    private static Stack<ListObject> toStringRecursion = new Stack<>();

    private TypeHandlerMemberCollection<ListObject> members;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.putMethod(ListObject.class.getMethod("add", Object.class));
            members.putMethod(ListObject.class.getMethod("insert", Object.class, Integer.class));
            members.putMethod(ListObject.class.getMethod("remove", Integer.class));
            members.putMethod(ListObject.class.getMethod("contains", Object.class));
            members.putMethod(ListObject.class.getMethod("indexOf", Object.class));
            members.putMethod(ListObject.class.getMethod("lastIndexOf", Object.class));
            members.putMethod(ListObject.class.getMethod("isEmpty"));
            members.putMethod(ListObject.class.getMethod("clear"));

            members.putMethod(ListObject.class.getMethod("map", PrimitivePrismarineFunction.class, TokenPattern.class, ISymbolContext.class));
            members.putMethod(ListObject.class.getMethod("filter", PrimitivePrismarineFunction.class, TokenPattern.class, ISymbolContext.class));
            members.putMethod(ListObject.class.getMethod("reduce", PrimitivePrismarineFunction.class, Object.class, TokenPattern.class, ISymbolContext.class));

            members.putReadOnlyField("length", ListObject::size);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Symbol> content = new ArrayList<>();

    private ListObject(PrismarineTypeSystem typeSystem, boolean isStaticHandler) {
        this.typeSystem = typeSystem;
        this.isStaticHandler = isStaticHandler;
    }

    public ListObject(PrismarineTypeSystem typeSystem) {
        this(typeSystem, false);
    }

    public ListObject(PrismarineTypeSystem typeSystem, Object[] objects) {
        this(typeSystem, false);
        for(Object obj : objects) {
            add(obj);
        }
    }

    public <T> ListObject(PrismarineTypeSystem typeSystem, Iterable<T> objects) {
        this(typeSystem, false);
        objects.forEach(this::add);
    }

    @Override
    public Object getMember(ListObject object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
        return ((ListObject) typeSystem.getStaticHandlerForObject(object)).members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(ListObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        int realIndex = PrismarineTypeSystem.assertOfClass(index, pattern, ctx, Integer.class);
        if(realIndex < 0 || realIndex >= object.size()) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + object.size(), pattern, ctx);
        }

        Symbol elem = object.content.get(realIndex);
        return keepSymbol || elem == null ? elem : elem.getValue(pattern, ctx);
    }

    @Override
    public Object cast(ListObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(isStaticHandler) return typeSystem.getMetaTypeHandler().cast(object, targetType, pattern, ctx);
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(ListObject list, ISymbolContext ctx) {
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

    public void add(@NativeFunctionAnnotations.NullableArg Object object) {
        content.add(new Symbol(content.size() + "", SymbolVisibility.GLOBAL, object));
    }

    public void insert(@NativeFunctionAnnotations.NullableArg Object object, Integer index) {
        content.add(index, new Symbol(content.size() + "", SymbolVisibility.GLOBAL, object));
    }

    public boolean contains(@NativeFunctionAnnotations.NullableArg Object object) {
        return content.stream().anyMatch(s -> Objects.equals(s.getValue(null, null), object));
    }

    public int indexOf(@NativeFunctionAnnotations.NullableArg Object object) {
        int index = 0;
        for(Symbol sym : content) {
            if(Objects.equals(sym.getValue(null, null), object)) return index;
            index++;
        }
        return -1;
    }

    public int lastIndexOf(@NativeFunctionAnnotations.NullableArg Object object) {
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

    public void remove(Integer index) {
        for(int i = index+1; i < size(); i++) {
            content.get(i).setName((index - 1) + "");
        }
        content.remove((int) index);
    }

    public ListObject map(PrimitivePrismarineFunction function, TokenPattern<?> pattern, ISymbolContext ctx) {
        ListObject newList = new ListObject(typeSystem);

        try {
            int i = 0;
            for (Symbol sym : content) {
                newList.add(function.safeCall(new ActualParameterList(new Object[] {sym.getValue(pattern, ctx), i}, null, pattern), ctx, null));
                i++;
            }
        } catch(ConcurrentModificationException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
        }

        return newList;
    }

    public ListObject filter(PrimitivePrismarineFunction function, TokenPattern<?> pattern, ISymbolContext ctx) {
        ListObject newList = new ListObject(typeSystem);

        try {
            int i = 0;
            for (Symbol sym : content) {
                Object obj = function.safeCall(new ActualParameterList(new Object[] {sym.getValue(pattern, ctx), i}, null, pattern), ctx, null);
                if(Boolean.TRUE.equals(obj)) {
                    newList.add(sym.getValue(pattern, ctx));
                }
                i++;
            }
        } catch(ConcurrentModificationException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
        }

        return newList;
    }

    public Object reduce(PrimitivePrismarineFunction function, Object initialValue, TokenPattern<?> pattern, ISymbolContext ctx) {
        Object current = initialValue;
        int currentIndex = 0;
        if(current == null) {
            if(this.isEmpty()) {
                throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Cannot reduce an empty array with no initial value", pattern, ctx);
            } else {
                current = this.get(0);
                currentIndex = 1;
            }
        }

        try {
            for(; currentIndex < size(); currentIndex++) {
                current = function.safeCall(new ActualParameterList(new Object[] {current, this.get(currentIndex), currentIndex}, null, pattern), ctx, null);
            }
        } catch(ConcurrentModificationException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Concurrent modification", pattern, ctx);
        }

        return current;
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
        String str = "[" + content.stream().map((Symbol s) -> s.getValue(null, null) instanceof String ? "\"" + s.getValue(null, null) + "\"" : typeSystem.castToString(s.getValue(null, null))).collect(Collectors.joining(", "))  + "]";
        toStringRecursion.pop();
        return str;
    }

    public String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(toStringRecursion.contains(this)) {
            return "[ ...circular... ]";
        }
        toStringRecursion.push(this);
        String str = "[" + content.stream().map((Symbol s) -> s.getValue(pattern, ctx) instanceof String ? "\"" + s.getValue(pattern, ctx) + "\"" : typeSystem.castToString(s.getValue(pattern, ctx), pattern, ctx)).collect(Collectors.joining(", ")) + "]";
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

    public static ListObject createStaticHandler(PrismarineTypeSystem typeSystem) {
        return new ListObject(typeSystem, true);
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
