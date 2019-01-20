package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class ListType implements VariableTypeHandler<ListType>, Iterable<Object> {
    private static HashMap<String, MemberWrapper<ListType>> members = new HashMap<>();

    static {
        try {
            members.put("add", new MethodWrapper<>(ListType.class.getMethod("add", Object.class)));
            members.put("insert", new MethodWrapper<>(ListType.class.getMethod("insert", Object.class, int.class)));
            members.put("contains", new MethodWrapper<>(ListType.class.getMethod("contains", Object.class)));
            members.put("remove", new MethodWrapper<>(ListType.class.getMethod("remove", int.class)));
            members.put("indexOf", new MethodWrapper<>(ListType.class.getMethod("indexOf", Object.class)));
            members.put("lastIndexOf", new MethodWrapper<>(ListType.class.getMethod("lastIndexOf", Object.class)));
            members.put("isEmpty", new MethodWrapper<>(ListType.class.getMethod("isEmpty")));
            members.put("clear", new MethodWrapper<>(ListType.class.getMethod("clear")));

            members.put("length", new FieldWrapper<>(ListType::size));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }



    private ArrayList<Symbol> content = new ArrayList<>();

    public ListType() {
    }

    public ListType(Object... objects) {
        for(Object obj : objects) {
            add(obj);
        }
    }

    public ListType(Collection<Object> objects) {
        objects.forEach(this::add);
    }

    @Override
    public Object getMember(ListType object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        MemberWrapper<ListType> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(ListType object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        int realIndex = assertOfType(index, pattern, file, Integer.class);
        if(realIndex < 0 || realIndex >= object.content.size()) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Index out of bounds: " + index, pattern));
            throw new EntryParsingException();
        }

        Symbol elem = object.content.get(realIndex);
        return keepSymbol || elem == null ? elem : elem.getValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(ListType object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }

    public int size() {
        return content.size();
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public Object get(int index) {
        return content.get(index).getValue();
    }

    public void add(Object object) {
        content.add(new Symbol(content.size() + "", Symbol.SymbolAccess.GLOBAL, object));
    }

    public void insert(Object object, int index) {
        content.add(index, new Symbol(content.size() + "", Symbol.SymbolAccess.GLOBAL, object));
    }

    public boolean contains(Object object) {
        return content.stream().anyMatch(s -> Objects.equals(s.getValue(), object));
    }

    public int indexOf(Object object) {
        int index = 0;
        for(Symbol sym : content) {
            if(Objects.equals(sym.getValue(), object)) return index;
            index++;
        }
        return -1;
    }

    public int lastIndexOf(Object object) {
        int index = size()-1;
        for (Iterator<Symbol> it = new ArrayDeque<>(content).descendingIterator(); it.hasNext(); ) {
            Symbol sym = it.next();
            if(Objects.equals(sym.getValue(), object)) return index;
            index--;
        }
        return -1;
    }

    public void clear() {
        content.clear();
    }

    public Symbol remove(int index) {
        for(int i = index+1; i < size(); i++) {
            content.get(i).setName((index - 1) + "");
        }
        return content.remove(index);
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
                return it.next().getValue();
            }
        };
    }

    @Override
    public String toString() {
        return "[" + content.stream().map((Symbol s) -> s.getValue() instanceof String ? "\"" + s.getValue() + "\"" : String.valueOf(s.getValue())).collect(Collectors.joining(", "))  + "]";
    }
}
