/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.openapi.util.io;

import com.intellij.openapi.util.text.StringUtilRt;
import org.jetbrains.annotations.NotNull;

/**
 * Stripped-down version of {@code com.intellij.openapi.util.io.FileUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public class FileUtilRt {


  private static String ensureEnds(@NotNull String s, final char endsWith) {
    return StringUtilRt.endsWithChar(s, endsWith) ? s : s + endsWith;
  }

  @NotNull
  public static CharSequence getNameWithoutExtension(@NotNull CharSequence name) {
    int i = StringUtilRt.lastIndexOf(name, '.', 0, name.length());
    return i < 0 ? name : name.subSequence(0, i);
  }

  @NotNull
  public static String getNameWithoutExtension(@NotNull String name) {
    return getNameWithoutExtension((CharSequence)name).toString();
  }
}