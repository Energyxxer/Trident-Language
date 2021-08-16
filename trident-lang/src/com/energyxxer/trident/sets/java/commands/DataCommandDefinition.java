package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.*;
import com.energyxxer.commodore.functionlogic.nbt.DataHolder;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathCompoundRoot;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTInspector;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import java.util.ArrayList;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DataCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"data"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {

        TokenStructureMatch sourceMatch = choice(
                group(literal("from"), productions.getOrCreateStructure("DATA_HOLDER"), optional(TridentProductions.sameLine(), productions.getOrCreateStructure("NBT_PATH")).setSimplificationFunctionContentIndex(1).setName("PATH_CLAUSE")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    DataHolder holder = (DataHolder) p.find("DATA_HOLDER").evaluate(ctx);

                    NBTPath path = (NBTPath) p.findThenEvaluate("PATH_CLAUSE", null, ctx);
                    return new ModifySourceFromHolder(holder, path);
                }),
                group(literal("value"), TridentProductions.noToken().addTags("cspn:NBT Value"), productions.getOrCreateStructure("NBT_VALUE")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    NBTTag value = (NBTTag) p.find("NBT_VALUE").evaluate(ctx);

                    return new ModifySourceValue(value);
                })
        ).setName("DATA_SOURCE");
        sourceMatch.addTags("cspn:Data Source");

        return group(
                TridentProductions.commandHeader("data"),
                choice(
                        group(
                                literal("get"),
                                productions.getOrCreateStructure("DATA_HOLDER"),
                                optional(
                                        TridentProductions.sameLine(),
                                        productions.getOrCreateStructure("NBT_PATH"),
                                        TridentProductions.real(productions).setOptional().setName("SCALE").addTags("cspn:Scale")
                                ).setName("PATH_CLAUSE")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            DataHolder target = (DataHolder) p.find("DATA_HOLDER").evaluate(ctx);
                            NBTPath path = (NBTPath) p.findThenEvaluate("PATH_CLAUSE.NBT_PATH", null, ctx);
                            double scale = (double) p.findThenEvaluate("PATH_CLAUSE.SCALE", 1.0, ctx);

                            try {
                                return new DataGetCommand(target, path, scale);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("DATA_HOLDER.ENTITY"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }
                        }),
                        group(
                                literal("merge"),
                                productions.getOrCreateStructure("DATA_HOLDER"),
                                TridentProductions.noToken().addTags("cspn:NBT"),
                                productions.getOrCreateStructure("NBT_COMPOUND")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            DataHolder target = (DataHolder) p.find("DATA_HOLDER").evaluate(ctx);
                            TagCompound nbt = (TagCompound) p.find("NBT_COMPOUND").evaluate(ctx);

                            try {
                                PathContext context = NBTInspector.createContextForDataHolder(target, ctx);
                                NBTInspector.inspectTag(nbt, context, p.find("NBT_COMPOUND"), ctx);
                                return new DataMergeCommand(target, nbt);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("DATA_HOLDER.ENTITY"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }
                        }),
                        group(
                                literal("modify"),
                                productions.getOrCreateStructure("DATA_HOLDER"),
                                productions.getOrCreateStructure("NBT_PATH"),
                                choice(
                                        literal("append").setEvaluator((p, d) -> DataModifyCommand.APPEND()),
                                        group(literal("insert"), TridentProductions.integer(productions).setName("INSERT_INDEX").addTags("cspn:Insert Index")).setEvaluator((p, d) -> {
                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                            int insertIndex = (int) p.find("INSERT_INDEX").evaluate(ctx);
                                            return DataModifyCommand.INSERT(insertIndex);
                                        }),
                                        literal("merge").setEvaluator((p, d) -> DataModifyCommand.MERGE()),
                                        literal("prepend").setEvaluator((p, d) -> DataModifyCommand.PREPEND()),
                                        literal("set").setEvaluator((p, d) -> DataModifyCommand.SET())
                                ).setName("INNER"),
                                sourceMatch
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            DataHolder target = (DataHolder) p.find("DATA_HOLDER").evaluate(ctx);
                            NBTPath path = (NBTPath) p.find("NBT_PATH").evaluate(ctx);
                            DataModifyCommand.ModifySource source = (DataModifyCommand.ModifySource) p.find("DATA_SOURCE").evaluate(ctx);

                            DataModifyCommand.ModifyOperation operation = (DataModifyCommand.ModifyOperation) p.find("INNER").evaluate(ctx);

                            NBTPath analysisPath = path;
                            if (operation.isListModification()) {
                                ArrayList<NBTPathNode> newNodes = new ArrayList<>();
                                for (NBTPath subPath : path) {
                                    newNodes.add(subPath.getNode());
                                }
                                newNodes.add(new NBTListMatch());
                                analysisPath = new NBTPath(newNodes.toArray(new NBTPathNode[0]));
                            }

                            try {
                                if (source instanceof ModifySourceValue) {
                                    NBTInspector.inspectTag(((ModifySourceValue) source).getValue(), NBTInspector.createContextForDataHolder(target, ctx), analysisPath, p, ctx);
                                } else {
                                    NBTPath sourcePath = ((ModifySourceFromHolder) source).getSourcePath();
                                    if (sourcePath == null)
                                        sourcePath = new NBTPath(new NBTPathCompoundRoot(new TagCompound()));
                                    NBTInspector.comparePaths(analysisPath, NBTInspector.createContextForDataHolder(target, ctx), sourcePath, NBTInspector.createContextForDataHolder(((ModifySourceFromHolder) source).getHolder(), ctx), p, ctx);
                                }
                                return new DataModifyCommand(target, path, operation, source);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("DATA_HOLDER.ENTITY"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }

                        }),
                        group(literal("remove"), productions.getOrCreateStructure("DATA_HOLDER"), productions.getOrCreateStructure("NBT_PATH")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            try {
                                return new DataRemoveCommand((DataHolder) p.find("DATA_HOLDER").evaluate(ctx), (NBTPath) p.find("NBT_PATH").evaluate(ctx));
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("DATA_HOLDER.ENTITY"))
                                        .invokeThrow();
                                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                            }
                        })
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
