// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * Utility wrappers for accessing system properties.
 *
 * @author yole
 * @see SystemInfo
 */
public class SystemProperties {
    private static String ourTestUserName;

    private SystemProperties() {
    }

    @NotNull
    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    public static String getUserName() {
        return ourTestUserName != null ? ourTestUserName : System.getProperty("user.name");
    }

    @TestOnly
    public static void setTestUserName(@Nullable String name) {
        ourTestUserName = name;
    }

    public static String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public static String getJavaHome() {
        return System.getProperty("java.home");
    }

    /**
     * Returns the value of given property as integer, or {@code defaultValue} if the property is not specified or malformed.
     */
    public static int getIntProperty(@NotNull final String key, final int defaultValue) {
        final String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }

        return defaultValue;
    }

    /**
     * Returns the value of given property as a boolean, or {@code defaultValue} if the property is not specified or malformed.
     */
    public static boolean getBooleanProperty(@NotNull final String key, final boolean defaultValue) {
        final String value = System.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return defaultValue;
    }

    public static boolean is(String key) {
        return getBooleanProperty(key, false);
    }

    public static boolean has(String key) {
        return System.getProperty(key) != null;
    }

    public static boolean isTrueSmoothScrollingEnabled() {
        return getBooleanProperty("idea.true.smooth.scrolling", false);
    }
}