package com.intellij.openapi.progress;

import org.eclipse.lsp4j.jsonrpc.CancelChecker;

public class ProgressManager {

    private static CancelChecker ourCancelChecker;

    public static void setCancelChecker(CancelChecker checker) {
        ourCancelChecker = checker;
    }

    public static void checkCanceled() {
        if (ourCancelChecker != null)
            ourCancelChecker.checkCanceled();
    }
}
