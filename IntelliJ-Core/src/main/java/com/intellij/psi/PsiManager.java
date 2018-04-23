/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.PsiManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main entry point for accessing the PSI services for a project.
 */
public abstract class PsiManager extends UserDataHolderBase {

    /**
     * Returns the PSI manager instance for the specified project.
     *
     * @param project the project for which the PSI manager is requested.
     * @return the PSI manager instance.
     */
    @NotNull
    public static PsiManager getInstance(@NotNull Project project) {
        return PsiManagerImpl.getInstance();
    }

    /**
     * Returns the project with which the PSI manager is associated.
     *
     * @return the project instance.
     */
    @NotNull
    public abstract Project getProject();

    /**
     * Checks if the specified two PSI elements (possibly invalid) represent the same source element
     * or can are considered equivalent for resolve purposes. Can be used to match two versions of the
     * PSI tree with each other after a reparse.<p/>
     * <p>
     * For example, Java classes with the same full-qualified name are equivalent, which is useful when working
     * with both library source and class roots. Source and compiled classes are definitely different ({@code equals()} returns false),
     * but for reference resolve or inheritance checks they're equivalent.
     *
     * @param element1 the first element to check for equivalence
     * @param element2 the second element to check for equivalence
     * @return true if the elements are equivalent, false if the elements are different or
     * it was not possible to determine the equivalence
     */
    public abstract boolean areElementsEquivalent(@Nullable PsiElement element1, @Nullable PsiElement element2);

    /**
     * Returns the PSI file corresponding to the specified virtual file.
     *
     * @param file the file for which the PSI is requested.
     * @return the PSI file, or null if {@code file} is a directory, an invalid virtual file,
     * or the current project is a dummy or default project.
     */
    @Nullable
    public abstract PsiFile findFile(@NotNull VirtualFile file);
}
