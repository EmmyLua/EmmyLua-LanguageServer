package com.intellij.codeInsight.lookup;

import org.eclipse.lsp4j.CompletionItem;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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

    public LookupElement withIcon(@Nullable Icon icon) {
        return this;
    }
}
