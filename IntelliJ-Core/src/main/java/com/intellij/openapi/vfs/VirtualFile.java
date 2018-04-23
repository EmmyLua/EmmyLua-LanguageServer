package com.intellij.openapi.vfs;

import org.jetbrains.annotations.NotNull;

public interface VirtualFile {
    @NotNull
    String getName();

    String getText();
}
