package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.operators.OperatorManager;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.ContextualToString;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags.*;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethod;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.entities.EntityEvent;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.worker.tasks.SetupPropertiesTask;
import org.jetbrains.annotations.Contract;

public class TridentTypeSystem extends PrismarineTypeSystem {

    private CustomClass baseClass;
    public java.util.Random projectRandom;
    private OperatorManager<ClassMethod> operatorManager;

    public TridentTypeSystem(PrismarineCompiler compiler, ISymbolContext globalCtx) {
        super(compiler, globalCtx);
        operatorManager = new OperatorManager<>(this);


        int defaultSeed = globalCtx.getCompiler().getRootDir().getName().hashCode();
        int projectSeed = JsonTraverser.INSTANCE.reset(globalCtx.get(SetupPropertiesTask.INSTANCE)).get("random-seed").asInt(defaultSeed);

        projectRandom = new java.util.Random(projectSeed);
    }

    @Override
    protected void registerTypes() {
        //Extrinsic Type Handlers
        registerPrimitiveTypeHandler(new NullTypeHandler(this));
        registerPrimitiveTypeHandler(new BooleanTypeHandler(this));
        registerPrimitiveTypeHandler(new IntTypeHandler(this));
        registerPrimitiveTypeHandler(new RealTypeHandler(this));
        registerPrimitiveTypeHandler(new StringTypeHandler(this));
        registerPrimitiveTypeHandler(new ResourceTypeHandler(this));
        registerPrimitiveTypeHandler(new TextComponentTypeHandler(this));
        registerPrimitiveTypeHandler(new CoordinateTypeHandler(this));
        registerPrimitiveTypeHandler(new RotationTypeHandler(this));
        registerPrimitiveTypeHandler(new UUIDTypeHandler(this));
        registerPrimitiveTypeHandler(new BlockTypeHandler(this));
        registerPrimitiveTypeHandler(new EntityTypeHandler(this));
        registerPrimitiveTypeHandler(new IntRangeTypeHandler(this));
        registerPrimitiveTypeHandler(new ItemTypeHandler(this));
        registerPrimitiveTypeHandler(new NBTPathTypeHandler(this));
        registerPrimitiveTypeHandler(new RealRangeTypeHandler(this));

        TagCompoundTypeHandler tagCompoundHandler = new TagCompoundTypeHandler(this);
        registerPrimitiveTypeHandler(tagCompoundHandler);
        registerPrimitiveTypeHandler("nbt", tagCompoundHandler);

        registerPrimitiveTypeHandler(new TagListTypeHandler(this));
        registerPrimitiveTypeHandler(new TagByteTypeHandler(this));
        registerPrimitiveTypeHandler(new TagShortTypeHandler(this));
        registerPrimitiveTypeHandler(new TagIntTypeHandler(this));
        registerPrimitiveTypeHandler(new TagFloatTypeHandler(this));
        registerPrimitiveTypeHandler(new TagDoubleTypeHandler(this));
        registerPrimitiveTypeHandler(new TagLongTypeHandler(this));
        registerPrimitiveTypeHandler(new TagStringTypeHandler(this));

        registerPrimitiveTypeHandler(new TagByteArrayTypeHandler(this));
        registerPrimitiveTypeHandler(new TagIntArrayTypeHandler(this));
        registerPrimitiveTypeHandler(new TagLongArrayTypeHandler(this));

        registerPrimitiveTypeHandler(new NBTTagTypeHandler(this));

        registerPrimitiveTypeHandler(new ExceptionTypeHandler(this));
        registerPrimitiveTypeHandler(new FunctionTypeHandler(this));

        //Intrinsic Type Handlers
        registerPrimitiveTypeHandler(CustomEntity.createStaticHandler(this));
        registerPrimitiveTypeHandler(CustomItem.createStaticHandler(this));
        registerPrimitiveTypeHandler(EntityEvent.createStaticHandler(this));
        registerPrimitiveTypeHandler(DictionaryObject.createStaticHandler(this));
        registerPrimitiveTypeHandler(ListObject.createStaticHandler(this));
        registerPrimitiveTypeHandler(PointerObject.createStaticHandler(this));

        setMetaTypeHandler(new TypeHandlerTypeHandler(this));

        baseClass = CustomClass.createBaseClass(this);
    }

    @Contract("null -> null")
    @Override
    public Object sanitizeObject(Object obj) {
        if(obj == null) return null;
        if(obj.getClass().isArray()) {
            return new ListObject(this, (Object[]) obj);
        }
        return obj;
    }

    @Override
    public String castToString(Object obj) {
        if(obj == null) {
            return "null";
        } else if(obj instanceof PrimitivePrismarineFunction && !(obj instanceof PrismarineFunction)) {
            return "<internal function>";
        } else if(obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler()) {
            return "type_definition<" + typeHandlerToString((TypeHandler) obj) + ">";
        } else {
            return super.castToString(obj);
        }
    }

    @Override
    public String castToString(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(obj == null) {
            return "null";
        } else if(obj instanceof PrimitivePrismarineFunction && !(obj instanceof PrismarineFunction)) {
            return "<internal function>";
        } else if(obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler()) {
            return "type_definition<" + typeHandlerToString((TypeHandler) obj) + ">";
        } else if(obj instanceof Command) {
            return ((Command) obj).resolveCommand(new ExecutionContext()).construct();
        } else if(obj instanceof ContextualToString) {
            return ((ContextualToString) obj).contextualToString(pattern, ctx);
        } else {
            return String.valueOf(obj);
        }
    }

    public CustomClass getBaseClass() {
        return baseClass;
    }

    @Override
    public OperatorManager<ClassMethod> getOperatorManager() {
        return operatorManager;
    }
}
