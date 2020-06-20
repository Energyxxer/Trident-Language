package com.energyxxer.util.processes;

import java.util.ArrayList;

public abstract class AbstractProcess {
    private String name;
    private String status;
    private float progress = -1;

    protected Thread thread;

    private boolean complete = false;
    private boolean successful = false;

    private ArrayList<StartListener> startListeners = new ArrayList<>();
    private ArrayList<ProgressListener> progressListeners = new ArrayList<>();
    private ArrayList<CompletionListener> completionListeners = new ArrayList<>();

    public AbstractProcess(String name) {
        this.name = name;
    }

    protected void initializeThread(Runnable runnable) {
        this.thread = new Thread(runnable, this.name);
        thread.setUncaughtExceptionHandler((th, ex) -> {
            finalizeProcess(false);
        });
    }

    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    public void start() {
        thread.start();
    }

    public void terminate() {
        thread.stop();
        finalizeProcess(false);
    }

    protected void updateStatus(String status) {
        this.status = status;
        invokeProgressUpdate();
    }

    protected void updateProgress(float progress) {
        this.progress = progress;
    }

    protected void updateStatusAndProgress(String status, float progress) {
        this.status = status;
        this.progress = progress;
        invokeProgressUpdate();
    }

    protected void invokeStart() {
        for(StartListener startListener : startListeners) {
            startListener.onStart(this);
        }
    }

    protected void invokeProgressUpdate() {
        for(ProgressListener progressListener : progressListeners) {
            progressListener.onProgress(this);
        }
    }

    protected void finalizeProcess(boolean success) {
        this.complete = true;
        this.successful = success;
        for(CompletionListener completionListener : completionListeners) {
            completionListener.onCompletion(this, success);
        }
        startListeners.clear();
        progressListeners.clear();
        completionListeners.clear();
    }

    public void addStartListener(StartListener listener) {
        startListeners.add(listener);
    }

    public void removeStartListener(StartListener listener) {
        startListeners.remove(listener);
    }

    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
    }

    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    public void addCompletionListener(CompletionListener listener) {
        completionListeners.add(listener);
    }

    public void removeCompletionListener(CompletionListener listener) {
        completionListeners.remove(listener);
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public float getProgress() {
        return progress;
    }

    public Thread getThread() {
        return thread;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
