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
            // A freshly created buffer places the cursor at column 0, row 0,
            // which is the top-left corner of the screen.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 100);
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void newBuffer_screenIsAllSpaces() {
            // Every cell on the initial screen must be a blank space with default
            // attributes, so that the buffer starts in a visually empty state.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            for (int row = 0; row < 3; row++) {
                assertEquals("     ", buf.getLineAsString(row));
            }
        }

        @Test
        void newBuffer_scrollbackIsEmpty() {
            // No lines have been scrolled off the top yet, so the scrollback
            // buffer must be empty immediately after construction.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void newBuffer_currentAttributesAreDefault() {
            // The active write attributes start as TextAttributes.DEFAULT so that
            // text written before any attribute change is unstyled.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertEquals(TextAttributes.DEFAULT, buf.getCurrentAttributes());
        }

        @Test
        void newBuffer_dimensionAccessors() {
            // getWidth() and getHeight() must return exactly the dimensions
            // supplied to the constructor.
            TerminalBuffer buf = new TerminalBuffer(80, 24, 1000);
            assertEquals(80,   buf.getWidth());
            assertEquals(24,   buf.getHeight());
        }

        @Test
        void constructor_rejectsNonPositiveDimensions() {
            // A terminal with zero or negative width or height is nonsensical;
            // the constructor must reject such values with IllegalArgumentException.
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(0,  5, 0));
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(5,  0, 0));
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(-1, 5, 0));
        }

        @Test
        void constructor_rejectsNegativeScrollbackSize() {
            // A negative scrollback limit has no valid interpretation and must
            // be rejected with an IllegalArgumentException.
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(5, 3, -1));
        }

        @Test
        void constructor_acceptsZeroScrollbackSize() {
            // A scrollback size of zero is legal and means no lines are retained
            // after they scroll off the top of the screen.
            assertDoesNotThrow(() -> new TerminalBuffer(5, 3, 0));
        }
    }

    @Nested
    class Cursor {

        private TerminalBuffer buf;

        @BeforeEach
        void setUp() { buf = new TerminalBuffer(10, 5, 0); }

        @Test
        void setCursorPosition_updatesRowAndCol() {
            // setCursorPosition stores the given column and row and makes them
            // available through getCursorCol() and getCursorRow() respectively.
            buf.setCursorPosition(3, 2);
            assertEquals(3, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());
        }

        @Test
        void setCursorPosition_clampsNegativeValues() {
            // Negative coordinates are clamped to 0 so callers do not need to
            // guard against underflow when computing positions programmatically.
            buf.setCursorPosition(-1, -1);
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void setCursorPosition_clampsValuesAboveMax() {
            // Coordinates exceeding the screen bounds are clamped to the last
            // valid column (width-1) and row (height-1).
            buf.setCursorPosition(100, 100);
            assertEquals(9, buf.getCursorCol());
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        void moveCursorUp_decreasesRow() {
            // moveCursorUp subtracts n from the current row, moving the cursor
            // toward the top of the screen.
            buf.setCursorPosition(0, 3);
            buf.moveCursorUp(2);
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void moveCursorUp_clampsAtTopEdge() {
            // Moving up by more than the current row distance stops at row 0
            // rather than producing a negative row index.
            buf.moveCursorUp(100);
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void moveCursorDown_increasesRow() {
            // moveCursorDown adds n to the current row, moving the cursor
            // toward the bottom of the screen.
            buf.moveCursorDown(3);
            assertEquals(3, buf.getCursorRow());
        }

        @Test
        void moveCursorDown_clampsAtBottomEdge() {
            // Moving down by more than the remaining rows stops at height-1
            // rather than going past the bottom of the screen.
            buf.moveCursorDown(100);
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        void moveCursorLeft_decreasesCol() {
            // moveCursorLeft subtracts n from the current column, moving
            // the cursor toward the left edge of the screen.
            buf.setCursorPosition(5, 0);
            buf.moveCursorLeft(3);
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void moveCursorLeft_clampsAtLeftEdge() {
            // Moving left by more than the current column stops at column 0
            // rather than producing a negative column index.
            buf.moveCursorLeft(100);
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void moveCursorRight_increasesCol() {
            // moveCursorRight adds n to the current column, moving the cursor
            // toward the right edge of the screen.
            buf.moveCursorRight(4);
            assertEquals(4, buf.getCursorCol());
        }

        @Test
        void moveCursorRight_clampsAtRightEdge() {
            // Moving right by more than the remaining columns stops at width-1
            // rather than going past the right edge of the screen.
            buf.moveCursorRight(100);
            assertEquals(9, buf.getCursorCol());
        }
    }

    @Nested
    class Attributes {

        @Test
        void setCurrentAttributes_appliedToSubsequentWrites() {
            // After setCurrentAttributes, any text written to the buffer uses
            // those attributes, so the stored cell attributes reflect the change.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes bold = TextAttributes.builder().bold(true).build();
            buf.setCurrentAttributes(bold);
            buf.writeText("A");
            assertTrue(buf.getAttributesAt(0, 0).bold());
        }

        @Test
        void setCurrentAttributes_doesNotAffectAlreadyWrittenCells() {
            // Changing the current attributes only affects future writes; cells
            // already written keep the attributes they were written with.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("A");
            TextAttributes red = TextAttributes.builder().foreground(TerminalColor.RED).build();
            buf.setCurrentAttributes(red);
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(0, 0));
        }

        @Test
        void setCurrentAttributes_rejectsNull() {
            // Passing null as the new attributes must throw IllegalArgumentException
            // because every write must have a valid, non-null attribute set.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IllegalArgumentException.class, () -> buf.setCurrentAttributes(null));
        }

        @Test
        void setCurrentAttributes_allStyleFlags_storedCorrectly() {
            // All five attribute fields (foreground, background, bold, italic,
            // underline) must be stored and retrievable from the written cell.
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

    @Nested
    class WriteText {

        @Test
        void writeText_simpleString_appearsOnFirstLine() {
            // Writing a short string from the origin places each character in
            // the corresponding column of row 0, leaving remaining columns blank.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hello");
            assertEquals("hello     ", buf.getLineAsString(0));
        }

        @Test
        void writeText_advancesCursor() {
            // After writing n characters, the cursor column must be n positions
            // to the right of where it started, on the same row.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hello");
            assertEquals(5, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void writeText_overwritesExistingContent() {
            // Writing at a position that already has content replaces it;
            // the buffer does not insert — it overwrites in place.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hello");
            buf.setCursorPosition(0, 0);
            buf.writeText("world");
            assertEquals("world     ", buf.getLineAsString(0));
        }

        @Test
        void writeText_wrapsToNextLine() {
            // When writing reaches the end of a line, subsequent characters
            // continue on the next row starting at column 0.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.writeText("abcdefgh");
            assertEquals("abcde", buf.getLineAsString(0));
            assertEquals("fgh  ", buf.getLineAsString(1));
            assertEquals(1, buf.getCursorRow());
            assertEquals(3, buf.getCursorCol());
        }

        @Test
        void writeText_scrollsWhenReachingBottom() {
            // Writing past the last screen row triggers a scroll: the topmost
            // screen line moves to scrollback and a blank line appears at the bottom.
            // Due to the deferred-wrap mechanism each writeText that fills a row
            // triggers the actual wrap only when the next write begins.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("aaaaa");
            buf.writeText("bbbbb");
            buf.writeText("ccccc");
            buf.writeText("ddddd");
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("aaaaa", buf.getLineAsString(0));
            assertEquals("bbbbb", buf.getLineAsString(1));
            assertEquals("ddddd", buf.getLineAsString(buf.getScrollbackSize() + 2));
        }

        @Test
        void writeText_emptyString_doesNothing() {
            // An empty string is a no-op: cursor position and screen content
            // must remain unchanged after the call.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("");
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void writeText_null_doesNothing() {
            // A null argument is treated as a no-op, leaving cursor and content
            // unchanged, consistent with the empty-string behaviour.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText(null);
            assertEquals(0, buf.getCursorCol());
        }
    }

    @Nested
    class InsertText {

        @Test
        void insertText_intoEmptyLine_behavesLikeWrite() {
            // Inserting into an empty line has no existing content to shift,
            // so the result is identical to a plain writeText call.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.insertText("hello");
            assertEquals("hello     ", buf.getLineAsString(0));
        }

        @Test
        void insertText_shiftsExistingContentRight() {
            // Inserting text at a column where content already exists shifts
            // that content to the right by the number of inserted characters.
            TerminalBuffer buf = new TerminalBuffer(11, 5, 0);
            buf.writeText("world");
            buf.setCursorPosition(0, 0);
            buf.insertText("hello ");
            assertEquals("hello world", buf.getLineAsString(0));
        }

        @Test
        void insertText_advancesCursorPastInsertedChars() {
            // After inserting n characters, the cursor column advances by n,
            // landing just past the last inserted character.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.insertText("abc");
            assertEquals(3, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void insertText_overflowCascadesToNextLine() {
            // When inserting text pushes existing content past the right edge,
            // the displaced characters cascade to the beginning of the next line.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 100);
            buf.writeText("abcde");
            buf.setCursorPosition(0, 0);
            buf.insertText("X");
            assertEquals("Xabcd", buf.getLineAsString(buf.getScrollbackSize()));
            assertEquals('e',     buf.getCharAt(buf.getScrollbackSize() + 1, 0));
        }

        @Test
        void insertText_multiLineOverflow_cascadesCorrectly() {
            // When displacement cascades across multiple rows, each row receives
            // the overflow from the row above so that no content is silently lost
            // until it reaches the bottom of the screen.
            TerminalBuffer buf = new TerminalBuffer(3, 4, 100);
            buf.writeText("abcdefghi");
            buf.setCursorPosition(0, 0);
            buf.insertText("XYZ");
            int sb = buf.getScrollbackSize();
            assertEquals("XYZ", buf.getLineAsString(sb));
            assertEquals("abc", buf.getLineAsString(sb + 1));
            assertEquals("def", buf.getLineAsString(sb + 2));
        }

        @Test
        void insertText_scrollsWhenBottomLineOverflows() {
            // When cascade reaches the last screen row and there is still
            // non-empty overflow, the buffer scrolls to make room, moving the
            // top screen line into scrollback.
            TerminalBuffer buf = new TerminalBuffer(3, 2, 100);
            buf.writeText("abcdef");
            buf.setCursorPosition(0, 0);
            buf.insertText("X");
            assertTrue(buf.getScrollbackSize() >= 1);
        }

        @Test
        void insertText_emptyString_doesNothing() {
            // An empty string has no characters to insert, so cursor position
            // and screen content must remain unchanged after the call.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.insertText("");
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void insertText_null_doesNothing() {
            // A null argument is treated as a no-op, leaving cursor and screen
            // unchanged.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.insertText(null);
            assertEquals(0, buf.getCursorCol());
        }
    }

    @Nested
    class FillLine {

        @Test
        void fillLine_withChar_fillsCursorRow() {
            // fillLine(ch) overwrites every cell in the cursor's current row
            // with the given character using the active attributes.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.setCursorPosition(0, 2);
            buf.fillLine('-');
            assertEquals("-----", buf.getLineAsString(2));
        }

        @Test
        void fillLine_doesNotAffectOtherRows() {
            // fillLine operates only on the cursor row; rows above and below
            // must remain in their previous state.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.setCursorPosition(0, 2);
            buf.fillLine('-');
            assertEquals("     ", buf.getLineAsString(1));
            assertEquals("     ", buf.getLineAsString(3));
        }

        @Test
        void fillLine_withNull_clearsRow() {
            // Passing null to fillLine clears the current row, resetting every
            // cell to the empty codepoint with default attributes.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.writeText("hello");
            buf.setCursorPosition(0, 0);
            buf.fillLine(null);
            assertEquals("     ", buf.getLineAsString(0));
        }

        @Test
        void fillLine_usesCurrentAttributes() {
            // fillLine(ch) applies the current write attributes to every cell,
            // so all cells in the row share the same styling after the call.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            TextAttributes red = TextAttributes.builder().foreground(TerminalColor.RED).build();
            buf.setCurrentAttributes(red);
            buf.fillLine('=');
            assertEquals(TerminalColor.RED, buf.getAttributesAt(0, 0).foreground());
            assertEquals(TerminalColor.RED, buf.getAttributesAt(0, 4).foreground());
        }

        @Test
        void fillLine_withNull_usesDefaultAttributes() {
            // fillLine(null) always uses default attributes regardless of the
            // current write attributes, because clearing a line removes styling.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            TextAttributes bold = TextAttributes.builder().bold(true).build();
            buf.setCurrentAttributes(bold);
            buf.writeText("AAAAA");
            buf.setCursorPosition(0, 0);
            buf.fillLine(null);
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(0, 0));
        }
    }

    @Nested
    class ScrollbackBehaviour {

        @Test
        void insertEmptyLineAtBottom_movesTopLineToScrollback() {
            // insertEmptyLineAtBottom scrolls the screen: the topmost screen line
            // moves into scrollback and its content is preserved there.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("hello", buf.getLineAsString(0));
        }

        @Test
        void insertEmptyLineAtBottom_addsBlankLineAtBottom() {
            // After scrolling, a new blank line appears at the bottom of the
            // screen so the screen height remains constant.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            int lastScreenRow = buf.getScrollbackSize() + buf.getHeight() - 1;
            assertEquals("     ", buf.getLineAsString(lastScreenRow));
        }

        @Test
        void scrollback_screenHeightRemainsConstant() {
            // Repeated scrolling never changes the screen height; only the
            // scrollback buffer grows (up to its configured maximum).
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            assertEquals(3, buf.getHeight());
        }

        @Test
        void scrollback_respectsMaxSize() {
            // When the scrollback buffer is full, the oldest entry is evicted
            // before the new line is added, keeping size at the configured maximum.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 2);
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            assertEquals(2, buf.getScrollbackSize());
        }

        @Test
        void scrollback_oldestLineDiscardedWhenFull() {
            // When the scrollback is at capacity, the line that was scrolled in
            // first (oldest) is discarded and the second-oldest becomes the new head.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 2);
            buf.writeText("aaaaa"); buf.insertEmptyLineAtBottom();
            buf.writeText("bbbbb"); buf.insertEmptyLineAtBottom();
            buf.writeText("ccccc"); buf.insertEmptyLineAtBottom();
            assertEquals(2, buf.getScrollbackSize());
            assertEquals("bbbbb", buf.getLineAsString(0));
        }

        @Test
        void scrollback_zeroMaxSize_nothingStored() {
            // With a scrollback limit of zero, lines that scroll off the top are
            // immediately discarded and the scrollback size stays at zero.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void scrollback_contentIsPreservedAccurately() {
            // Once a line is in scrollback, subsequent writes to the screen must
            // not alter its content because scrollback is read-only.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            buf.writeText("world");
            assertEquals("hello", buf.getLineAsString(0));
        }
    }

    @Nested
    class ClearOperations {

        @Test
        void clearScreen_blanksAllScreenRows() {
            // clearScreen resets every cell on the visible screen to the empty
            // state, producing a screen that is entirely blank spaces.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("helloworld!!!!!");
            buf.clearScreen();
            for (int row = 0; row < 3; row++) {
                assertEquals("     ", buf.getLineAsString(row));
            }
        }

        @Test
        void clearScreen_resetsCursorToOrigin() {
            // clearScreen moves the cursor back to (0, 0) so that subsequent
            // writes start from the top-left of the newly cleared screen.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.setCursorPosition(3, 2);
            buf.clearScreen();
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void clearScreen_preservesScrollback() {
            // clearScreen affects only the visible screen; scrollback lines that
            // have already been committed must remain intact.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            buf.clearScreen();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("hello", buf.getLineAsString(0));
        }

        @Test
        void clearAll_blanksScreenAndScrollback() {
            // clearAll resets the entire buffer: both the visible screen and the
            // scrollback become empty, as if the buffer were freshly constructed.
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
            // clearAll moves the cursor to (0, 0) alongside clearing all content,
            // leaving the buffer in the same state as a freshly constructed one.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.setCursorPosition(4, 2);
            buf.clearAll();
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getCursorCol());
        }
    }

    @Nested
    class ContentAccess {

        @Test
        void getCharAt_returnsCorrectCharOnScreen() {
            // getCharAt with a global row that falls within the screen region
            // returns the character stored at that column, or a space for unwritten columns.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("hi");
            assertEquals('h', buf.getCharAt(0, 0));
            assertEquals('i', buf.getCharAt(0, 1));
            assertEquals(' ', buf.getCharAt(0, 2));
        }

        @Test
        void getCharAt_returnsCorrectCharInScrollback() {
            // getCharAt with a global row index in the scrollback region returns
            // the character stored in scrollback at that position.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertEquals('h', buf.getCharAt(0, 0));
            assertEquals('o', buf.getCharAt(0, 4));
        }

        @Test
        void getCodepointAt_returnFullCodepoint() {
            // getCodepointAt returns the full Unicode codepoint, which for BMP
            // characters matches the integer value of the Java char.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("A");
            assertEquals((int) 'A', buf.getCodepointAt(0, 0));
        }

        @Test
        void getAttributesAt_returnsCorrectAttributesOnScreen() {
            // getAttributesAt returns the TextAttributes stored in the cell at
            // the given position; cells not explicitly written have default attributes.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            TextAttributes underline = TextAttributes.builder().underline(true).build();
            buf.setCurrentAttributes(underline);
            buf.writeText("A");
            assertTrue(buf.getAttributesAt(0, 0).underline());
            assertEquals(TextAttributes.DEFAULT, buf.getAttributesAt(0, 1));
        }

        @Test
        void getAttributesAt_returnsCorrectAttributesInScrollback() {
            // Attributes stored in scrollback are accessible through getAttributesAt
            // using the global row index that maps to the scrollback region.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            TextAttributes bold = TextAttributes.builder().bold(true).build();
            buf.setCurrentAttributes(bold);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertTrue(buf.getAttributesAt(0, 0).bold());
        }

        @Test
        void getLineAsString_returnsFullWidthString() {
            // getLineAsString always returns a string padded to the full line width,
            // with unwritten positions represented as spaces.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            buf.writeText("hi");
            assertEquals("hi   ", buf.getLineAsString(0));
        }

        @Test
        void getLineAsString_fromScrollback() {
            // getLineAsString works for scrollback rows (global index < scrollback size)
            // and returns the line's content as it was when it scrolled off the screen.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
            buf.writeText("hello");
            buf.insertEmptyLineAtBottom();
            assertEquals("hello", buf.getLineAsString(0));
        }

        @Test
        void getScreenAsString_newlineSeparated() {
            // getScreenAsString joins all screen rows with newline characters.
            // Due to deferred-wrap semantics, writing exactly width characters fills
            // row 0 and the next write begins on row 1.
            TerminalBuffer buf = new TerminalBuffer(3, 2, 0);
            buf.writeText("abcdef");
            assertEquals("abc\ndef", buf.getScreenAsString());
        }

        @Test
        void getScreenAsString_doesNotIncludeScrollback() {
            // getScreenAsString returns only the visible screen; lines that have
            // scrolled into scrollback must not appear in its output.
            TerminalBuffer buf = new TerminalBuffer(3, 2, 100);
            buf.writeText("abc");
            buf.insertEmptyLineAtBottom();
            buf.writeText("def");
            String screen = buf.getScreenAsString();
            assertFalse(screen.startsWith("abc"));
        }

        @Test
        void getAllContentAsString_includesScrollbackThenScreen() {
            // getAllContentAsString concatenates scrollback followed by screen rows.
            // Scrollback content therefore appears before screen content in the result.
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
            // A negative global row index is always out of range and must cause
            // an IndexOutOfBoundsException.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(-1, 0));
        }

        @Test
        void getCharAt_rowBeyondTotal_throwsIndexOutOfBoundsException() {
            // A global row index at or beyond the total number of rows (scrollback
            // + screen) must cause an IndexOutOfBoundsException.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(3, 0));
        }

        @Test
        void getCharAt_columnOutOfBounds_throwsIndexOutOfBoundsException() {
            // A column index at or beyond the line width must cause an
            // IndexOutOfBoundsException.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharAt(0, 5));
        }

        @Test
        void getLineAsString_rowOutOfRange_throwsIndexOutOfBoundsException() {
            // Requesting a line at a global row index beyond the buffer's total
            // row count must throw IndexOutOfBoundsException.
            TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getLineAsString(99));
        }
    }

    @Nested
    class WideCharacters {

        @Test
        void writeText_wideChar_advancesCursorByTwo() {
            // A wide character occupies two display columns, so the cursor must
            // advance by 2 after writing one.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("中");
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void writeText_wideChar_setsLeadAndContCells() {
            // A wide character is stored as a WIDE_LEAD cell holding the codepoint
            // followed by a WIDE_CONT placeholder. getCharAt on the lead returns
            // the character; getCharAt on the cont returns a space.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("中");
            assertEquals(Cell.CellType.WIDE_LEAD, buf.getCodepointAt(0, 0) == '中'
                    ? Cell.CellType.WIDE_LEAD : Cell.CellType.NORMAL);
            assertEquals('中', buf.getCharAt(0, 0));
            assertEquals(' ',  buf.getCharAt(0, 1));
        }

        @Test
        void writeText_wideCharAtLastColumn_wrapsToNextLine() {
            // If the cursor is at the last column and the next character is wide,
            // there is no room for the continuation cell. The implementation pads
            // the last column with a space and places the wide character on the next line.
            TerminalBuffer buf = new TerminalBuffer(5, 5, 0);
            buf.setCursorPosition(4, 0);
            buf.writeText("中");
            assertEquals('中', buf.getCharAt(1, 0));
            assertEquals(2,    buf.getCursorCol());
            assertEquals(1,    buf.getCursorRow());
        }

        @Test
        void writeText_mixedNarrowAndWide() {
            // Mixing narrow and wide characters in a single write positions each
            // character correctly: narrow characters advance by 1, wide by 2.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("A中B");
            assertEquals('A',  buf.getCharAt(0, 0));
            assertEquals('中', buf.getCharAt(0, 1));
            assertEquals('B',  buf.getCharAt(0, 3));
            assertEquals(4,    buf.getCursorCol());
        }

        @Test
        void getLineAsString_wideChar_notDoubled() {
            // toContentString skips WIDE_CONT cells, so each wide character appears
            // exactly once in the returned string even though it occupies two columns.
            TerminalBuffer buf = new TerminalBuffer(6, 3, 0);
            buf.writeText("中文");
            String line = buf.getLineAsString(0);
            assertTrue(line.contains("中"));
            assertTrue(line.contains("文"));
            assertEquals(1, countOccurrences(line, "中"));
            assertEquals(1, countOccurrences(line, "文"));
        }

        @Test
        void insertText_wideChar_shiftsExistingContent() {
            // Inserting a wide character shifts existing content right by two
            // columns, keeping the WIDE_LEAD and WIDE_CONT pair together.
            TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
            buf.writeText("AB");
            buf.setCursorPosition(0, 0);
            buf.insertText("中");
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
