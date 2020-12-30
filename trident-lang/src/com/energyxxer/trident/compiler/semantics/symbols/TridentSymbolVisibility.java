package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.nio.file.Path;
import java.util.Objects;

public class TridentSymbolVisibility {
    private TridentSymbolVisibility() {}

    public static final SymbolVisibility LOCAL = new SymbolVisibility(2) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            return true;
        }

        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return (symbol.getStartIndex() <= inFileIndex || symbol.getDeclarationPattern().getStringLocation().index == inFileIndex) || !Objects.equals(fromPath, symbol.getParentFileSummary().getFileLocation());
        }

        @Override
        public String toString() {
            return "LOCAL";
        }
    };
    public static final SymbolVisibility MEMBER_ACCESS_ONLY = new SymbolVisibility(2) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            return false;
        }

        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return false;
        }

        @Override
        public boolean isVisibleMemberFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return true;
        }

        @Override
        public String toString() {
            return "MEMBER_ACCESS_ONLY";
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
                    && (fromPath == null || symbol.getParentFileSummary() == null || fromPath.equals(symbol.getParentFileSummary().getFileLocation()));
        }

        @Override
        public String toString() {
            return "PRIVATE";
        }
    };











    public static final SymbolVisibility SUMMARY_CLASS_PUBLIC = new SymbolVisibility(3) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            throw new UnsupportedOperationException();
        }

        //is visible in subclasses
        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return isAccessedBySameClass(symbol, fromPath, inFileIndex);
        }

        //is accessible via dot notation
        @Override
        public boolean isVisibleMemberFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return true;
        }

        @Override
        public String toString() {
            return "SUMMARY_CLASS_PUBLIC";
        }
    };
    public static final SymbolVisibility SUMMARY_CLASS_LOCAL = new SymbolVisibility(2) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            throw new UnsupportedOperationException();
        }

        //is visible in subclasses
        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return isAccessedBySameClass(symbol, fromPath, inFileIndex) || (symbol.isInstanceField() && (isAccessedBySubClass(symbol, inFileIndex, fromPath) || true));
        }

        //is accessible via dot notation
        @Override
        public boolean isVisibleMemberFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return isAccessedBySameFile(symbol, fromPath) || isAccessedBySubClass(symbol, inFileIndex, fromPath);
        }

        @Override
        public String toString() {
            return "SUMMARY_CLASS_LOCAL";
        }
    };
    public static final SymbolVisibility SUMMARY_CLASS_PRIVATE = new SymbolVisibility(1) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            throw new UnsupportedOperationException();
        }

        //is visible in subclasses
        @Override
        public boolean isVisibleFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return isAccessedBySameClass(symbol, fromPath, inFileIndex);
        }

        //is accessible via dot notation
        @Override
        public boolean isVisibleMemberFromSummaryBlock(SummarySymbol symbol, Path fromPath, int inFileIndex) {
            return isAccessedBySameClass(symbol, fromPath, inFileIndex);
        }

        @Override
        public String toString() {
            return "SUMMARY_CLASS_PRIVATE";
        }
    };


    private static boolean isAccessedBySameClass(SummarySymbol symbol, Path fromPath, int inFileIndex) {
        return ((symbol.getScopeEnd() == 0 || (symbol.getScopeStart() <= inFileIndex && inFileIndex <= symbol.getScopeEnd())) || symbol.getDeclarationPattern().getStringLocation().index == inFileIndex) && isAccessedBySameFile(symbol, fromPath);
    }

    private static boolean isAccessedBySameFile(SummarySymbol symbol, Path fromPath) {
        Path filePath = symbol.getParentFileSummary() != null ? symbol.getParentFileSummary().getFileLocation() : null;
        return fromPath == null || filePath == null || Objects.equals(fromPath, filePath);
    }

    private static boolean isAccessedBySubClass(SummarySymbol symbol, int inFileIndex, Path fromPath) {
        //TODO
        return false;
    }
}
