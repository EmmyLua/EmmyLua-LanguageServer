package com.intellij.psi.impl.source.codeStyle;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;

public class CodeEditUtil {
    private static final Key<Boolean> GENERATED_FLAG = new Key<>("GENERATED_FLAG");
    private static final Key<Integer> INDENT_INFO = new Key<>("INDENT_INFO");
    private static final Key<Boolean> REFORMAT_BEFORE_KEY = new Key<>("REFORMAT_BEFORE_KEY");
    private static final Key<Boolean> REFORMAT_KEY = new Key<>("REFORMAT_KEY");

    public static void setNodeGenerated(final ASTNode next, final boolean value) {
        if (next == null) return;
        next.putCopyableUserData(GENERATED_FLAG, value ? true : null);
    }
}
