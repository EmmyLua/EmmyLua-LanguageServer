package com.intellij.psi.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public class CachedValuesManager {

    public static CachedValuesManager instance = new CachedValuesManager();

    public static CachedValuesManager getManager(Project project) {
        return instance;
    }

    public <T, D extends UserDataHolder, P> T getParameterizedCachedValue(@NotNull D dataHolder,
                                                                          @NotNull Key<ParameterizedCachedValue<T,P>> key,
                                                                          @NotNull ParameterizedCachedValueProvider<T, P> provider,
                                                                          boolean trackValue,
                                                                          P parameter) {
        CachedValueProvider.Result<T> result = provider.compute(parameter);
        if (result != null)
            return result.getValue();
        return null;
    }

    public static <T> T getCachedValue(@NotNull final PsiElement psi, @NotNull Key<CachedValue<T>> key, @NotNull final CachedValueProvider<T> provider) {
        CachedValue<T> value = psi.getUserData(key);
        if (value != null) {
            return value.getValue();
        }
        CachedValueProvider.Result<T> r = provider.compute();
        if (r != null)
            return r.getValue();
        return null;
    }
}
