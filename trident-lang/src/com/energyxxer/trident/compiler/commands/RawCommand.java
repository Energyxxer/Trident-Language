package com.energyxxer.trident.compiler.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.inspection.CommandResolution;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionContext;
import org.jetbrains.annotations.NotNull;

public class RawCommand implements Command {

    private String command;

    public RawCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public @NotNull CommandResolution resolveCommand(ExecutionContext executionContext) {
        return new CommandResolution(executionContext, command);
    }
}
