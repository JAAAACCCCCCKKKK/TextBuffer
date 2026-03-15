package com.terminal.buffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    @Nested
    class Initialisation {

        @Test
        void newBuffer_cursorAtOrigin() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 100);
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void newBuffer_screenIsAllSpaces() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            for (int row = 0; row < 3; row++) {
                assertEquals("     ", buf.getLineAsString(row));
            }
        }

        @Test
        void newBuffer_scrollbackIsEmpty() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void newBuffer_currentAttributesAreDefault() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertEquals(TextAttributes.DEFAULT, buf.getCurrentAttributes());
        }

        @Test
        void newBuffer_dimensionAccessors() {
            TerminalBuffer buf = new TerminalBuffer(80, 24, 1000);
            assertEquals(80,   buf.getWidth());
            assertEquals(24,   buf.getHeight());
        }

        @Test
        void constructor_rejectsNonPositiveDimensions() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(0,  5, 0));
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(5,  0, 0));
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(-1, 5, 0));
        }

        @Test
        void constructor_rejectsNegativeScrollbackSize() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(5, 3, -1));
        }

        @Test
        void constructor_acceptsZeroScrollbackSize() {
            assertDoesNotThrow(() -> new TerminalBuffer(5, 3, 0));
        }
    }

    // ─── Cursor ───────────────────────────────────────────────────────────────

    @Nested
    class Cursor {

        private TerminalBuffer buf;

        @BeforeEach
        void setUp() { buf = new TerminalBuffer(10, 5, 0); }

        @Test
        void setCursorPosition_updatesRowAndCol() {
            buf.setCursorPosition(3, 2);
            assertEquals(3, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());
        }

        @Test
        void setCursorPosition_clampsNegativeValues() {
            buf.setCursorPosition(-1, -1);
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void setCursorPosition_clampsValuesAboveMax() {
            buf.setCursorPosition(100, 100);
            assertEquals(9, buf.getCursorCol());
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        void moveCursorUp_decreasesRow() {
            buf.setCursorPosition(0, 3);
            buf.moveCursorUp(2);
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void moveCursorUp_clampsAtTopEdge() {
            buf.moveCursorUp(100);
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void moveCursorDown_increasesRow() {
            buf.moveCursorDown(3);
            assertEquals(3, buf.getCursorRow());
        }

        @Test
        void moveCursorDown_clampsAtBottomEdge() {
            buf.moveCursorDown(100);
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        void moveCursorLeft_decreasesCol() {
            buf.setCursorPosition(5, 0);
            buf.moveCursorLeft(3);
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void moveCursorLeft_clampsAtLeftEdge() {
            buf.moveCursorLeft(100);
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void moveCursorRight_increasesCol() {
            buf.moveCursorRight(4);
            assertEquals(4, buf.getCursorCol());
        }

        @Test
        void moveCursorRight_clampsAtRightEdge() {
            buf.moveCursorRight(100);
            assertEquals(9, buf.getCursorCol());
        }
    }

    // ─── Attributes ───────────────────────────────────────────────────────────

    @Nested
    class Attributes {

        @Test
        void setCurrentAttributes_appliedToSubsequentWrites() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes bold = TextAttributes.builder().bold(true).build();
            buf.setCurrentAttributes(bold);
            buf.writeText("A");
            assertTrue(buf.getAttributesAt(0, 0).bold());
        }

        @Test
        void setCurrentAttributes_doesNotAffectAlreadyWrittenCells() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("A");
            TextAttributes red = TextAttributes.builder().foreground(TerminalColor.RED).build();
            buf.setCurrentAttributes(red);
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(0, 0));
        }

        @Test
        void setCurrentAttributes_rejectsNull() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IllegalArgumentException.class, () -> buf.setCurrentAttributes(null));
        }

        @Test
        void setCurrentAttributes_allStyleFlags_storedCorrectly() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes attrs = new TextAttributes(
                    TerminalColor.RED, TerminalColor.BLUE, true, true, true);
            buf.setCurrentAttributes(attrs);
            buf.writeText("X");
            TextAttributes stored = buf.getAttributesAt(0, 0);
            assertEquals(TerminalColor.RED,  stored.foreground());
            assertEquals(TerminalColor.BLUE, stored.background());
            assertTrue(stored.bold());
            assertTrue(stored.italic());
            assertTrue(stored.underline());
        }
    }

    // ─── writeText ────────────────────────────────────────────────────────────

    @Nested
    class WriteText {

        @Test
        void writeText_simpleString_appearsOnFirstLine() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hello");
            assertEquals("hello     ", buf.getLineAsString(0));
        }

        @Test
        void writeText_advancesCursor() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hello");
            assertEquals(5, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void writeText_overwritesExistingContent() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hello");
            buf.setCursorPosition(0, 0);
            buf.writeText("world");
            assertEquals("world     ", buf.getLineAsString(0));
        }

        @Test
        void writeText_wrapsToNextLine() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.writeText("abcdefgh");
            assertEquals("abcde", buf.getLineAsString(0));
            assertEquals("fgh  ", buf.getLineAsString(1));
            assertEquals(1, buf.getCursorRow());
            assertEquals(3, buf.getCursorCol());
        }

        @Test
        void writeText_scrollsWhenReachingBottom() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("aaaaa"); // row 0
            buf.writeText("bbbbb"); // row 1
            buf.writeText("ccccc"); // row 2
            buf.writeText("ddddd"); // triggers scroll
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("aaaaa", buf.getLineAsString(0));               // in scrollback
            assertEquals("bbbbb", buf.getLineAsString(1));               // screen row 0
            assertEquals("ddddd", buf.getLineAsString(buf.getScrollbackSize() + 2)); // screen row 2
        }

        @Test
        void writeText_emptyString_doesNothing() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("");
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void writeText_null_doesNothing() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText(null);
            assertEquals(0, buf.getCursorCol());
        }
    }

    // ─── insertText ───────────────────────────────────────────────────────────

    @Nested
    class InsertText {

        @Test
        void insertText_intoEmptyLine_behavesLikeWrite() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.insertText("hello");
            assertEquals("hello     ", buf.getLineAsString(0));
        }

        @Test
        void insertText_shiftsExistingContentRight() {
            TerminalBuffer buf = new TerminalBuffer(11, 5, 0);
            buf.writeText("world");
            buf.setCursorPosition(0, 0);
            buf.insertText("hello ");
            assertEquals("hello world", buf.getLineAsString(0));
        }

        @Test
        void insertText_advancesCursorPastInsertedChars() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.insertText("abc");
            assertEquals(3, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void insertText_overflowCascadesToNextLine() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 100);
            buf.writeText("abcde"); // fill row 0
            buf.setCursorPosition(0, 0);
            buf.insertText("X");   // 'e' pushed to row 1
            assertEquals("Xabcd", buf.getLineAsString(buf.getScrollbackSize()));
            assertEquals('e',     buf.getCharAt(buf.getScrollbackSize() + 1, 0));
        }

        @Test
        void insertText_multiLineOverflow_cascadesCorrectly() {
            TerminalBuffer buf = new TerminalBuffer(3, 4, 100);
            buf.writeText("abcdefghi"); // fills rows 0-2: "abc","def","ghi"
            buf.setCursorPosition(0, 0);
            buf.insertText("XYZ"); // push everything 3 positions right across rows
            int sb = buf.getScrollbackSize();
            assertEquals("XYZ", buf.getLineAsString(sb));
            assertEquals("abc", buf.getLineAsString(sb + 1));
            assertEquals("def", buf.getLineAsString(sb + 2));
        }

        @Test
        void insertText_scrollsWhenBottomLineOverflows() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 100);
            buf.writeText("abcdef"); // "abc" / "def"
            buf.setCursorPosition(0, 0);
            buf.insertText("X");
            assertTrue(buf.getScrollbackSize() >= 1);
        }

        @Test
        void insertText_null_doesNothing() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.insertText(null);
            assertEquals(0, buf.getCursorCol());
        }
    }

    // ─── fillLine ─────────────────────────────────────────────────────────────

    @Nested
    class FillLine {

        @Test
        void fillLine_withChar_fillsCursorRow() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.setCursorPosition(0, 2);
            buf.fillLine('-');
            assertEquals("-----", buf.getLineAsString(2));
        }

        @Test
        void fillLine_doesNotAffectOtherRows() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.setCursorPosition(0, 2);
            buf.fillLine('-');
            assertEquals("     ", buf.getLineAsString(1));
            assertEquals("     ", buf.getLineAsString(3));
        }

        @Test
        void fillLine_withNull_clearsRow() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.writeText("hello");
            buf.setCursorPosition(0, 0);
            buf.fillLine(null);
            assertEquals("     ", buf.getLineAsString(0));
        }

        @Test
        void fillLine_usesCurrentAttributes() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            TextAttributes red = TextAttributes.builder().foreground(TerminalColor.RED).build();
            buf.setCurrentAttributes(red);
            buf.fillLine('=');
            assertEquals(TerminalColor.RED, buf.getAttributesAt(0, 0).foreground());
            assertEquals(TerminalColor.RED, buf.getAttributesAt(0, 4).foreground());
        }

        @Test
        void fillLine_withNull_usesDefaultAttributes() {
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            TextAttributes bold = TextAttributes.builder().bold(true).build();
            buf.setCurrentAttributes(bold);
            buf.writeText("AAAAA");
            buf.setCursorPosition(0, 0);
            buf.fillLine(null);
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(0, 0));
        }
    }

    // ─── insertEmptyLineAtBottom / scrollback ─────────────────────────────────

    @Nested
    class ScrollbackBehaviour {

        @Test
        void insertEmptyLineAtBottom_movesTopLineToScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("hello", buf.getLineAsString(0));
        }

        @Test
        void insertEmptyLineAtBottom_addsBlankLineAtBottom() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            int lastScreenRow = buf.getScrollbackSize() + buf.getHeight() - 1;
            assertEquals("     ", buf.getLineAsString(lastScreenRow));
        }

        @Test
        void scrollback_screenHeightRemainsConstant() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            assertEquals(3, buf.getHeight());
        }

        @Test
        void scrollback_respectsMaxSize() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 2);
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom(); // should evict oldest
            assertEquals(2, buf.getScrollbackSize());
        }

        @Test
        void scrollback_oldestLineDiscardedWhenFull() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 2);
            buf.writeText("aaaaa"); buf.insertEmptyLineAtBottom(); // scrollback: "aaaaa"
            buf.writeText("bbbbb"); buf.insertEmptyLineAtBottom(); // scrollback: "aaaaa","bbbbb"
            buf.writeText("ccccc"); buf.insertEmptyLineAtBottom(); // "aaaaa" evicted
            assertEquals(2, buf.getScrollbackSize());
            assertEquals("bbbbb", buf.getLineAsString(0)); // oldest surviving
        }

        @Test
        void scrollback_zeroMaxSize_nothingStored() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void scrollback_contentIsPreservedAccurately() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            // Modify current screen — scrollback must be unaffected
            buf.writeText("world");
            assertEquals("hello", buf.getLineAsString(0));
        }
    }

    // ─── clearScreen / clearAll ───────────────────────────────────────────────

    @Nested
    class ClearOperations {

        @Test
        void clearScreen_blanksAllScreenRows() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("helloworld!!!!!");
            buf.clearScreen();
            for (int row = 0; row < 3; row++) {
                assertEquals("     ", buf.getLineAsString(row));
            }
        }

        @Test
        void clearScreen_resetsCursorToOrigin() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.setCursorPosition(3, 2);
            buf.clearScreen();
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void clearScreen_preservesScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            buf.clearScreen();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("hello", buf.getLineAsString(0));
        }

        @Test
        void clearAll_blanksScreenAndScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            buf.writeText("world");
            buf.clearAll();
            assertEquals(0, buf.getScrollbackSize());
            assertEquals("     ", buf.getLineAsString(0));
        }

        @Test
        void clearAll_resetsCursorToOrigin() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.setCursorPosition(4, 2);
            buf.clearAll();
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getCursorCol());
        }
    }

    // ─── Content access ───────────────────────────────────────────────────────

    @Nested
    class ContentAccess {

        @Test
        void getCharAt_returnsCorrectCharOnScreen() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hi");
            assertEquals('h', buf.getCharAt(0, 0));
            assertEquals('i', buf.getCharAt(0, 1));
            assertEquals(' ', buf.getCharAt(0, 2));
        }

        @Test
        void getCharAt_returnsCorrectCharInScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertEquals('h', buf.getCharAt(0, 0));
            assertEquals('o', buf.getCharAt(0, 4));
        }

        @Test
        void getCodepointAt_returnFullCodepoint() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("A");
            assertEquals((int) 'A', buf.getCodepointAt(0, 0));
        }

        @Test
        void getAttributesAt_returnsCorrectAttributesOnScreen() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes underline = TextAttributes.builder().underline(true).build();
            buf.setCurrentAttributes(underline);
            buf.writeText("A");
            assertTrue(buf.getAttributesAt(0, 0).underline());
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(0, 1));
        }

        @Test
        void getAttributesAt_returnsCorrectAttributesInScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            TextAttributes bold = TextAttributes.builder().bold(true).build();
            buf.setCurrentAttributes(bold);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertTrue(buf.getAttributesAt(0, 0).bold());
        }

        @Test
        void getLineAsString_returnsFullWidthString() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("hi");
            assertEquals("hi   ", buf.getLineAsString(0));
        }

        @Test
        void getLineAsString_fromScrollback() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertEquals("hello", buf.getLineAsString(0));
        }

        @Test
        void getScreenAsString_newlineSeparated() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 0);
            buf.writeText("abcdef");
            assertEquals("abc\ndef", buf.getScreenAsString());
        }

        @Test
        void getScreenAsString_doesNotIncludeScrollback() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 100);
            buf.writeText("abc");
            buf.insertEmptyLineAtBottom();
            buf.writeText("def");
            String screen = buf.getScreenAsString();
            assertFalse(screen.startsWith("abc")); // "abc" is now in scrollback
        }

        @Test
        void getAllContentAsString_includesScrollbackThenScreen() {
            TerminalBuffer buf = new TerminalBuffer(3, 2, 100);
            buf.writeText("abc");
            buf.insertEmptyLineAtBottom();
            buf.writeText("def");
            String all = buf.getAllContentAsString();
            int abcIdx = all.indexOf("abc");
            int defIdx = all.indexOf("def");
            assertTrue(abcIdx >= 0);
            assertTrue(defIdx > abcIdx);
        }

        @Test
        void getCharAt_negativeRow_throwsIndexOutOfBoundsException() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(-1, 0));
        }

        @Test
        void getCharAt_rowBeyondTotal_throwsIndexOutOfBoundsException() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(3, 0));
        }

        @Test
        void getCharAt_columnOutOfBounds_throwsIndexOutOfBoundsException() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(0, 5));
        }

        @Test
        void getLineAsString_rowOutOfRange_throwsIndexOutOfBoundsException() {
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getLineAsString(99));
        }
    }

    // ─── Wide characters ──────────────────────────────────────────────────────

    @Nested
    class WideCharacters {

        @Test
        void writeText_wideChar_advancesCursorByTwo() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("中");
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void writeText_wideChar_setsLeadAndContCells() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("中");
            assertEquals(Cell.CellType.WIDE_LEAD, buf.getCodepointAt(0, 0) == '中'
                    ? Cell.CellType.WIDE_LEAD : Cell.CellType.NORMAL);
            assertEquals('中', buf.getCharAt(0, 0));
            assertEquals(' ',  buf.getCharAt(0, 1)); // continuation placeholder
        }

        @Test
        void writeText_wideCharAtLastColumn_wrapsToNextLine() {
            // Width 5: cursor at col 4 (last col), wide char needs 2 → must wrap
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.setCursorPosition(4, 0);
            buf.writeText("中");
            // Wide char should be on row 1 col 0
            assertEquals('中', buf.getCharAt(1, 0));
            assertEquals(2,    buf.getCursorCol());
            assertEquals(1,    buf.getCursorRow());
        }

        @Test
        void writeText_mixedNarrowAndWide() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("A中B");
            assertEquals('A',  buf.getCharAt(0, 0));
            assertEquals('中', buf.getCharAt(0, 1));
            assertEquals('B',  buf.getCharAt(0, 3));
            assertEquals(4,    buf.getCursorCol());
        }

        @Test
        void getLineAsString_wideChar_notDoubled() {
            TerminalBuffer buf = new TerminalBuffer(6, 3, 0);
            buf.writeText("中文");
            String line = buf.getLineAsString(0);
            // "中文" takes 4 cols, remaining 2 cols are spaces.
            // toContentString skips WIDE_CONT, so we get "中文  " (4 chars in Java)
            assertTrue(line.contains("中"));
            assertTrue(line.contains("文"));
            assertEquals(1, countOccurrences(line, "中"));
            assertEquals(1, countOccurrences(line, "文"));
        }

        @Test
        void insertText_wideChar_shiftsExistingContent() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("AB");
            buf.setCursorPosition(0, 0);
            buf.insertText("中"); // inserts 2 cols before "AB"
            assertEquals('中', buf.getCharAt(0, 0));
            assertEquals('A',  buf.getCharAt(0, 2));
            assertEquals('B',  buf.getCharAt(0, 3));
        }

        private int countOccurrences(String s, String sub) {
            int count = 0, idx = 0;
            while ((idx = s.indexOf(sub, idx)) != -1) { count++; idx++; }
            return count;
        }
    }
}