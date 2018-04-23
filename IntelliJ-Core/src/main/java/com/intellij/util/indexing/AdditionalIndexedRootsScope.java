package com.intellij.util.indexing;

import com.intellij.psi.search.GlobalSearchScope;

public class AdditionalIndexedRootsScope extends GlobalSearchScope {
    public AdditionalIndexedRootsScope(GlobalSearchScope baseScope) {
        //super(baseScope);
    }

    public AdditionalIndexedRootsScope(GlobalSearchScope baseScope, Class<? extends IndexableSetContributor> providerClass) {
        this(baseScope);
    }
}
