package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.sets.MinecraftLiteralSet;
import com.energyxxer.trident.worker.tasks.SetupPropertiesTask;
import com.energyxxer.trident.worker.tasks.SetupTypeMapTask;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.*;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.energyxxer.nbtmapper.tags.PathProtocol.STORAGE;

public class NBTInspector {
    /**
     * NBTInspector should not be instantiated.
     * */
    private NBTInspector() {
    }

    public static void comparePaths(NBTPath pathA, PathContext contextA, NBTPath pathB, PathContext contextB, TokenPattern<?> pattern, ISymbolContext ctx) {
        JsonObject properties = ctx.get(SetupPropertiesTask.INSTANCE);
        ReportDelegate delegate = new ReportDelegate(ctx, properties.has("strict-nbt") &&
                properties.get("strict-nbt").isJsonPrimitive() &&
                properties.get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                properties.get("strict-nbt").getAsBoolean(), pattern);

        DataTypeQueryResponse responseA = ctx.get(SetupTypeMapTask.INSTANCE).collectTypeInformation(pathA, contextA);
        DataTypeQueryResponse responseB = ctx.get(SetupTypeMapTask.INSTANCE).collectTypeInformation(pathB, contextB);

        if(responseA.isEmpty()) {
            if(delegate.strict) {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown data type for path '" + pathA + "'. Path context: " + contextA + ". Consider adding it to the type map", pattern));
            }
        } else if(responseB.isEmpty()) {
            if(delegate.strict) {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown data type for path '" + pathB + "'. Path context: " + contextB + ". Consider adding it to the type map", pattern));
            }
        } else {
            for(DataType typeA : responseA.getPossibleTypes()) {
                for(DataType typeB : responseB.getPossibleTypes()) {
                    if(typeA.getCorrespondingTagType() == typeB.getCorrespondingTagType()) {
                        //match found, all good
                        return;
                    }
                }
            }
            if(responseA.getPossibleTypes().size() > 1) {
                delegate.report("Data type at path '" + pathA + "' %s be one of the following: " + responseA.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")) + "; got " + responseB.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")));
            } else {
                delegate.report("Data type at path '" + pathA + "' %s be of type " + responseA.getPossibleTypes().toArray(new DataType[0])[0].getShortTypeName() + "; got " + responseB.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")));
            }
        }
    }

    public static void inspectTag(NBTTag tag, PathContext context, NBTPath path, TokenPattern<?> pattern, ISymbolContext ctx) {
        JsonObject properties = ctx.get(SetupPropertiesTask.INSTANCE);
        inspectTag(tag, context, path, pattern, ctx, new ReportDelegate(ctx, properties.has("strict-nbt") &&
                properties.get("strict-nbt").isJsonPrimitive() &&
                properties.get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                properties.get("strict-nbt").getAsBoolean(), pattern), true);
    }

    public static void inspectTag(NBTTag tag, PathContext context, NBTPath path, TokenPattern<?> pattern, ISymbolContext ctx, ReportDelegate delegate, boolean deepScan) {
        DataTypeQueryResponse response = ctx.get(SetupTypeMapTask.INSTANCE).collectTypeInformation(path, context);

        if(!response.isEmpty()) {
            ArrayList<DataType> filteredPossibleTypes = new ArrayList<>(response.getPossibleTypes());
            filteredPossibleTypes.removeIf(t -> !t.getCorrespondingTagType().isAssignableFrom(tag.getClass()));

            if(filteredPossibleTypes.size() > 1) {
                //Debug.log("Ambiguity between possible types, skipping it");
                //Debug.log(filteredPossibleTypes);
                return;
            }

            boolean matchesType = false;
            for(DataType type : filteredPossibleTypes) {
                if(type.getCorrespondingTagType().isAssignableFrom(tag.getClass())) {
                    matchesType = true;

                    TypeFlags flags;
                    if(type instanceof FlatType && (flags = type.getFlags()) != null) {

                        //region (boolean) flag
                        if(flags.hasFlag("boolean") && tag instanceof TagByte) {
                            byte byteValue = ((TagByte) tag).getValue();
                            if(byteValue != 0 && byteValue != 1) {
                                delegate.report("Byte at path '" + path + "' is boolean-like; %s be either 0b or 1b");
                            }
                        }
                        //endregion

                        //region type() flags
                        if(!flags.getTypeCategories().isEmpty() && tag instanceof TagString) {
                            boolean matched = false;
                            ResourceLocation location = ResourceLocation.createStrict(((TagString)tag).getValue());
                            if(location == null) {
                                delegate.report("String at path '" + path + "' is a type; but it doesn't look like a resource location: '" + ((TagString) tag).getValue() + "'");
                                continue;
                            }
                            for(String category : flags.getTypeCategories()) {
                                Type referencedType = MinecraftLiteralSet.parseType(location, pattern, ctx, category, false);
                                if(referencedType != null) {
                                    matched = true;
                                    ((TagString) tag).setValue(referencedType.toString());
                                }
                            }

                            if(!matched) {
                                if(flags.getTypeCategories().size() > 1) {
                                    delegate.report("String at path '" + path + "' %s be one of the following types: " + String.join(", ", flags.getTypeCategories()) + "; but '" + ((TagString) tag).getValue() + "' is not a type of any of the previous categories");
                                } else {
                                    delegate.report("String at path '" + path + "' %s be of type '" + flags.getTypeCategories().toArray(new String[0])[0] + "'. Instead got '" + ((TagString) tag).getValue() + "'.");
                                }
                            }
                        }
                        //endregion

                        //region one_of flags
                        if(!flags.getStringOptions().isEmpty() && tag instanceof TagString) {
                            boolean matched = false;

                            for(String option : flags.getStringOptions()) {
                                if(option.equals(((TagString) tag).getValue())) {
                                    matched = true;
                                    break;
                                }
                            }
                            if(!matched) {
                                delegate.report("String at path '" + path + "' %s be one of the following: " + String.join(", ", flags.getStringOptions()) + "; instead got '" + ((TagString) tag).getValue() + "'");
                            }
                        }
                        //endregion
                    }

                    break;
                }
            }
            if(!matchesType) {
                if(response.getPossibleTypes().size() > 1) {
                    delegate.report("Data type at path '" + path + "' %s be one of the following: " + response.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")) + "; got " + tag.getType().substring("TAG_".length()));
                } else {
                    delegate.report("Data type at path '" + path + "' %s be of type " + response.getPossibleTypes().toArray(new DataType[0])[0].getShortTypeName() + "; got " + tag.getType().substring("TAG_".length()));
                }
            }
        } else {
            if(delegate.strict) {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown data type for path '" + path + "'. Path context: " + context + ". Consider adding it to the type map", pattern));
            }
        }


        if(deepScan && tag instanceof ComplexNBTTag) {
            inspectTag(((ComplexNBTTag) tag), context, path, pattern, ctx);
        }
    }

    public static void inspectTag(ComplexNBTTag compound, PathContext context, TokenPattern<?> pattern, ISymbolContext file) {
        inspectTag(compound, context, null, pattern, file);
    }

    public static void inspectTag(ComplexNBTTag compound, PathContext context, NBTPath preAppended, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) throw new NullPointerException();

        JsonObject properties = ctx.get(SetupPropertiesTask.INSTANCE);

        ReportDelegate delegate = new ReportDelegate(ctx, properties.has("strict-nbt") &&
                properties.get("strict-nbt").isJsonPrimitive() &&
                properties.get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                properties.get("strict-nbt").getAsBoolean(), pattern);

        TagCompoundTraverser traverser = new TagCompoundTraverser(compound);

        TagCompoundTraverser.PathContents next;
        while((next = (traverser.next())) != null) {
            NBTPath path = next.getPath();
            NBTTag value = next.getValue();

            if(preAppended != null) {
                ArrayList<NBTPathNode> newPath = new ArrayList<>();

                for(NBTPath node : preAppended) {
                    newPath.add(node.getNode());
                }
                for(NBTPath node : path) {
                    newPath.add(node.getNode());
                }

                path = new NBTPath(newPath.toArray(new NBTPathNode[0]));
            }

            inspectTag(value, context, path, pattern, ctx, delegate, false);
        }
    }

    public static PathContext createContextForDataHolder(DataHolder holder, ISymbolContext ctx) {
        return new PathContext().setIsSetting(true).setProtocol(holder instanceof DataHolderEntity ? PathProtocol.ENTITY : holder instanceof DataHolderBlock ? PathProtocol.BLOCK_ENTITY : STORAGE, holder instanceof DataHolderEntity ? CommonParsers.guessEntityType((Entity) ((DataHolderEntity) holder).getEntity(), ctx) : holder instanceof DataHolderStorage ? ((DataHolderStorage) holder).getTarget().toString() : null);
    }

    static class ReportDelegate {
        private boolean strict;
        private TokenPattern<?> pattern;
        private ISymbolContext file;

        private String auxiliaryVerb;

        ReportDelegate(ISymbolContext file, boolean strict, TokenPattern<?> pattern) {
            this.strict = strict;
            this.pattern = pattern;
            this.file = file;

            auxiliaryVerb = strict ? "must" : "should";
        }

        public void report(String message) {
            message = message.replace("%s", auxiliaryVerb);
            if(this.strict) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, message, pattern, file);
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, message, pattern));
            }
        }
    }
}
