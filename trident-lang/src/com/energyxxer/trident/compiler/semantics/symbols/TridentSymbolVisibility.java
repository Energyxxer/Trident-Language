package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.nio.file.Path;
import java.util.Objects;

public class TridentSymbolVisibility {
    private TridentSymbolVisibility() {}
    public static final SymbolVisibility PUBLIC = new SymbolVisibility(3) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            return true;
        }

        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return true;
        }

        @Override
        public String toString() {
            return "PUBLIC";
        }
    };
    public static final SymbolVisibility LOCAL = new SymbolVisibility(2) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            return true;
        }

        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return (symbol.getStartIndex() <= inFileIndex || symbol.getDeclarationPattern().getStringLocation().index == inFileIndex) && Objects.equals(fromPath, ((TridentSummaryModule) symbol.getParentFileSummary()).getFileLocation());
        }

        @Override
        public String toString() {
            return "LOCAL";
        }
    };
    public static final SymbolVisibility PRIVATE = new SymbolVisibility(1) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            return containingContext == accessingContext ||
                    ((TridentFile) containingContext.getStaticParentUnit()).getRootFile() == ((TridentFile) accessingContext.getStaticParentUnit()).getRootFile();
            //for classes: accessingContext.isAncestor(containingContext)
        }

        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            // Special case for symbol.getScopeEnd() == 0: to enable suggestions of private variables from inside the file,
            // while the file hasn't called surroundBlock() with proper block start and end indices
            return (symbol.getScopeEnd() == 0 || (symbol.getScopeStart() <= inFileIndex &&
                    inFileIndex <= symbol.getScopeEnd()))
                    && Objects.equals(fromPath, ((PrismarineSummaryModule) symbol.getParentFileSummary()).getFileLocation());
        }

        @Override
        public String toString() {
            return "PRIVATE";
        }
    };
}
