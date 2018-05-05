package com.intellij.codeInsight.lookup;

import org.eclipse.lsp4j.CompletionItem;

public class LookupElementBuilder {
    public static CompletionItem create(Object obj) {
        return new CompletionItem(obj.toString());
    }
}
