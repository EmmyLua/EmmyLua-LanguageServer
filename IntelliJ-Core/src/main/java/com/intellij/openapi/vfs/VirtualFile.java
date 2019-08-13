package com.intellij.openapi.vfs;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public interface VirtualFile {
    @NotNull
    String getName();

    CharSequence getText();

    PsiFile getPsiFile();

    String getPath();
}
