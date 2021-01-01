package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.schedule.ScheduleCommand;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.functionlogic.functions.FunctionWriter;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.util.TimeSpan;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TickingFunction extends SpecialFile {

    private ArrayList<FunctionWriter> tickEvents = new ArrayList<>();
    private final int interval;
    private EntityTickFunction entityTickFunction;

    public TickingFunction(SpecialFileManager parent, int interval) {
        super(parent, interval == 1 ? "tick" : "tick/" + interval + "t");
        if(interval <= 0) throw new IllegalArgumentException("Ticking interval may not be zero or negative");

        this.interval = interval;
    }

    public void addTickEvent(FunctionWriter writer) {
        tickEvents.add(writer);
    }

    public void addEntityTickEvent(EntityEvent event) {
        if(entityTickFunction == null) {
            entityTickFunction = new EntityTickFunction();
            tickEvents.add(entityTickFunction);
        }
        entityTickFunction.entityEvents.add(event);
    }

    @Override
    public boolean shouldForceCompile() {
        return !tickEvents.isEmpty();
    }

    @Override
    public Function getFunction() {
        if(this.function == null) this.function = parent.getNamespace().functions.getOrCreate("trident/" + this.getFunctionName());
        return this.function;
    }

    @Override
    protected void compile() {
        Tag tag = getParent().getModule().minecraft.tags.functionTags.getOrCreate(interval == 1 ? "tick" : "load");
        tag.setExport(true);
        tag.addValue(new FunctionReference(function));

        tickEvents.forEach(function::append);

        if(interval > 1) {
            function.append(new ScheduleCommand(function, new TimeSpan(interval)));
        }
    }

    private class EntityTickFunction implements FunctionWriter {

        ArrayList<EntityEvent> entityEvents = new ArrayList<>();
        private Command resolvedCommand = null;

        @Override
        public void onAppend(@NotNull FunctionSection section) {
            if(entityEvents.size() == 1) {
                EntityEvent singleEvent = entityEvents.get(0);
                Selector selector = singleEvent.getSelector(false);
                resolvedCommand = new ExecuteCommand(singleEvent.command, new ExecuteAsEntity(selector), new ExecuteAtEntity(new Selector(Selector.BaseSelector.SENDER)));
            } else {
                Function entityTickFunction = parent.getNamespace().functions.getOrCreate("trident/" + getFunctionName() + "/entity");
                for(EntityEvent entityEvent : entityEvents) {
                    Selector selector = entityEvent.getSelector(true);
                    if(!selector.getAllArguments().isEmpty()) {
                        entityTickFunction.append(new ExecuteCommand(entityEvent.command, new ExecuteConditionEntity(ExecuteCondition.ConditionType.IF, selector)));
                    } else {
                        entityTickFunction.append(entityEvent.command);
                    }
                }
                resolvedCommand = new ExecuteCommand(new FunctionCommand(entityTickFunction), new ExecuteAsEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES)), new ExecuteAtEntity(new Selector(Selector.BaseSelector.SENDER)));
            }
            resolvedCommand.onAppend(section);
        }

        @Override
        public @NotNull String toFunctionContent(@NotNull FunctionSection functionSection) {
            return resolvedCommand.toFunctionContent(functionSection);
        }
    }

    public static class EntityEvent {
        SelectorArgument[] entityFilter;
        Command command;

        public EntityEvent(SelectorArgument[] entityFilter, Command command) {
            this.entityFilter = entityFilter;
            this.command = command;
        }

        Selector getSelector(boolean narrowedDownToSender) {
            if(narrowedDownToSender) {
                return new Selector(Selector.BaseSelector.SENDER, entityFilter);
            } else {
                return new Selector(Selector.BaseSelector.ALL_ENTITIES, entityFilter);
            }
        }
    }
}
