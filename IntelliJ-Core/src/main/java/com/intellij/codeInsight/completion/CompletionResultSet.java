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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import org.jetbrains.annotations.NotNull;

/**
 * {@link com.intellij.codeInsight.completion.CompletionResultSet}s feed on {@link com.intellij.codeInsight.lookup.LookupElement}s,
 * match them against specified
 * {@link com.intellij.codeInsight.completion.PrefixMatcher} and give them to special {@link com.intellij.util.Consumer}
 * (see {@link CompletionService#createResultSet(CompletionParameters, com.intellij.util.Consumer, CompletionContributor)})
 * for further processing, which usually means
 * they will sooner or later appear in completion list. If they don't, there must be some {@link CompletionContributor}
 * up the invocation stack that filters them out.
 * <p>
 * If you want to change the matching prefix, use {@link #withPrefixMatcher(PrefixMatcher)} or {@link #withPrefixMatcher(String)}
 * to obtain another {@link com.intellij.codeInsight.completion.CompletionResultSet} and give your lookup elements to that one.
 *
 * @author peter
 */
public abstract class CompletionResultSet {
    private boolean myStopped;

    private PrefixMatcher prefixMatcher;

    public void stopHere() {
        myStopped = true;
    }

    public boolean isStopped() {
        return myStopped;
    }

    @NotNull public abstract CompletionResultSet withPrefixMatcher(@NotNull String prefix);

    public abstract void addElement(LookupElement item);

    public void setPrefixMatcher(PrefixMatcher prefixMatcher) {
        this.prefixMatcher = prefixMatcher;
    }

    public PrefixMatcher getPrefixMatcher() {
        return prefixMatcher;
    }

    public void restartCompletionOnAnyPrefixChange() {

    }
}
