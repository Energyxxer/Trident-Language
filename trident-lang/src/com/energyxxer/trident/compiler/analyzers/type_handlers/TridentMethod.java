package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.logger.Debug;

import java.util.Arrays;
import java.util.stream.Collectors;

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
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().getMember(object, member, pattern, ctx, keepSymbol);
        if(member.equals("formalParameters")) return new ListObject();
        if(member.equals("declaringFile")) return null;
        throw new MemberNotFoundException();
    }

    @Override
    default Object getIndexer(TridentMethod object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().getIndexer(object, index, pattern, ctx, keepSymbol);
        throw new MemberNotFoundException();
    }

    @Override
    default Object cast(TridentMethod object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(this == STATIC_HANDLER) return TridentTypeManager.getTypeHandlerTypeHandler().cast(object, targetType, pattern, ctx);
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
        public static <T> T assertOfClass(Object param, TokenPattern<?> pattern, ISymbolContext ctx, Class<? extends T>... expected) {
            TypeHandler[] expectedTypes = new TypeHandler[expected.length];
            for(int i = 0; i < expected.length; i++) {
                expectedTypes[i] = TridentTypeManager.getHandlerForHandledClass(sanitizeClass(expected[i]));
            }
            param = convertToType(param, pattern, ctx, false, expectedTypes);

            //Ensure of the expected types
            for(Class cls : expected) {
                cls = sanitizeClass(cls);
                if(cls.isInstance(param)) return (T) param;
            }
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Expected one of the following java classes: " + Arrays.stream(expected).map(Class::getSimpleName).collect(Collectors.joining(", ")) + "; Instead got: " + param.getClass().getSimpleName(), pattern, ctx);
        }

        public static void assertOfType(Object value, TokenPattern<?> pattern, ISymbolContext ctx, boolean nullable, TypeHandler... expected) { //no coercion
            if(value == null && nullable) return;
            if(value != null) {
                for(TypeHandler type : expected) {
                    if(type == null) return;
                    if(type.isInstance(value)) return;
                }
            }

            if(expected.length > 1) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected value of one of the following types: " + Arrays.stream(expected).map(TypeHandler::getTypeIdentifier).collect(Collectors.joining(", ")) + "; Instead got " + TridentTypeManager.getTypeIdentifierForObject(value), pattern, ctx);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected value of type " + expected[0].getTypeIdentifier() + "; Instead got " + TridentTypeManager.getTypeIdentifierForObject(value), pattern, ctx);
            }
        }

        @SuppressWarnings("unchecked")
        public static Object convertToType(Object value, TokenPattern<?> pattern, ISymbolContext ctx, boolean nullable, TypeHandler... expected) { //coercion
            if(value == null && nullable) return null;
            if(value != null) {
                TypeHandler valueType = TridentTypeManager.getHandlerForObject(value);

                TypeHandler couldCoerce = null;
                for(TypeHandler type : expected) {
                    if(type == null) return value;
                    if(type.isInstance(value)) return value;
                    if(couldCoerce == null && valueType.getHandledClass().isInstance(value) && valueType.canCoerce(value, type)) couldCoerce = type;
                }

                //not instance of accepted types. Attempt to coerce into the first applicable expected value
                if(couldCoerce != null) {
                    Object coerced = valueType.coerce(value, couldCoerce, pattern, ctx);
                    if(coerced != null) {
                        return coerced;
                    } else {
                        Debug.log("LIES");
                    }
                }
            }

            if(expected.length > 1) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected value of one of the following types: " + Arrays.stream(expected).map(TypeHandler::getTypeIdentifier).collect(Collectors.joining(", ")) + "; Instead got " + TridentTypeManager.getTypeIdentifierForObject(value), pattern, ctx);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected value of type " + expected[0].getTypeIdentifier() + "; Instead got " + TridentTypeManager.getTypeIdentifierForObject(value), pattern, ctx);
            }
        }

        //Java amirite
        public static Class sanitizeClass(Class cls) {
            if(cls == double.class) return Double.class;
            if(cls == int.class) return Integer.class;
            if(cls == boolean.class) return Boolean.class;
            return cls;
        }
    }
}
