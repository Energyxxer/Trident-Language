package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.energyxxer.trident.extensions.EObject.assertNotNull;

public interface TridentMethod extends TypeHandler<TridentMethod> {
    TridentMethod STATIC_HANDLER = (params, patterns, pattern, ctx) -> null;

    Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx);

    default Object safeCall(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            return call(params, patterns, pattern, ctx);
        } catch(TridentException | TridentException.Grouped x) {
            throw x;
        } catch (Exception x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.toString(), pattern, ctx);
        }
    }

    @Override
    default Object getMember(TridentMethod object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("formalParameters")) return new ListObject();
        if(member.equals("declaringFile")) return null;
        throw new MemberNotFoundException();
    }

    @Override
    default Object getIndexer(TridentMethod object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    default <F> F cast(TridentMethod object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    default Class<TridentMethod> getHandledClass() {
        return TridentMethod.class;
    }

    @Override
    default String getTypeIdentifier() {
        return "function";
    }

    class HelperMethods {

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, ISymbolContext ctx, Class<T> expected) {
            expected = sanitizeClass(expected);
            if(expected.isInstance(param)) return (T) param;
            assertNotNull(param, pattern, ctx);
            TypeHandler handler = TridentTypeManager.getHandlerForObject(param);
            if(handler != null) {
                T coerced = (T) handler.coerce(param, expected, pattern, ctx);
                if(coerced != null) {
                    return coerced;
                } else {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected parameter of type " + TridentTypeManager.getHandlerForHandledClass(expected).getTypeIdentifier(), pattern, ctx);
                }
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown variable handler for '" + param.getClass().getSimpleName() + "'", pattern, ctx);
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, ISymbolContext ctx, Class<? extends T>... expected) {
            for(Class cls : expected) {
                cls = sanitizeClass(cls);
                if(cls.isInstance(param)) return (T) param;
            }
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected parameter of one of the following types: " + Arrays.stream(expected).map((cls) -> TridentTypeManager.getHandlerForHandledClass(cls).getTypeIdentifier()).collect(Collectors.joining(", ")), pattern, ctx);
        }

        //Java amirite
        private static Class sanitizeClass(Class cls) {
            if(cls == double.class) return Double.class;
            if(cls == int.class) return Integer.class;
            return cls;
        }
    }
}
