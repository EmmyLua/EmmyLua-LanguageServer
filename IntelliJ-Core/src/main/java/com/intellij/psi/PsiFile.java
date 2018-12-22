/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * A PSI element representing a file.
 * <p/>
 * Please see <a href="http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview">IntelliJ IDEA Architectural Overview </a>
 * for high-level overview.
 *
 * @see com.intellij.openapi.actionSystem.LangDataKeys#PSI_FILE
 * @see PsiElement#getContainingFile()
 * @see PsiManager#findFile(VirtualFile)
 * @see PsiDocumentManager#getPsiFile(com.intellij.openapi.editor.Document)
 */
public interface PsiFile extends PsiNamedElement {
    /**
     * The empty array of PSI files which can be reused to avoid unnecessary allocations.
     */
    PsiFile[] EMPTY_ARRAY = new PsiFile[0];

    /**
     * If the file is a non-physical copy of a file, returns the original file which had
     * been copied. Otherwise, returns the same file.
     *
     * @return the original file of a copy, or the same file if the file is not a copy.
     */
    @NotNull
    PsiFile getOriginalFile();

    @NotNull
    @NonNls
    String getName();

    @NotNull
    VirtualFile getVirtualFile();

    Long getModificationStamp();
}
