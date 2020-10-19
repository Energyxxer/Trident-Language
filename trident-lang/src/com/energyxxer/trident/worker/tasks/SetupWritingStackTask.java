package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

import java.util.Stack;

public class SetupWritingStackTask extends PrismarineProjectWorkerTask<SetupWritingStackTask.WritingStack> {

    public static final SetupWritingStackTask INSTANCE = new SetupWritingStackTask();

    private SetupWritingStackTask() {}

    @Override
    public WritingStack perform(PrismarineProjectWorker worker) throws Exception {
        return new WritingStack();
    }

    @Override
    public String getProgressMessage() {
        return "Setting up writing stack";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[0];
    }

    public static class WritingStack {
        private final Stack<TridentFile> stack = new Stack<>();

        public void pushWritingFile(TridentFile file) {
            stack.push(file);
        }

        public TridentFile popWritingFile() {
            return stack.pop();
        }

        public TridentFile getWritingFile() {
            return stack.peek();
        }
    }
}
