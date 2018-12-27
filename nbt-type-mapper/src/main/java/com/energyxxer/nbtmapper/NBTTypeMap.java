package com.energyxxer.nbtmapper;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.parser.NBTTMLexerProfile;
import com.energyxxer.nbtmapper.parser.NBTTMProductions;
import com.energyxxer.nbtmapper.tags.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class NBTTypeMap {
    public final NBTTypeMap.Parsing parsing = this.new Parsing();
    private CommandModule module;
    private ArrayList<Notice> notices = new ArrayList<>();

    private HashMap<String, ArrayList<DeepDataType>> rootTypes = new HashMap<>();

    public NBTTypeMap(CommandModule module) {
        this.module = module;
    }

    private void addType(String typeName, DeepDataType type) {
        if(!rootTypes.containsKey(typeName)) {
            rootTypes.put(typeName, new ArrayList<>());
        }
        rootTypes.get(typeName).add(type);
    }

    public void collectDataTypeFor(String rootName, PathContext context, NBTPath path, DataTypeQueryResponse response) {
        if(rootTypes.containsKey(rootName)) {
            for(DeepDataType type : rootTypes.get(rootName)) {
                type.collectDataTypeFor(context, path, response);
            }
        }
    }

    private static final String ENTITY_ROOT = "ENTITY";
    private static final String ENTITY_BREEDABLE_ROOT = "ENTITY_BREEDABLE";
    private static final String ENTITY_TAMABLE_ROOT = "ENTITY_TAMABLE";
    private static final String ENTITY_MOB_ROOT = "ENTITY_MOB";

    private String getRootForEntity(Type type) {
        return "ENTITY_" + type.toString().toUpperCase().replace(':','_');
    }

    public DataTypeQueryResponse collectTypeInformation(NBTPath path, PathContext context) {
        DataTypeQueryResponse response = new DataTypeQueryResponse();
        ArrayList<String> rootsToCheck = new ArrayList<>();
        if(context.getProtocol() == PathProtocol.ENTITY) {
            rootsToCheck.add(ENTITY_ROOT);
            Type suspectedType = (Type) context.getProtocolMetadata();
            if(suspectedType != null) {
                if(suspectedType.getCategory().equals(EntityType.CATEGORY)) {
                    if("true".equals(suspectedType.getProperty("living"))) rootsToCheck.add(0, ENTITY_MOB_ROOT);
                    if("true".equals(suspectedType.getProperty("breedable"))) rootsToCheck.add(0, ENTITY_BREEDABLE_ROOT);
                    if("true".equals(suspectedType.getProperty("tamable"))) rootsToCheck.add(0, ENTITY_TAMABLE_ROOT);
                    rootsToCheck.add(0, getRootForEntity(suspectedType));
                }
            } else {
                rootsToCheck.add(0, ENTITY_MOB_ROOT);
                rootsToCheck.add(0, ENTITY_BREEDABLE_ROOT);
                rootsToCheck.add(0, ENTITY_TAMABLE_ROOT);
                module.getAllNamespaces().forEach(n -> n.types.entity.list().forEach(e -> rootsToCheck.add(0, getRootForEntity(e))));
            }
        }
        for(String rootName : rootsToCheck) {
            collectDataTypeFor(rootName, context, path, response);
        }
        return response;
    }

    public class Parsing {
        public void parseNBTTMFile(File file, String content) {
            TokenStream ts = new TokenStream();
            LazyLexer lex = new LazyLexer(ts, NBTTMProductions.FILE);
            lex.tokenizeParse(file, content, new NBTTMLexerProfile(module));

            notices.addAll(lex.getNotices());

            TokenMatchResponse response = lex.getMatchResponse();

            if(response != null && response.matched && lex.getNotices().isEmpty()) {
                TokenPattern<?> pattern = response.pattern;
                TokenList entryList = (TokenList)pattern.find("ENTRIES");
                if(entryList != null) {
                    for(TokenPattern<?> entry : entryList.getContents()) {
                        if(entry.getName().equals("ENTRY") && ((TokenStructure) entry).getContents().getName().equals("ROOT_TYPE")) {
                            String typeName = entry.find("ROOT_TYPE.TYPE_NAME").flatten(false).substring(1);

                            DataType type = parseType(entry.find("ROOT_TYPE.TYPE"));

                            addType(typeName, (DeepDataType) type);
                        }
                    }
                }
            }
        }

        private DataType parseType(TokenPattern<?> pattern) {
            TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
            switch(inner.getName()) {
                case "PRIMITIVE": {
                    DataType type = new FlatType(NBTTypeMap.this, inner.find("PRIMITIVE_NAME").flatten(false));
                    type.setFlags(parseFlags(inner.find("FLAGS")));
                    return type;
                }
                case "COMPOUND": {
                    CompoundType compound = new CompoundType(NBTTypeMap.this);
                    TokenList innerList = (TokenList) inner.find("COMPOUND_INNER_LIST");
                    if(innerList != null) {
                        for(TokenPattern<?> entry : innerList.getContents()) {
                            if(entry.getName().equals("COMPOUND_INNER")) {
                                String key = parseKey(entry.find("KEY"));
                                DataType value = parseType(entry.find("TYPE"));
                                if(key == null) {
                                    compound.setDefaultType(value);
                                } else if(value.getFlags() != null && value.getFlags().hasFlag("volatile")) {
                                    compound.putVolatile(key, value);
                                } else {
                                    compound.put(key, value);
                                }
                            }
                        }
                    }
                    return compound;
                }
                case "LIST": {
                    return new ListType(NBTTypeMap.this, parseType(inner.find("TYPE")));
                }
                case "REFERENCE": {
                    return new ReferenceType(NBTTypeMap.this, inner.find("REFERENCE_NAME").flatten(false).substring(1), parseFlags(inner.find("FLAGS")));
                }
                default: {
                    notices.add(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                    throw new RuntimeException("Unknown grammar branch; details in notices");
                }
            }
        }

        private TypeFlags parseFlags(TokenPattern<?> pattern) {
            if(pattern == null) return null;
            TypeFlags flags = new TypeFlags();

            TokenList flagList = ((TokenList) pattern.find("FLAG_LIST"));
            if(flagList != null) {
                for(TokenPattern<?> flag : flagList.getContents()) {
                    if(flag.getName().equals("FLAG")) {
                        TokenPattern<?> inner = ((TokenStructure) flag).getContents();
                        switch(inner.getName()) {
                            case "TYPE_FLAG": {
                                String category = inner.find("DEFINITION_CATEGORY").flatten(false);
                                if(inner.find("IS_TAG") != null) category = "#" + category;
                                flags.putTypeCategory(category);
                                break;
                            }
                            case "ONE_OF_FLAG": {
                                TokenList optionList = ((TokenList) inner.find("OPTION_LIST"));
                                if(optionList != null) {
                                    for(TokenPattern<?> option : optionList.getContents()) {
                                        if(option.getName().equals("OPTION")) {
                                            flags.putStringOption(option.flatten(false));
                                        }
                                    }
                                }
                                break;
                            }
                            default: {
                                flags.putFlag(inner.flatten(false));
                            }
                        }
                    }
                }
            }
            return flags;
        }

        private String parseKey(TokenPattern<?> pattern) {
            String raw = pattern.flatten(false);
            if(raw.equals("*")) return null;
            if(raw.startsWith('"')) {
                return CommandUtils.parseQuotedString(raw);
            }
            return raw;
        }
    }
}