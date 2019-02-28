package com.energyxxer.util.processes;

public interface CompletionListener {
    void onCompletion(AbstractProcess process, boolean success);
}
