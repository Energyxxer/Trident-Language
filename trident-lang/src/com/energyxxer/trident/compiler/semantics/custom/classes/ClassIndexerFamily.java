package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.functions.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.util.logger.Debug;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassIndexerFamily {
    //Ready to add indexer overloading in the future when needed
    private ClassIndexer indexer = null;
    private final ArrayList<ClashingInheritedIndexers> clashingInheritedIndexers = new ArrayList<>();

    private boolean superClassesRequireSetter = false;
    private SymbolVisibility superClassesGetterVisibility = TridentSymbolVisibility.PRIVATE;
    private SymbolVisibility superClassesSetterVisibility = TridentSymbolVisibility.PRIVATE;

    public void put(ClassIndexer newIndexer, CustomClass.MemberParentMode mode, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(newIndexer == null) return;
        if(indexer == null || mode == CustomClass.MemberParentMode.FORCE) {
            this.indexer = newIndexer;
            if(mode == CustomClass.MemberParentMode.INHERIT) {
                superClassesRequireSetter = superClassesRequireSetter || newIndexer.getSetterFunction() != null;
                superClassesGetterVisibility = SymbolVisibility.max(superClassesGetterVisibility, newIndexer.getGetterVisibility());
                superClassesSetterVisibility = SymbolVisibility.max(superClassesSetterVisibility, newIndexer.getSetterVisibility());
            }
            return;
        }

        ClassIndexer oldIndexer = indexer;

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            if(oldIndexer.getDefiningClass() != newIndexer.getDefiningClass() && oldIndexer.getDefiningClass() != ((TridentTypeSystem) ctx.getTypeSystem()).getBaseClass()) {
                //Make note for later
                registerClashingIndexers(oldIndexer, newIndexer);
            } else {
                Debug.log("Skipping " + newIndexer + " since defined in same class: " + newIndexer.getDefiningClass());
            }
        } else {
            if(oldIndexer.getDefiningClass() == newIndexer.getDefiningClass()) {
                throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Duplicate definition for class indexer", pattern, ctx);
            } else if(mode != CustomClass.MemberParentMode.OVERRIDE) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Indexer " + oldIndexer + " is already defined in inherited class " + newIndexer.getDefiningClass().getTypeIdentifier() + " with the same parameter type. Use the 'override' keyword to replace it", pattern, ctx);
            }
        }

        if(!oldIndexer.getDefiningClass().hasAccess(ctx, oldIndexer.getGetterVisibility())) {
            if(mode == CustomClass.MemberParentMode.INHERIT) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + oldIndexer + ": Getter has " + oldIndexer.getGetterVisibility().toString().toLowerCase() + " access in " + oldIndexer.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + oldIndexer + ": Getter has " + oldIndexer.getGetterVisibility().toString().toLowerCase() + " access in " + oldIndexer.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }
        if(oldIndexer.getSetterFunction() != null && !oldIndexer.getDefiningClass().hasAccess(ctx, oldIndexer.getSetterVisibility())) {
            if(mode == CustomClass.MemberParentMode.INHERIT) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + oldIndexer + ": Setter has " + oldIndexer.getSetterVisibility().toString().toLowerCase() + " access in " + oldIndexer.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + oldIndexer + ": Setter has " + oldIndexer.getSetterVisibility().toString().toLowerCase() + " access in " + oldIndexer.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }
        if(mode == CustomClass.MemberParentMode.INHERIT) {
            if(!TypeConstraints.constraintsEqual(newIndexer.getIndexParameter().getConstraints(), oldIndexer.getIndexParameter().getConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Clashing inherited implementations of " + oldIndexer + ": incompatible index parameter constraints.\n    Constraint:        " + oldIndexer.getIndexParameter().getConstraints() + " in " + oldIndexer.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + newIndexer.getIndexParameter().getConstraints() + " in " + newIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        } else if(!TypeConstraints.constraintAContainsB(newIndexer.getIndexParameter().getConstraints(), oldIndexer.getIndexParameter().getConstraints())) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot override " + oldIndexer + " due to incompatible index parameter constraints.\n    Constraint:        " + oldIndexer.getIndexParameter().getConstraints() + " in " + oldIndexer.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + newIndexer.getIndexParameter().getConstraints() + " in " + newIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
        }

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            //If inherited, just require the resolving implementation to specify a setter; done at the end of this method
        } else {
            if (superClassesRequireSetter && newIndexer.getSetterFunction() == null) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + oldIndexer + ": required setter not present in overriding indexer", pattern, ctx);
            }
        }

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            //If inherited, just get the most general visibility; done at the end of this method
        } else {
            if(newIndexer.getGetterVisibility().getVisibilityIndex() < superClassesGetterVisibility.getVisibilityIndex()) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override getter for " + oldIndexer + ": attempting to assign weaker access privileges '" + newIndexer.getGetterVisibility().toString().toLowerCase() + "', was '" + superClassesGetterVisibility.toString().toLowerCase() + "'", pattern, ctx);
            }
            if(newIndexer.getSetterFunction() != null && newIndexer.getSetterVisibility().getVisibilityIndex() < superClassesSetterVisibility.getVisibilityIndex()) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override setter for " + oldIndexer + ": attempting to assign weaker access privileges '" + newIndexer.getSetterVisibility().toString().toLowerCase() + "', was '" + superClassesSetterVisibility.toString().toLowerCase() + "'", pattern, ctx);
            }
        }


        if(mode == CustomClass.MemberParentMode.INHERIT) {
            if(!TypeConstraints.constraintsEqual(oldIndexer.getGetterFunction().getBranch().getReturnConstraints(), newIndexer.getGetterFunction().getBranch().getReturnConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Clashing inherited implementations of " + oldIndexer + ": incompatible getter return constraints.\n    Constraint:        " + newIndexer.getGetterFunction().getBranch().getReturnConstraints() + " in " + newIndexer.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldIndexer.getGetterFunction().getBranch().getReturnConstraints() + " in " + oldIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
            if(oldIndexer.getSetterFunction() != null && newIndexer.getSetterFunction() != null && !TypeConstraints.constraintsEqual(newIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints(), oldIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Clashing inherited implementations of " + oldIndexer + ": incompatible setter value constraints.\n    Constraint:        " + newIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + newIndexer.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + oldIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        } else {
            if(!TypeConstraints.constraintAContainsB(oldIndexer.getGetterFunction().getBranch().getReturnConstraints(), newIndexer.getGetterFunction().getBranch().getReturnConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot override " + oldIndexer + " due to incompatible getter return constraints.\n    Constraint:        " + newIndexer.getGetterFunction().getBranch().getReturnConstraints() + " in " + newIndexer.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldIndexer.getGetterFunction().getBranch().getReturnConstraints() + " in " + oldIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
            if(oldIndexer.getSetterFunction() != null && newIndexer.getSetterFunction() != null && !TypeConstraints.constraintAContainsB(newIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints(), oldIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot override " + oldIndexer + " due to incompatible setter value constraints.\n    Constraint:        " + newIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + newIndexer.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldIndexer.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + oldIndexer.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            superClassesRequireSetter = superClassesRequireSetter || newIndexer.getSetterFunction() != null;
            superClassesGetterVisibility = SymbolVisibility.max(superClassesGetterVisibility, newIndexer.getGetterVisibility());
            superClassesSetterVisibility = SymbolVisibility.max(superClassesSetterVisibility, newIndexer.getSetterVisibility());
        } else {
            this.indexer = newIndexer;
        }
    }

    public ClassIndexer get() {
        return indexer;
    }

    public void putAll(ClassIndexerFamily others, TokenPattern<?> pattern, ISymbolContext ctx) {
        put(others.get(), CustomClass.MemberParentMode.INHERIT, pattern, ctx);
    }

    private void registerClashingIndexers(ClassIndexer existing, ClassIndexer method) {
        for(ClashingInheritedIndexers clashingIndexers : clashingInheritedIndexers) {
            if(clashingIndexers.matches(method)) {
                clashingIndexers.addIndexer(method);
                return;
            }
        }
        //Could not find registered clashes, add both existing and method
        clashingInheritedIndexers.add(new ClashingInheritedIndexers(existing, method));
    }

    public void checkClashingInheritedIndexersResolved(CustomClass resolvingClass, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(indexer == null) return;
        if(!clashingInheritedIndexers.isEmpty() && indexer.getDefiningClass() != resolvingClass) {
            //Did not change
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Indexer '" + indexer + "' is defined in multiple inherited classes: " + clashingInheritedIndexers.get(0).getIndexers().stream().map(m -> m.getDefiningClass().getTypeIdentifier()).collect(Collectors.joining(", ")) + ". Override it in the class body", pattern, ctx);
        }
    }

    private static class ClashingInheritedIndexers {
        FormalParameter indexParameter;
        List<ClassIndexer> indexers;

        public ClashingInheritedIndexers(ClassIndexer a, ClassIndexer b) {
            indexers = new ArrayList<>();
            indexers.add(a);
            indexers.add(b);
            indexParameter = a.getIndexParameter();
        }

        public void addIndexer(ClassIndexer indexer) {
            indexers.add(indexer);
        }

        public boolean matches(ClassIndexer indexer) {
            return TypeConstraints.constraintsEqual(indexParameter.getConstraints(), indexer.getIndexParameter().getConstraints());
        }

        public FormalParameter getIndexParameter() {
            return indexParameter;
        }

        public List<ClassIndexer> getIndexers() {
            return indexers;
        }

        @Override
        public String toString() {
            return indexers.get(0).toString();
        }
    }
}
