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

import com.intellij.openapi.editor.Caret;
import com.intellij.psi.PsiFile;

/**
 * @author peter
 */
public class CompletionInitializationContext {
    public static String DUMMY_IDENTIFIER = "emmy ";
    public static String DUMMY_IDENTIFIER_TRIMMED = "emmy";

    private String dummyIdentifier;

    private PsiFile file;

    private int startOffset;

    private Caret caret;

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public Caret getCaret() {
        return caret;
    }

    public void setCaret(Caret caret) {
        this.caret = caret;
    }

    public PsiFile getFile() {
        return file;
    }

    public void setFile(PsiFile file) {
        this.file = file;
    }

    public String getDummyIdentifier() {
        return dummyIdentifier;
    }

    public void setDummyIdentifier(String dummyIdentifier) {
        this.dummyIdentifier = dummyIdentifier;
    }
}
