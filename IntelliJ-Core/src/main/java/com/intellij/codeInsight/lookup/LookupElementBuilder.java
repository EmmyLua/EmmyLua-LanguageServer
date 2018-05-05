package com.intellij.codeInsight.lookup;

public class LookupElementBuilder {
    public static LookupElement create(Object obj) {
        return new LookupElement(obj.toString());
    }
}
