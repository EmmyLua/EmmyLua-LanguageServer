package com.intellij.psi.impl.source.resolve.reference;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReferenceProvidersRegistry {

    private static List<RefData> patterns = new ArrayList<>();

    static class RefData {
        ElementPattern<? extends PsiElement> pattern;
        PsiReferenceProvider provider;
    }

    public static void register(PsiReferenceContributor contributor) {
        contributor.registerReferenceProviders(new PsiReferenceRegistrar() {
            @Override
            public void registerReferenceProvider(@NotNull ElementPattern<? extends PsiElement> pattern, @NotNull PsiReferenceProvider provider) {
                RefData ref = new RefData();
                ref.pattern = pattern;
                ref.provider = provider;
                patterns.add(ref);
            }
        });
    }

    public static PsiReference[] getReferencesFromProviders(PsiElement context, Class<?> clazz) {
        return getReferencesFromProviders(context);
    }

    public static PsiReference[] getReferencesFromProviders(PsiElement context) {
        ProcessingContext processingContext = new ProcessingContext();
        for (RefData pattern : patterns) {
            if (pattern.pattern.accepts(context)) {
                return pattern.provider.getReferencesByElement(context, processingContext);
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
