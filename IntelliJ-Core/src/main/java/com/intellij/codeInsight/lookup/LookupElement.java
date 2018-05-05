package com.intellij.codeInsight.lookup;

import org.eclipse.lsp4j.CompletionItem;

public class LookupElement extends CompletionItem {

    public LookupElement(String label) {
        super(label);
    }

    public boolean isCaseSensitive() {
        return false;
    }

    public String[] getAllLookupStrings() {
        return new String[]{ this.getLabel() };
    }
}
