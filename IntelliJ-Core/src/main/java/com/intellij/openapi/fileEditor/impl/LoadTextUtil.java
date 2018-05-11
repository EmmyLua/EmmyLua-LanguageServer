/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.ByteArrayCharSequence;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public final class LoadTextUtil {

    private LoadTextUtil() {
    }

    @NotNull
    private static ConvertResult convertLineSeparatorsToSlashN(@NotNull CharBuffer buffer) {
        int dst = 0;
        char prev = ' ';
        int crCount = 0;
        int lfCount = 0;
        int crlfCount = 0;

        final int length = buffer.length();
        final char[] bufferArray = CharArrayUtil.fromSequenceWithoutCopying(buffer);

        for (int src = 0; src < length; src++) {
            char c = bufferArray != null ? bufferArray[src] : buffer.charAt(src);
            switch (c) {
                case '\r':
                    if (bufferArray != null) bufferArray[dst++] = '\n';
                    else buffer.put(dst++, '\n');
                    crCount++;
                    break;
                case '\n':
                    if (prev == '\r') {
                        crCount--;
                        crlfCount++;
                    } else {
                        if (bufferArray != null) bufferArray[dst++] = '\n';
                        else buffer.put(dst++, '\n');
                        lfCount++;
                    }
                    break;
                default:
                    if (bufferArray != null) bufferArray[dst++] = c;
                    else buffer.put(dst++, c);
                    break;
            }
            prev = c;
        }

        String detectedLineSeparator = guessLineSeparator(crCount, lfCount, crlfCount);

        CharSequence result = buffer.length() == dst ? buffer : buffer.subSequence(0, dst);
        return new ConvertResult(result, detectedLineSeparator);
    }

    @NotNull
    private static ConvertResult convertLineSeparatorsToSlashN(@NotNull byte[] charsAsBytes, int startOffset, int endOffset) {
        int index = ArrayUtil.indexOf(charsAsBytes, (byte) '\r', startOffset, endOffset);
        if (index == -1) {
            // optimisation: if there is no CR in the file, no line separator conversion is necessary. we can re-use the passed byte buffer in place
            ByteArrayCharSequence sequence = new ByteArrayCharSequence(charsAsBytes, startOffset, endOffset);
            String detectedLineSeparator = ArrayUtil.indexOf(charsAsBytes, (byte) '\n', startOffset, endOffset) == -1 ? null : "\n";
            return new ConvertResult(sequence, detectedLineSeparator);
        }
        int dst = 0;
        char prev = ' ';
        int crCount = 0;
        int lfCount = 0;
        int crlfCount = 0;
        byte[] result = new byte[endOffset - startOffset];

        for (int src = startOffset; src < endOffset; src++) {
            char c = (char) charsAsBytes[src];
            switch (c) {
                case '\r':
                    result[dst++] = '\n';

                    crCount++;
                    break;
                case '\n':
                    if (prev == '\r') {
                        crCount--;
                        crlfCount++;
                    } else {
                        result[dst++] = '\n';
                        lfCount++;
                    }
                    break;
                default:
                    result[dst++] = (byte) c;
                    break;
            }
            prev = c;
        }

        String detectedLineSeparator = guessLineSeparator(crCount, lfCount, crlfCount);

        ByteArrayCharSequence sequence = new ByteArrayCharSequence(result, 0, dst);
        return new ConvertResult(sequence, detectedLineSeparator);
    }

    @Nullable
    private static String guessLineSeparator(int crCount, int lfCount, int crlfCount) {
        String detectedLineSeparator = null;
        if (crlfCount > crCount && crlfCount > lfCount) {
            detectedLineSeparator = "\r\n";
        } else if (crCount > lfCount) {
            detectedLineSeparator = "\r";
        } else if (lfCount > 0) {
            detectedLineSeparator = "\n";
        }
        return detectedLineSeparator;
    }

    // private fake charsets for files which have one-byte-for-ascii-characters encoding but contain seven bits characters only. used for optimization since we don't have to encode-decode bytes here.
    private static final Charset INTERNAL_SEVEN_BIT_UTF8 = new SevenBitCharset(CharsetToolkit.UTF8_CHARSET);
    private static final Charset INTERNAL_SEVEN_BIT_ISO_8859_1 = new SevenBitCharset(CharsetToolkit.ISO_8859_1_CHARSET);
    private static final Charset INTERNAL_SEVEN_BIT_WIN_1251 = new SevenBitCharset(CharsetToolkit.WIN_1251_CHARSET);

    private static class SevenBitCharset extends Charset {
        private final Charset myBaseCharset;

        /**
         * should be {@code this.name().contains(CharsetToolkit.UTF8)} for {@link #getOverriddenCharsetByBOM(byte[], Charset)} to work
         */
        SevenBitCharset(Charset baseCharset) {
            super("IJ__7BIT_" + baseCharset.name(), ArrayUtil.EMPTY_STRING_ARRAY);
            myBaseCharset = baseCharset;
        }

        @Override
        public boolean contains(Charset cs) {
            throw new IllegalStateException();
        }

        @Override
        public CharsetDecoder newDecoder() {
            throw new IllegalStateException();
        }

        @Override
        public CharsetEncoder newEncoder() {
            throw new IllegalStateException();
        }
    }

    public static class DetectResult {
        public Charset hardCodedCharset;
        public final CharsetToolkit.GuessedEncoding guessed;
        @Nullable
        public final byte[] BOM;

        DetectResult(Charset hardCodedCharset, CharsetToolkit.GuessedEncoding guessed, @Nullable byte[] BOM) {
            this.hardCodedCharset = hardCodedCharset;
            this.guessed = guessed;
            this.BOM = BOM;
        }
    }

    // guess from file type or content
    @NotNull
    private static DetectResult detectHardCharset(@NotNull byte[] content,
                                                  int length) {
        DetectResult guessed = guessFromContent(content, length);
        Charset hardCodedCharset = guessed.hardCodedCharset;

        if (hardCodedCharset == null && guessed.guessed == CharsetToolkit.GuessedEncoding.VALID_UTF8) {
            return new DetectResult(CharsetToolkit.UTF8_CHARSET, guessed.guessed, guessed.BOM);
        }
        return new DetectResult(hardCodedCharset, guessed.guessed, guessed.BOM);
    }

    private static final boolean GUESS_UTF = Boolean.parseBoolean(System.getProperty("idea.guess.utf.encoding", "true"));

    @NotNull
    private static DetectResult guessFromContent(@NotNull byte[] content, int length) {
        DetectResult info;
        if (GUESS_UTF) {
            info = guessFromBytes(content, 0, length, Charset.defaultCharset());
        } else {
            info = new DetectResult(null, null, null);
        }
        return info;
    }

    @NotNull
    private static DetectResult guessFromBytes(@NotNull byte[] content,
                                               int startOffset, int endOffset,
                                               @NotNull Charset defaultCharset) {
        CharsetToolkit toolkit = new CharsetToolkit(content, defaultCharset);
        toolkit.setEnforce8Bit(true);
        Charset charset = toolkit.guessFromBOM();
        if (charset != null) {
            byte[] bom = ObjectUtils.notNull(CharsetToolkit.getMandatoryBom(charset), CharsetToolkit.UTF8_BOM);
            return new DetectResult(charset, null, bom);
        }
        CharsetToolkit.GuessedEncoding guessed = toolkit.guessFromContent(startOffset, endOffset);
        if (guessed == CharsetToolkit.GuessedEncoding.VALID_UTF8) {
            return new DetectResult(CharsetToolkit.UTF8_CHARSET, CharsetToolkit.GuessedEncoding.VALID_UTF8, null); //UTF detected, ignore all directives
        }
        return new DetectResult(null, guessed, null);
    }

    @NotNull
    private static DetectResult detectInternalCharsetAndSetBOM(@NotNull byte[] content,
                                                               int length) {
        DetectResult info = detectHardCharset(content, length);

        Charset charset = info.hardCodedCharset;

        byte[] bom = info.BOM;

        Charset result = charset;
        // optimisation
        if (info.guessed == CharsetToolkit.GuessedEncoding.SEVEN_BIT) {
            if (charset == CharsetToolkit.UTF8_CHARSET) {
                result = INTERNAL_SEVEN_BIT_UTF8;
            } else if (charset == CharsetToolkit.ISO_8859_1_CHARSET) {
                result = INTERNAL_SEVEN_BIT_ISO_8859_1;
            } else if (charset == CharsetToolkit.WIN_1251_CHARSET) {
                result = INTERNAL_SEVEN_BIT_WIN_1251;
            }
        }

        return new DetectResult(result, info.guessed, bom);
    }


    @NotNull
    private static Pair.NonNull<Charset, byte[]> getOverriddenCharsetByBOM(@NotNull byte[] content, @NotNull Charset charset) {
        if (charset.name().contains(CharsetToolkit.UTF8) && CharsetToolkit.hasUTF8Bom(content)) {
            return Pair.createNonNull(charset, CharsetToolkit.UTF8_BOM);
        }
        Charset charsetFromBOM = CharsetToolkit.guessFromBOM(content);
        if (charsetFromBOM != null) {
            byte[] bom = ObjectUtils.notNull(CharsetToolkit.getMandatoryBom(charsetFromBOM), ArrayUtil.EMPTY_BYTE_ARRAY);
            return Pair.createNonNull(charsetFromBOM, bom);
        }

        return Pair.createNonNull(charset, ArrayUtil.EMPTY_BYTE_ARRAY);
    }

    @NotNull
    public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes) {
        DetectResult info = detectInternalCharsetAndSetBOM(bytes, bytes.length);
        byte[] bom = info.BOM;
        if (info.hardCodedCharset == null) {
            CharsetToolkit toolkit = new CharsetToolkit(bytes);
            info.hardCodedCharset = toolkit.guessEncoding(10);
        }
        ConvertResult result = convertBytes(bytes, Math.min(bom == null ? 0 : bom.length, bytes.length), bytes.length,
                info.hardCodedCharset);

        return result.text;
    }

    @NotNull
    public static CharSequence getTextByBinaryPresentation(@NotNull byte[] bytes, @NotNull Charset charset) {
        Pair.NonNull<Charset, byte[]> pair = getOverriddenCharsetByBOM(bytes, charset);
        byte[] bom = pair.getSecond();

        final ConvertResult result = convertBytes(bytes, Math.min(bom.length, bytes.length), bytes.length, pair.first);
        return result.text;
    }

    @NotNull
    private static ConvertResult convertBytes(@NotNull byte[] bytes,
                                              final int startOffset, int endOffset,
                                              @NotNull Charset internalCharset) {
        assert startOffset >= 0 && startOffset <= endOffset && endOffset <= bytes.length : startOffset + "," + endOffset + ": " + bytes.length;
        if (internalCharset instanceof SevenBitCharset || internalCharset == CharsetToolkit.US_ASCII_CHARSET) {
            // optimisation: skip byte-to-char conversion for ascii chars
            return convertLineSeparatorsToSlashN(bytes, startOffset, endOffset);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, startOffset, endOffset - startOffset);

        CharBuffer charBuffer;
        try {
            charBuffer = internalCharset.decode(byteBuffer);
        } catch (Exception e) {
            // esoteric charsets can throw any kind of exception
            charBuffer = CharBuffer.wrap(ArrayUtil.EMPTY_CHAR_ARRAY);
        }
        return convertLineSeparatorsToSlashN(charBuffer);
    }

    private static class ConvertResult {
        @NotNull
        private final CharSequence text;
        @Nullable
        private final String lineSeparator;

        ConvertResult(@NotNull CharSequence text, @Nullable String lineSeparator) {
            this.text = text;
            this.lineSeparator = lineSeparator;
        }
    }
}
