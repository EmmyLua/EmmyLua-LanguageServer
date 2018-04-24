package com.intellij.psi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiManagerImpl extends PsiManagerEx {

    private static PsiManagerImpl instance = new PsiManagerImpl();

    public static PsiManager getInstance() {
        return instance;
    }

    @Override
    public boolean isBatchFilesProcessingMode() {
        return false;
    }

    @Override
    public void registerRunnableToRunOnChange(@NotNull Runnable runnable) {

    }

    @Override
    public void registerRunnableToRunOnAnyChange(@NotNull Runnable runnable) {

    }

    @Override
    public void registerRunnableToRunAfterAnyChange(@NotNull Runnable runnable) {

    }

    @Override
    public void beforeChange(boolean isPhysical) {

    }

    @Override
    public void afterChange(boolean isPhysical) {

    }

    @NotNull
    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public boolean areElementsEquivalent(@Nullable PsiElement element1, @Nullable PsiElement element2) {
        if (element1 == element2) return true;
        if (element1 == null || element2 == null) {
            return false;
        }

        return element1.equals(element2) || element1.isEquivalentTo(element2) || element2.isEquivalentTo(element1);
    }

    @Nullable
    @Override
    public PsiFile findFile(@NotNull VirtualFile file) {
        return file.getPsiFile();
    }
}
