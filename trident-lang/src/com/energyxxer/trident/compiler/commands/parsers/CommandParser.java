package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.CommandParserAnnotation;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;
import org.reflections.Reflections;

import java.util.HashMap;

public interface CommandParser {
    void parse(TokenPattern<?> pattern, TridentFile file);

    class Static {

        private static HashMap<String, CommandParser> parsers = new HashMap<>();

        static {

            Debug.log("Starting command parser caching");
            Reflections r = new Reflections(CommandParser.class.getPackage());
            for(Class<?> cls : r.getTypesAnnotatedWith(CommandParserAnnotation.class)) {

                try {
                    String header = cls.getAnnotation(CommandParserAnnotation.class).headerCommand();
                    Object parser = cls.newInstance();
                    if(!(parser instanceof CommandParser)) {
                        Debug.log("Class marked with CommandParserAnnotation does not implement required interface CommandParser: " + cls, Debug.MessageType.ERROR);
                        continue;
                    }

                    parsers.put(header, (CommandParser) parser);
                    Debug.log("New parser for command '" + header + "': " + parser);
                } catch (InstantiationException | IllegalAccessException x) {
                    Debug.log(x, Debug.MessageType.ERROR);
                }
            }
            Debug.log("Finished command parser caching");
        }

        public static void parse(TokenPattern<?> pattern, TridentFile file) {
            CommandParser parser = parsers.get(pattern.flattenTokens().get(0).value);
            if(parser != null) {
                parser.parse(pattern, file);
            }
        }

        public static void initialize() {}
    }
}
