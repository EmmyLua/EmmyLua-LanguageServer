package com.intellij.psi;

import org.jetbrains.annotations.NotNull;

public abstract class PsiReferenceContributor {
    public abstract void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar);
}
