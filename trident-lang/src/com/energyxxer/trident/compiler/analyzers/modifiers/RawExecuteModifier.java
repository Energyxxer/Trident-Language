package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.execute.SubCommandResult;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionContext;
import org.jetbrains.annotations.NotNull;

public class RawExecuteModifier implements ExecuteModifier {

    private final String raw;

    public RawExecuteModifier(String raw) {
        this.raw = raw;
    }

    @Override
    public @NotNull SubCommandResult getSubCommand(ExecutionContext executionContext) {
        return new SubCommandResult(executionContext, raw);
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public boolean isSignificant() {
        return true;
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }
}
