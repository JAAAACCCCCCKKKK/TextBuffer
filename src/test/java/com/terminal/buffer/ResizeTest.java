package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResizeTest {

    @Test
    void resize_rejectsNonPositiveDimensions() {
        // A terminal dimension of zero or negative is invalid; resize must throw
        // IllegalArgumentException rather than silently producing a malformed buffer.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
        assertThrows(IllegalArgumentException.class, () -> buf.resize(0,  3));
        assertThrows(IllegalArgumentException.class, () -> buf.resize(5,  0));
        assertThrows(IllegalArgumentException.class, () -> buf.resize(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> buf.resize(5, -1));
    }

    @Test
    void resize_getWidthAndHeightReflectNewSize() {
        // After resize, getWidth() and getHeight() must return the new dimensions
        // rather than the original ones.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
        buf.resize(12, 6);
        assertEquals(12, buf.getWidth());
        assertEquals(6,  buf.getHeight());
    }

    @Test
    void resize_sameSize_dimensionsAndContentUnchanged() {
        // Resizing to the same dimensions is a no-op: width, height, and all
        // screen content remain exactly as they were before the call.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
        buf.writeText("hello");
        buf.resize(5, 3);
        assertEquals(5, buf.getWidth());
        assertEquals(3, buf.getHeight());
        assertEquals("hello", buf.getLineAsString(buf.getScrollbackSize()));
    }

    @Test
    void resize_wider_existingContentPreservedAndNewColsBlank() {
        // When width increases, existing characters stay in their original columns
        // and the new columns to the right are filled with blank spaces.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
        buf.writeText("hello");
        buf.resize(8, 3);
        assertEquals("hello   ", buf.getLineAsString(buf.getScrollbackSize()));
    }

    @Test
    void resize_wider_allScreenLinesGrow() {
        // Every screen line is extended when the width grows, not just the line
        // that the cursor is on. All lines gain the same number of blank columns.
        TerminalBuffer buf = new TerminalBuffer(3, 3, 0);
        buf.writeText("abc");
        buf.writeText("def");
        buf.resize(5, 3);
        int sb = buf.getScrollbackSize();
        assertEquals("abc  ", buf.getLineAsString(sb));
        assertEquals("def  ", buf.getLineAsString(sb + 1));
    }

    @Test
    void resize_wider_scrollbackLinesAlsoPadded() {
        // Scrollback lines are resized together with screen lines so that the
        // buffer is consistent at all global row indices after the resize.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
        buf.writeText("hello");
        buf.insertEmptyLineAtBottom();
        buf.resize(8, 3);
        assertEquals("hello   ", buf.getLineAsString(0));
    }

    @Test
    void resize_narrower_contentTruncated() {
        // When width decreases, characters beyond the new right edge are discarded.
        // The line length reported by getLineAsString matches the new width.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
        buf.writeText("hello");
        buf.resize(3, 3);
        assertEquals("hel", buf.getLineAsString(buf.getScrollbackSize()));
        assertEquals(3, buf.getWidth());
    }

    @Test
    void resize_narrower_scrollbackLinesTruncated() {
        // Scrollback lines are truncated to the new width along with screen lines,
        // keeping all lines consistent with the new buffer width.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
        buf.writeText("hello");
        buf.insertEmptyLineAtBottom();
        buf.resize(3, 3);
        assertEquals("hel", buf.getLineAsString(0));
    }

    @Test
    void resize_narrower_wideLeadAtNewLastColumnCleared() {
        // If truncation leaves a WIDE_LEAD cell at the new last column with its
        // WIDE_CONT cut off, that cell must be cleared to avoid a half-wide character.
        // Layout before resize: A(0) B(1) 中-LEAD(2) 中-CONT(3) space(4) space(5)
        // After truncation to 3: col 2 is WIDE_LEAD whose CONT was cut — cleared to space.
        TerminalBuffer buf = new TerminalBuffer(6, 3, 0);
        buf.writeText("AB中");
        buf.resize(3, 3);
        int sb = buf.getScrollbackSize();
        assertEquals('A', buf.getCharAt(sb, 0));
        assertEquals('B', buf.getCharAt(sb, 1));
        assertEquals(' ', buf.getCharAt(sb, 2));
    }

    @Test
    void resize_narrower_intactWidePairPreserved() {
        // When both the WIDE_LEAD and WIDE_CONT cells of a pair fall within the
        // new width, the pair is kept intact and the character remains readable.
        // Truncation to 4: WIDE_LEAD(2) and WIDE_CONT(3) both fit — kept.
        TerminalBuffer buf = new TerminalBuffer(6, 3, 0);
        buf.writeText("AB中");
        buf.resize(4, 3);
        int sb = buf.getScrollbackSize();
        assertEquals('中', buf.getCharAt(sb, 2));
        assertEquals(' ',  buf.getCharAt(sb, 3));
    }

    @Test
    void resize_taller_blankLinesAppendedAtBottom() {
        // When height increases, new blank lines are appended at the bottom of
        // the screen. Existing content stays at the top of the screen region.
        TerminalBuffer buf = new TerminalBuffer(5, 2, 0);
        buf.writeText("hello");
        buf.resize(5, 4);
        int sb = buf.getScrollbackSize();
        assertEquals("hello", buf.getLineAsString(sb));
        assertEquals("     ", buf.getLineAsString(sb + 1));
        assertEquals("     ", buf.getLineAsString(sb + 2));
        assertEquals("     ", buf.getLineAsString(sb + 3));
        assertEquals(4, buf.getHeight());
    }

    @Test
    void resize_taller_scrollbackUnaffected() {
        // Increasing the screen height does not change the scrollback: lines
        // previously committed to scrollback remain there unchanged.
        TerminalBuffer buf = new TerminalBuffer(5, 2, 100);
        buf.writeText("hello");
        buf.insertEmptyLineAtBottom();
        int sbBefore = buf.getScrollbackSize();
        buf.resize(5, 4);
        assertEquals(sbBefore, buf.getScrollbackSize());
    }

    @Test
    void resize_shorter_bottomLinesRemoved() {
        // When height decreases, lines are removed from the bottom of the screen.
        // The lines that remain are the ones that were at the top of the original screen.
        // Each writeText that exactly fills a row defers the wrap, so the next
        // writeText triggers it, effectively filling rows sequentially.
        TerminalBuffer buf = new TerminalBuffer(5, 4, 0);
        buf.writeText("aaaaa");
        buf.writeText("bbbbb");
        buf.writeText("ccccc");
        buf.writeText("ddddd");
        buf.resize(5, 2);
        int sb = buf.getScrollbackSize();
        assertEquals(2, buf.getHeight());
        assertEquals("aaaaa", buf.getLineAsString(sb));
        assertEquals("bbbbb", buf.getLineAsString(sb + 1));
    }

    @Test
    void resize_shorter_scrollbackUnaffected() {
        // Decreasing the screen height does not affect lines already in scrollback;
        // only screen rows beyond the new height are discarded.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 100);
        buf.writeText("hello");
        buf.insertEmptyLineAtBottom();
        int sbBefore = buf.getScrollbackSize();
        buf.resize(5, 2);
        assertEquals(sbBefore, buf.getScrollbackSize());
    }

    @Test
    void resize_cursorClampedToNewBounds() {
        // If the cursor position exceeds the new dimensions it is clamped to
        // the last valid column (newWidth-1) and row (newHeight-1).
        TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
        buf.setCursorPosition(9, 4);
        buf.resize(4, 3);
        assertEquals(3, buf.getCursorCol());
        assertEquals(2, buf.getCursorRow());
    }

    @Test
    void resize_cursorWithinNewBounds_unmoved() {
        // If the cursor position is already within the new dimensions it must
        // not be changed by the resize operation.
        TerminalBuffer buf = new TerminalBuffer(10, 5, 0);
        buf.setCursorPosition(2, 1);
        buf.resize(8, 4);
        assertEquals(2, buf.getCursorCol());
        assertEquals(1, buf.getCursorRow());
    }

    @Test
    void resize_cancelsPendingWrap() {
        // When a row is completely filled, the buffer defers the wrap (pendingWrap=true)
        // so the cursor stays at the last column. resize() must cancel this pending
        // wrap; the next write then lands at the current cursor column rather than
        // advancing to the next row first.
        TerminalBuffer buf = new TerminalBuffer(5, 3, 0);
        buf.writeText("hello");
        buf.resize(5, 3);
        buf.writeText("X");
        int sb = buf.getScrollbackSize();
        assertEquals('X', buf.getCharAt(sb, 4));
        assertEquals(0,   buf.getCursorRow());
    }
}
