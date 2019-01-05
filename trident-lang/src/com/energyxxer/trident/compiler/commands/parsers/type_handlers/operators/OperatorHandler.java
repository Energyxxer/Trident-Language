package com.energyxxer.trident.compiler.commands.parsers.type_handlers.operators;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.HashMap;

public interface OperatorHandler<A, B> {

    Object perform(A a, B b, TokenPattern<?> pattern, TridentFile file);

    class Static {
        private static HashMap<String, OperatorHandler<?, ?>> handlers = new HashMap<>();

        @SuppressWarnings("unchecked")
        public static Object perform(Object a, Operator operator, Object b, TokenPattern<?> pattern, TridentFile file) {

            String idA = VariableTypeHandler.Static.getIdentifierForClass(a.getClass());
            String idB = VariableTypeHandler.Static.getIdentifierForClass(b.getClass());
            OperatorHandler handler = handlers.get(idA + " " + operator.getSymbol() + " " + idB);

            if(handler == null) handler = handlers.get(idA + " " + operator.getSymbol() + " *");

            if(handler == null) handler = handlers.get("* " + operator.getSymbol() + " " + idB);

            if(handler == null) handler = handlers.get("* " + operator.getSymbol() + " *");

            if(handler == null) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "The operator " + operator.getSymbol() + " is not defined for types " + a.getClass().getSimpleName() + " and " + b.getClass().getSimpleName(), pattern));
                throw new EntryParsingException();
            }
            Object result = handler.perform(a, b, pattern, file);
            if(result == null) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "The operator " + operator.getSymbol() + " is not defined for types " + a.getClass().getSimpleName() + " and " + b.getClass().getSimpleName(), pattern));
                throw new EntryParsingException();
            }
            return result;
        }

        static {
            handlers.put("java.lang.Integer + java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a + b);
            handlers.put("java.lang.Double + java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a + b);
            handlers.put("java.lang.Integer + java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a + b);
            handlers.put("java.lang.Double + java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a + b);

            handlers.put("java.lang.Integer - java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a - b);
            handlers.put("java.lang.Double - java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a - b);
            handlers.put("java.lang.Integer - java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a - b);
            handlers.put("java.lang.Double - java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a - b);

            handlers.put("java.lang.Integer * java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a * b);
            handlers.put("java.lang.Double * java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a * b);
            handlers.put("java.lang.Integer * java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a * b);
            handlers.put("java.lang.Double * java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a * b);

            handlers.put("java.lang.Integer / java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a / b);
            handlers.put("java.lang.Double / java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a / b);
            handlers.put("java.lang.Integer / java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a / b);
            handlers.put("java.lang.Double / java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a / b);

            handlers.put("java.lang.Integer % java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a % b);
            handlers.put("java.lang.Double % java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a % b);
            handlers.put("java.lang.Integer % java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a % b);
            handlers.put("java.lang.Double % java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a % b);

            handlers.put("java.lang.Integer > java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a > b);
            handlers.put("java.lang.Double > java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a > b);
            handlers.put("java.lang.Integer > java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a > b);
            handlers.put("java.lang.Double > java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a > b);

            handlers.put("java.lang.Integer >= java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a >= b);
            handlers.put("java.lang.Double >= java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a >= b);
            handlers.put("java.lang.Integer >= java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a >= b);
            handlers.put("java.lang.Double >= java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a >= b);

            handlers.put("java.lang.Integer < java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a < b);
            handlers.put("java.lang.Double < java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a < b);
            handlers.put("java.lang.Integer < java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a < b);
            handlers.put("java.lang.Double < java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a < b);

            handlers.put("java.lang.Integer <= java.lang.Integer", (Integer a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a <= b);
            handlers.put("java.lang.Double <= java.lang.Integer", (Double a, Integer b, TokenPattern<?> pattern, TridentFile file) -> a <= b);
            handlers.put("java.lang.Integer <= java.lang.Double", (Integer a, Double b, TokenPattern<?> pattern, TridentFile file) -> a <= b);
            handlers.put("java.lang.Double <= java.lang.Double", (Double a, Double b, TokenPattern<?> pattern, TridentFile file) -> a <= b);

            handlers.put("java.lang.Boolean && java.lang.Boolean", (Boolean a, Boolean b, TokenPattern<?> pattern, TridentFile file) -> a && b);
            handlers.put("java.lang.Boolean || java.lang.Boolean", (Boolean a, Boolean b, TokenPattern<?> pattern, TridentFile file) -> a || b);

            handlers.put("java.lang.String + java.lang.String", (OperatorHandler<String, String>) (s, str, pattern, compiler) -> s.concat(str));
            handlers.put("java.lang.String + *", (String a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                VariableTypeHandler handler = b instanceof VariableTypeHandler ? (VariableTypeHandler) b : ParserManager.getParser(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(b.getClass()));
                if(handler == null) {
                    return null;
                }
                String converted = (String)handler.cast(b, String.class, pattern, file);
                return converted != null ? a + converted : null;
            });

            handlers.put("* == *", (Object a, Object b, TokenPattern<?> pattern, TridentFile file) -> a.equals(b));


            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol = *", (Symbol a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                a.setValue(b);
                return b;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol += *", (Symbol a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                Object result = perform(a.getValue(), Operator.ADD, b, pattern, file);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol -= *", (Symbol a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                Object result = perform(a.getValue(), Operator.SUBTRACT, b, pattern, file);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol *= *", (Symbol a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                Object result = perform(a.getValue(), Operator.MULTIPLY, b, pattern, file);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol /= *", (Symbol a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                Object result = perform(a.getValue(), Operator.DIVIDE, b, pattern, file);
                a.setValue(result);
                return result;
            });

            handlers.put("com.energyxxer.trident.compiler.semantics.Symbol %= *", (Symbol a, Object b, TokenPattern<?> pattern, TridentFile file) -> {
                Object result = perform(a.getValue(), Operator.MODULO, b, pattern, file);
                a.setValue(result);
                return result;
            });
        }
    }
}
