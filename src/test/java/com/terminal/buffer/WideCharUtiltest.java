package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WideCharUtilTest {

    // ── isWide: narrow characters ─────────────────────────────────────────────

    @Test
    void isWide_asciiLetter_returnsFalse() {
        // Standard ASCII characters occupy exactly one terminal column.
        assertFalse(WideCharUtil.isWide('A'));
    }

    @Test
    void isWide_asciiDigit_returnsFalse() {
        // Digits are narrow single-column characters.
        assertFalse(WideCharUtil.isWide('0'));
    }

    @Test
    void isWide_latinExtended_returnsFalse() {
        // Latin extended characters such as accented letters are narrow.
        assertFalse(WideCharUtil.isWide('é'));
    }

    // ── isWide: CJK ranges ────────────────────────────────────────────────────

    @Test
    void isWide_cjkUnifiedIdeograph_returnsTrue() {
        // U+4E2D ('中') is a CJK Unified Ideograph (U+4E00–U+9FFF) and occupies
        // two terminal columns.
        assertTrue(WideCharUtil.isWide('中'));
    }

    @Test
    void isWide_cjkUnifiedIdeograph_upperBound_returnsTrue() {
        // U+9FFF is the last codepoint in the CJK Unified Ideograph block.
        assertTrue(WideCharUtil.isWide(0x9FFF));
    }

    @Test
    void isWide_cjkExtensionA_returnsTrue() {
        // U+3400 is the first codepoint of CJK Extension A (U+3400–U+4DBF).
        assertTrue(WideCharUtil.isWide(0x3400));
    }

    @Test
    void isWide_cjkCompatibilityIdeographs_returnsTrue() {
        // U+F900 is the first codepoint of CJK Compatibility Ideographs (U+F900–U+FAFF).
        assertTrue(WideCharUtil.isWide(0xF900));
    }

    @Test
    void isWide_cjkSymbolsAndPunctuation_returnsTrue() {
        // U+3000 (ideographic space) is in CJK Symbols and Punctuation (U+3000–U+303F).
        assertTrue(WideCharUtil.isWide(0x3000));
    }

    @Test
    void isWide_enclosedCjkLettersAndMonths_returnsTrue() {
        // U+3200 is the first codepoint of Enclosed CJK Letters and Months (U+3200–U+32FF).
        assertTrue(WideCharUtil.isWide(0x3200));
    }

    @Test
    void isWide_cjkCompatibilityBlock_returnsTrue() {
        // U+3300 is the first codepoint of CJK Compatibility (U+3300–U+33FF).
        assertTrue(WideCharUtil.isWide(0x3300));
    }

    @Test
    void isWide_cjkCompatibilityForms_returnsTrue() {
        // U+FE30 is the first codepoint of CJK Compatibility Forms (U+FE30–U+FE4F).
        assertTrue(WideCharUtil.isWide(0xFE30));
    }

    // ── isWide: Hangul ranges ─────────────────────────────────────────────────

    @Test
    void isWide_hangulSyllable_returnsTrue() {
        // U+AC00 (가) is the first Hangul Syllable (U+AC00–U+D7AF).
        assertTrue(WideCharUtil.isWide(0xAC00));
    }

    @Test
    void isWide_hangulJamo_returnsTrue() {
        // U+1100 (ᄀ) is the first codepoint of Hangul Jamo (U+1100–U+11FF).
        assertTrue(WideCharUtil.isWide(0x1100));
    }

    @Test
    void isWide_hangulJamoExtendedA_returnsTrue() {
        // U+A960 is the first codepoint of Hangul Jamo Extended-A (U+A960–U+A97F).
        assertTrue(WideCharUtil.isWide(0xA960));
    }

    @Test
    void isWide_hangulJamoExtendedB_returnsTrue() {
        // U+D7B0 is the first codepoint of Hangul Jamo Extended-B (U+D7B0–U+D7FF).
        assertTrue(WideCharUtil.isWide(0xD7B0));
    }

    // ── isWide: Japanese scripts ──────────────────────────────────────────────

    @Test
    void isWide_hiragana_returnsTrue() {
        // U+3041 (ぁ) is in the Hiragana block (U+3040–U+309F).
        assertTrue(WideCharUtil.isWide(0x3041));
    }

    @Test
    void isWide_katakana_returnsTrue() {
        // U+30A1 (ァ) is in the Katakana block (U+30A0–U+30FF).
        assertTrue(WideCharUtil.isWide(0x30A1));
    }

    @Test
    void isWide_katakanaPhoneticExtensions_returnsTrue() {
        // U+31F0 is the first codepoint of Katakana Phonetic Extensions (U+31F0–U+31FF).
        assertTrue(WideCharUtil.isWide(0x31F0));
    }

    // ── isWide: Fullwidth and currency forms ──────────────────────────────────

    @Test
    void isWide_fullwidthExclamationMark_returnsTrue() {
        // U+FF01 (！) is a Fullwidth Form (U+FF01–U+FF60) and occupies two columns.
        assertTrue(WideCharUtil.isWide(0xFF01));
    }

    @Test
    void isWide_fullwidthCurrencySign_returnsTrue() {
        // U+FFE0 (￠) is in the Fullwidth Currency Symbols block (U+FFE0–U+FFE6).
        assertTrue(WideCharUtil.isWide(0xFFE0));
    }

    // ── isWide: Supplementary plane ───────────────────────────────────────────

    @Test
    void isWide_supplementaryEmoji_returnsTrue() {
        // U+1F600 (😀) is above U+1F000 and matches the supplementary-plane
        // emoji rule, the first check evaluated.
        assertTrue(WideCharUtil.isWide(0x1F600));
    }

    @Test
    void isWide_supplementaryPlane_lowerBound_returnsTrue() {
        // U+1F000 is the exact lower bound of the supplementary-plane wide rule.
        assertTrue(WideCharUtil.isWide(0x1F000));
    }

    // ── displayWidth ──────────────────────────────────────────────────────────

    @Test
    void displayWidth_wideChar_returnsTwo() {
        // displayWidth is a thin wrapper: it returns 2 for any wide codepoint.
        assertEquals(2, WideCharUtil.displayWidth('中'));
    }

    @Test
    void displayWidth_narrowChar_returnsOne() {
        // displayWidth returns 1 for any narrow codepoint.
        assertEquals(1, WideCharUtil.displayWidth('A'));
    }
}
