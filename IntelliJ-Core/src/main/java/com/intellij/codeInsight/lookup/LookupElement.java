package com.intellij.codeInsight.lookup;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LookupElement {

    private String lookupString;

    private String itemText;

    public String getItemText() {
        return itemText;
    }

    public void setItemText(String itemText) {
        this.itemText = itemText;
    }

    public String getLookupString() {
        return lookupString;
    }

    public void setLookupString(String lookupString) {
        this.lookupString = lookupString;
    }

    public LookupElement(String label) {
        //super(label);
        this.lookupString = label;
    }

    public boolean isCaseSensitive() {
        return false;
    }

    public String[] getAllLookupStrings() {
        return new String[]{ this.lookupString };
    }

    public LookupElement withIcon(@Nullable Icon icon) {
        return this;
    }
}
