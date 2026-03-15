package com.terminal.buffer;

/**
 * Utility methods for determining the display width of Unicode codepoints.
 *
 * <p>Terminal emulators follow the East Asian Width property defined in Unicode:
 * characters classified as Wide or Fullwidth occupy 2 columns; all others occupy 1.
 *
 * <p>We approximate this using {@link Character#getType} ranges and explicit
 * block checks rather than pulling in an external Unicode library, which keeps
 * the project dependency-free while covering the most common cases (CJK
 * ideographs, Hangul, and emoji in the BMP + supplementary planes).
 */
public final class WideCharUtil {

    private WideCharUtil() {}

    /**
     * Returns {@code true} if {@code codepoint} should occupy 2 terminal columns.
     */
    public static boolean isWide(int codepoint) {
        // Supplementary-plane emoji and symbols (U+1F000 and above)
        if (codepoint >= 0x1F000) return true;

        // CJK Unified Ideographs and common extensions
        if (codepoint >= 0x4E00 && codepoint <= 0x9FFF) return true;   // CJK Unified Ideographs
        if (codepoint >= 0x3400 && codepoint <= 0x4DBF) return true;   // CJK Extension A
        if (codepoint >= 0x20000 && codepoint <= 0x2A6DF) return true; // CJK Extension B
        if (codepoint >= 0xF900 && codepoint <= 0xFAFF) return true;   // CJK Compatibility Ideographs

        // Hangul syllables and Jamo
        if (codepoint >= 0xAC00 && codepoint <= 0xD7AF) return true;   // Hangul Syllables
        if (codepoint >= 0x1100 && codepoint <= 0x11FF) return true;   // Hangul Jamo
        if (codepoint >= 0xA960 && codepoint <= 0xA97F) return true;   // Hangul Jamo Extended-A
        if (codepoint >= 0xD7B0 && codepoint <= 0xD7FF) return true;   // Hangul Jamo Extended-B

        // Fullwidth and Halfwidth Forms (fullwidth Latin, fullwidth digits, etc.)
        if (codepoint >= 0xFF01 && codepoint <= 0xFF60) return true;
        if (codepoint >= 0xFFE0 && codepoint <= 0xFFE6) return true;

        // CJK Symbols and Punctuation, Hiragana, Katakana
        if (codepoint >= 0x3000 && codepoint <= 0x303F) return true;   // CJK Symbols & Punctuation
        if (codepoint >= 0x3040 && codepoint <= 0x309F) return true;   // Hiragana
        if (codepoint >= 0x30A0 && codepoint <= 0x30FF) return true;   // Katakana
        if (codepoint >= 0x31F0 && codepoint <= 0x31FF) return true;   // Katakana Phonetic Extensions

        // Enclosed CJK, CJK Compatibility, etc.
        if (codepoint >= 0x3200 && codepoint <= 0x32FF) return true;
        if (codepoint >= 0x3300 && codepoint <= 0x33FF) return true;
        if (codepoint >= 0xFE30 && codepoint <= 0xFE4F) return true;   // CJK Compatibility Forms

        return false;
    }

    /**
     * Returns the display column width of {@code codepoint}: 2 for wide, 1 for all others.
     */
    public static int displayWidth(int codepoint) {
        return isWide(codepoint) ? 2 : 1;
    }
}