package com.intellij.openapi.application;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ApplicationManager {
    private static ApplicationManager instance;

    public static ApplicationManager getInstance() {
        if (instance == null) {
            instance = new ApplicationManager();
        }
        return instance;
    }

    private final ThreadPoolExecutor executor;

    public ApplicationManager() {
        executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public void executeOnPooledThread(Runnable runnable) {
        executor.execute(runnable);
    }
}
