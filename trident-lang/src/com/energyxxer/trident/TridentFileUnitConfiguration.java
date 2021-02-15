package com.energyxxer.trident;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSource;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.PrismarineLanguageUnitConfiguration;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.controlflow.ReturnException;
import com.energyxxer.prismarine.in.ProjectReader;
import com.energyxxer.prismarine.operators.OperatorPool;
import com.energyxxer.prismarine.summaries.PrismarineProjectSummary;
import com.energyxxer.prismarine.util.PathMatcher;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentOperatorPool;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.BreakException;
import com.energyxxer.trident.compiler.semantics.ContinueException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.sets.*;
import com.energyxxer.trident.sets.java.JavaCommandSet;
import com.energyxxer.trident.sets.java.JavaModifierSet;
import com.energyxxer.trident.sets.java.SelectorArgumentSet;
import com.energyxxer.trident.sets.trident.TridentInstructionSet;
import com.energyxxer.trident.sets.trident.TridentLiteralSet;
import com.energyxxer.trident.sets.trident.TridentPatternProvider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static com.energyxxer.prismarine.PrismarineCompiler.PassResult.OK;
import static com.energyxxer.trident.Trident.FUNCTION_EXTENSION;

public class TridentFileUnitConfiguration extends PrismarineLanguageUnitConfiguration<TridentFile> {

    public static final TridentFileUnitConfiguration INSTANCE = new TridentFileUnitConfiguration();

    private TridentFileUnitConfiguration() {
        STOP_PATH_MATCHER = PathMatcher.createMatcher(this.getStopPath());
    }

    @Override
    public Class<TridentFile> getUnitClass() {
        return TridentFile.class;
    }

    @Override
    public int getNumberOfPasses() {
        return 3;
    }

    @Override
    public PrismarineCompiler.PassResult performPass(TridentFile unit, PrismarineCompiler compiler, int passNumber) {
        switch(passNumber) {
            case 1: {
                compiler.setProgress("Resolving requires");
                unit.checkCircularRequires();
                return OK;
            }
            case 2: {
                compiler.setProgress("Resolving requires");
                this.getAllRequires(unit, compiler);
                return OK;
            }
            case 3: {
                compiler.setProgress("Analyzing " + unit.getResourceLocation());
                try {
                    unit.resolveEntries();
                } catch(ReturnException r) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Return instruction outside inner function", r.getPattern()));
                } catch(BreakException b) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Break instruction outside loop or switch", b.getPattern()));
                } catch(ContinueException c) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Continue instruction outside loop", c.getPattern()));
                }
                return OK;
            }
            default:
                throw new IllegalArgumentException("Cannot perform pass " + passNumber + " on this unit type"); //TODO make a better exception class for this
        }
    }

    @Override
    public void onPassEnd(PrismarineCompiler compiler, int passNumber) {
        if(passNumber == 2) {
            compiler.sortUnits(this, (a,b) -> (a.isCompileOnly() != b.isCompileOnly()) ? a.isCompileOnly() ? -2 : 2 : (int) Math.signum((b.getPriority() - a.getPriority()) * 1000));
        }
        super.onPassEnd(compiler, passNumber);
    }

    private Collection<ResourceLocation> getAllRequires(TridentFile file, PrismarineCompiler compiler) {
        if(file.getCascadingRequires() == null) {
            file.addCascadingRequires(Collections.emptyList());
            file.getRequires().forEach(fl -> file.addCascadingRequires(getAllRequires(compiler.getUnit(this, resourceLocationToFunctionPath(fl)), compiler)));
        }
        return file.getCascadingRequires();
    }

    @Override
    public OperatorPool getOperatorPool() {
        return TridentOperatorPool.INSTANCE;
    }

    @Override
    public void setupProductions(PrismarineProductions productions) {

        productions.installProviderSet(new ValueAccessExpressionSet());
        productions.installProviderSet(new BasicLiteralSet());
        productions.installProviderSet(new DataStructureLiteralSet());

        productions.installProviderSet(new JavaCommandSet());
        productions.installProviderSet(new JavaModifierSet());
        productions.installProviderSet(new SelectorArgumentSet());
        productions.installProviderSet(new JsonLiteralSet());
        productions.installProviderSet(new MinecraftLiteralSet());
        productions.installProviderSet(new MinecraftWrapperLiteralSet());

        productions.installProviderSet(new TridentPatternProvider());

        productions.installProviderSet(new TridentLiteralSet());
        productions.installProviderSet(new TridentInstructionSet());

    }

    @Override
    public String getStopPath() {
        return "datapack/data/(*)/functions/(**)" + FUNCTION_EXTENSION;
    }

    @Override
    public LexerProfile createLexerProfile() {
        return TridentLexerProfile.INSTANCE.getValue();
    }

    @Override
    public TridentSummaryModule createSummaryModule(TokenSource source, Path relativePath, PrismarineProjectSummary parentSummary) {
        TridentSummaryModule summary = new TridentSummaryModule();
        summary.setParentSummary(parentSummary);
        summary.setResourceLocation(functionPathToResourceLocation(relativePath));
        return summary;
    }

    @Override
    public TridentFile createUnit(PrismarineCompiler compiler, ProjectReader.Result readResult) {
        return new TridentFile(compiler, readResult.getRelativePath(), readResult.getPattern());
    }

    private static PathMatcher STOP_PATH_MATCHER;

    public static ResourceLocation functionPathToResourceLocation(Path path) {
        PathMatcher.Result result = STOP_PATH_MATCHER.getMatchResult(path.toString().replace(File.separatorChar,'/'));
        String namespace = result.groups[1];
        String body = result.groups[2];
        return new ResourceLocation(namespace + ":" + body);
    }

    public static Path resourceLocationToFunctionPath(ResourceLocation loc) {
        return Paths.get("datapack", "data", loc.namespace, "functions", loc.body.replace('/',File.separatorChar) + Trident.FUNCTION_EXTENSION);
    }
}
