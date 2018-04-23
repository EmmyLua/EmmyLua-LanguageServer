package com.intellij.openapi.project;

import org.jetbrains.annotations.NotNull;

public class DumbService {
    public static boolean isDumb(@NotNull Project project) {
        return false;
    }
}
