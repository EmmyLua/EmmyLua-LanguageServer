/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.psi.text;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolder;

public abstract class BlockSupport {
    /*public static BlockSupport getInstance(Project project) {
        return ServiceManager.getService(project, BlockSupport.class);
    }*/

    /*public abstract void reparseRange(@NotNull PsiFile file, int startOffset, int endOffset, @NonNls @NotNull CharSequence newText) throws IncorrectOperationException;

    @NotNull
    public abstract DiffLog reparseRange(@NotNull PsiFile file,
                                         @NotNull FileASTNode oldFileNode,
                                         @NotNull TextRange changedPsiRange,
                                         @NotNull CharSequence newText,
                                         @NotNull ProgressIndicator progressIndicator,
                                         @NotNull CharSequence lastCommittedText) throws IncorrectOperationException;

    public static final Key<Boolean> DO_NOT_REPARSE_INCREMENTALLY = Key.create("DO_NOT_REPARSE_INCREMENTALLY");
    public static final Key<Pair<ASTNode, CharSequence>> TREE_TO_BE_REPARSED = Key.create("TREE_TO_BE_REPARSED");

    public static class ReparsedSuccessfullyException extends RuntimeException implements ControlFlowException {
        private final DiffLog myDiffLog;

        public ReparsedSuccessfullyException(@NotNull DiffLog diffLog) {
            myDiffLog = diffLog;
        }

        @NotNull
        public DiffLog getDiffLog() {
            return myDiffLog;
        }

        @NotNull
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }*/


    public static final Key<Boolean> DO_NOT_REPARSE_INCREMENTALLY = Key.create("DO_NOT_REPARSE_INCREMENTALLY");
    public static final Key<Pair<ASTNode, CharSequence>> TREE_TO_BE_REPARSED = Key.create("TREE_TO_BE_REPARSED");
    // maximal tree depth for which incremental reparse is allowed
    // if tree is deeper then it will be replaced completely - to avoid SOEs
    public static final int INCREMENTAL_REPARSE_DEPTH_LIMIT = 100000;//Registry.intValue("psi.incremental.reparse.depth.limit");

    public static final Key<Boolean> TREE_DEPTH_LIMIT_EXCEEDED = Key.create("TREE_IS_TOO_DEEP");

    public static boolean isTooDeep(final UserDataHolder element) {
        return element != null && Boolean.TRUE.equals(element.getUserData(TREE_DEPTH_LIMIT_EXCEEDED));
    }
}
