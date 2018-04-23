package com.intellij.psi;

import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public abstract class PsiReferenceProvider {
    @NotNull
    public abstract PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context);
}
