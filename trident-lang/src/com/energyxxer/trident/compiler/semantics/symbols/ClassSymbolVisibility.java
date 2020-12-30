package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;

import java.nio.file.Path;
import java.util.Objects;

public class ClassSymbolVisibility {
    private ClassSymbolVisibility() {}

    public static final SymbolVisibility PUBLIC = new SymbolVisibility(SymbolVisibility.PUBLIC.getVisibilityIndex()) {
        @Override
        public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
            return true;
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
            return "PUBLIC";
        }
    };

    public static SymbolVisibility LOCAL(CustomClass definingClass) {
        return new SymbolVisibility(2) {
            @Override
            public boolean isVisibleFromContext(Symbol symbol, ISymbolContext declaringContext, ISymbolContext accessingContext) {
                if(definingClass == null) throw new UnsupportedOperationException();
                return (definingClass.getDeclaringFile().getPathFromRoot().equals(accessingContext.getPathFromRoot()) || definingClass.isProtectedAncestor(accessingContext));
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
                return "LOCAL";
            }
        };
    }

    public static SymbolVisibility PRIVATE(CustomClass definingClass) {
        return new SymbolVisibility(1) {
            @Override
            public boolean isVisibleFromContext(Symbol symbol, ISymbolContext containingContext, ISymbolContext accessingContext) {
                if(definingClass == null) throw new UnsupportedOperationException();
                return accessingContext.isAncestor(definingClass.getInnerStaticContext());
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
                return "PRIVATE";
            }
        };
    }

    public static final SymbolVisibility LOCAL = LOCAL(null);
    public static final SymbolVisibility PRIVATE = PRIVATE(null);




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
