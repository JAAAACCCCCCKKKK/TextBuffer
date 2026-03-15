package com.terminal.buffer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Core terminal text buffer.
 *
 * <p>Maintains two logical regions:
 * <ul>
 *   <li><b>Screen</b> — the visible area of {@code height} lines. All edits happen here.</li>
 *   <li><b>Scrollback</b> — lines that have scrolled off the top of the screen.
 *       Read-only; capped at {@code scrollbackMaxSize} lines.</li>
 * </ul>
 *
 * <p>Wide characters (CJK ideographs, emoji) occupy 2 columns. A wide character
 * is stored as a {@link Cell.CellType#WIDE_LEAD} cell followed immediately by a
 * {@link Cell.CellType#WIDE_CONT} placeholder. The cursor advances by 2 after
 * writing a wide character.
 *
 * <p>Global row addressing used by content-access methods:
 * <pre>
 *   0 .. scrollback.size()-1          → scrollback (oldest first)
 *   scrollback.size() .. total-1      → screen (top to bottom)
 * </pre>
 */
public class TerminalBuffer {

    private final int width;
    private final int height;
    private final int scrollbackMaxSize;

    private final List<Line> screen;
    private final Deque<Line> scrollback;

    private int cursorRow;
    private int cursorCol;
    private boolean pendingWrap;
    private TextAttributes currentAttributes;

    public TerminalBuffer(int initialWidth, int initialHeight, int scrollbackMaxSize) {
        if (initialWidth <= 0)     throw new IllegalArgumentException("Width must be positive, got: " + initialWidth);
        if (initialHeight <= 0)    throw new IllegalArgumentException("Height must be positive, got: " + initialHeight);
        if (scrollbackMaxSize < 0) throw new IllegalArgumentException("Scrollback max size must be >= 0, got: " + scrollbackMaxSize);

        this.width = initialWidth;
        this.height = initialHeight;
        this.scrollbackMaxSize = scrollbackMaxSize;

        this.screen = new ArrayList<>(initialHeight);
        for (int i = 0; i < initialHeight; i++) {
            screen.add(new Line(width));
        }

        this.scrollback = new ArrayDeque<>();
        this.cursorRow = 0;
        this.cursorCol = 0;
        this.pendingWrap = false;
        this.currentAttributes = TextAttributes.DEFAULT;
    }

    /** Replaces the current write attributes. All subsequent edits use these. */
    public void setCurrentAttributes(TextAttributes attributes) {
        if (attributes == null) throw new IllegalArgumentException("Attributes must not be null");
        this.currentAttributes = attributes;
    }

    public TextAttributes getCurrentAttributes() {
        return currentAttributes;
    }

    public int getCursorRow() { return cursorRow; }
    public int getCursorCol() { return cursorCol; }

    /** Moves the cursor to the given screen position, clamping to valid bounds. */
    public void setCursorPosition(int col, int row) {
        pendingWrap = false;
        cursorRow = clamp(row, 0, height - 1);
        cursorCol = clamp(col, 0, width - 1);
    }

    public void moveCursorUp(int n)    { cursorRow = clamp(cursorRow - n, 0, height - 1); }
    public void moveCursorDown(int n)  { cursorRow = clamp(cursorRow + n, 0, height - 1); }
    public void moveCursorLeft(int n)  { cursorCol = clamp(cursorCol - n, 0, width - 1); }
    public void moveCursorRight(int n) { cursorCol = clamp(cursorCol + n, 0, width - 1); }

    /**
     * Writes {@code text} starting at the current cursor position, overwriting
     * existing content. Wraps to subsequent lines at end-of-line.
     * Scrolls the screen upward if writing past the last line.
     *
     * <p>Wide characters advance the cursor by 2 columns. If a wide character
     * would start at the last column of a line, a space is written there instead
     * and the wide character is placed at column 0 of the next line.
     */
    public void writeText(String text) {
        if (text == null || text.isEmpty()) return;

        int i = 0;
        while (i < text.length()) {
            int cp    = text.codePointAt(i);
            i        += Character.charCount(cp);
            int cpWidth = WideCharUtil.displayWidth(cp);

            // Fire deferred wrap from the previous character before writing.
            if (pendingWrap) {
                pendingWrap = false;
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scroll();
                    cursorRow = height - 1;
                }
            }

            // If a wide char doesn't fit at the end of the line, pad with space and wrap.
            if (cpWidth == 2 && cursorCol == width - 1) {
                screen.get(cursorRow).setNarrowCell(cursorCol, Cell.EMPTY_CODEPOINT, currentAttributes);
                cursorCol = 0;
                cursorRow++;
                if (cursorRow >= height) {
                    scroll();
                    cursorRow = height - 1;
                }
            }

            if (cpWidth == 2) {
                screen.get(cursorRow).setWideCell(cursorCol, cp, currentAttributes);
            } else {
                screen.get(cursorRow).setNarrowCell(cursorCol, cp, currentAttributes);
            }

            cursorCol += cpWidth;

            if (cursorCol >= width) {
                // Defer the wrap: fire it when the next character is written.
                pendingWrap = true;
                cursorCol = width - 1;
            }
        }
    }

    /**
     * Inserts {@code text} at the current cursor position, shifting existing
     * content to the right. Overflow cascades to subsequent lines.
     * Scrolls the screen when the bottom line overflows.
     *
     * <p>Wide characters consume 2 pending slots in the queue. Continuation
     * cells are carried through the cascade together with their lead cell.
     */
    public void insertText(String text) {
        if (text == null || text.isEmpty()) return;

        // Build the pending queue from the input text.
        // Wide chars add a WIDE_LEAD cell followed by a WIDE_CONT cell so that
        // the pair always travels together through the cascade.
        Deque<Cell> pending = new ArrayDeque<>();
        int i = 0;
        while (i < text.length()) {
            int cp  = text.codePointAt(i);
            i      += Character.charCount(cp);
            if (WideCharUtil.isWide(cp)) {
                pending.addLast(new Cell(cp,currentAttributes, Cell.CellType.WIDE_LEAD));
                pending.addLast(new Cell(Cell.EMPTY_CODEPOINT, currentAttributes, Cell.CellType.WIDE_CONT));
            } else {
                pending.addLast(new Cell(cp, currentAttributes, Cell.CellType.NORMAL));
            }
        }

        int insertRow = cursorRow;
        int insertCol = cursorCol;

        while (!pending.isEmpty()) {
            Line line = screen.get(insertRow);

            int available = width - insertCol;
            int toWrite   = Math.min(pending.size(), available);

            // Capture the cells about to be displaced off the right edge.
            Deque<Cell> overflow = new ArrayDeque<>();
            for (int col = width - toWrite; col < width; col++) {
                Cell src = line.getCell(col);
                overflow.addLast(new Cell(src.getCodepoint(), src.getAttributes(), src.getType()));
            }

            // Shift existing content right by toWrite positions to make room.
            for (int col = width - 1; col >= insertCol + toWrite; col--) {
                Cell src = line.getCell(col - toWrite);
                line.getCell(col).set(src.getCodepoint(), src.getAttributes(), src.getType());
            }

            // Write pending cells into the freed slots.
            for (int col = insertCol; col < insertCol + toWrite; col++) {
                Cell c = pending.pollFirst();
                line.getCell(col).set(c.getCodepoint(), c.getAttributes(), c.getType());
            }

            insertCol += toWrite;

            boolean overflowHasContent = overflow.stream()
                    .anyMatch(c -> c.getCodepoint() != Cell.EMPTY_CODEPOINT);

            // Stop if pending is exhausted and nothing meaningful was displaced.
            if (insertCol < width && !overflowHasContent) {
                break;
            }

            // Cascade to the next row (pending still has items, or overflow has content).
            insertCol = 0;
            insertRow++;
            if (insertRow >= height) {
                if (overflowHasContent) scroll();
                insertRow = height - 1;
                break;
            }
            // Prepend overflow so displaced cells lead on the next line.
            Cell[] overflowArr = overflow.toArray(new Cell[0]);
            for (int j = overflowArr.length - 1; j >= 0; j--) {
                pending.addFirst(overflowArr[j]);
            }
        }

        cursorRow = insertRow;
        cursorCol = Math.min(insertCol, width - 1);
    }

    /**
     * Fills the cursor's current row with {@code ch} using the current attributes.
     * Pass {@code null} to clear the line with default attributes.
     */
    public void fillLine(Character ch) {
        if (ch == null) {
            screen.get(cursorRow).clear();
        } else {
            screen.get(cursorRow).fill(ch, currentAttributes);
        }
    }

    /**
     * Pushes an empty line onto the bottom of the screen.
     * The top screen line scrolls into scrollback.
     */
    public void insertEmptyLineAtBottom() {
        scroll();
    }

    /** Clears the screen (all cells reset). Scrollback is preserved. Cursor moves to (0, 0). */
    public void clearScreen() {
        for (Line line : screen) {
            line.clear();
        }
        cursorRow = 0;
        cursorCol = 0;
    }

    /** Clears the screen and the entire scrollback. Cursor moves to (0, 0). */
    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }

    /**
     * Returns the character at the given global row and column.
     * For supplementary-plane codepoints use {@link #getCodepointAt} instead.
     *
     * @throws IndexOutOfBoundsException if the position is outside the buffer
     */
    public char getCharAt(int globalRow, int col) {
        return resolveGlobalRow(globalRow).getCell(col).getCharacter();
    }

    /**
     * Returns the Unicode codepoint at the given global row and column.
     *
     * @throws IndexOutOfBoundsException if the position is outside the buffer
     */
    public int getCodepointAt(int globalRow, int col) {
        return resolveGlobalRow(globalRow).getCell(col).getCodepoint();
    }

    /**
     * Returns the attributes of the cell at the given global position.
     *
     * @throws IndexOutOfBoundsException if the position is outside the buffer
     */
    public TextAttributes getAttributesAt(int globalRow, int col) {
        return resolveGlobalRow(globalRow).getCell(col).getAttributes();
    }

    /**
     * Returns the content of a single line as a plain string.
     * Wide-character continuation cells are skipped.
     *
     * @throws IndexOutOfBoundsException if {@code globalRow} is out of range
     */
    public String getLineAsString(int globalRow) {
        return resolveGlobalRow(globalRow).toContentString();
    }

    /** Returns the entire screen content as a newline-separated string. */
    public String getScreenAsString() {
        StringBuilder sb = new StringBuilder(height * (width + 1));
        for (int i = 0; i < height; i++) {
            sb.append(screen.get(i).toContentString());
            if (i < height - 1) sb.append('\n');
        }
        return sb.toString();
    }

    /** Returns scrollback + screen content as a newline-separated string. */
    public String getAllContentAsString() {
        int totalLines = scrollback.size() + height;
        StringBuilder sb = new StringBuilder(totalLines * (width + 1));

        boolean first = true;
        for (Line line : scrollback) {
            if (!first) sb.append('\n');
            sb.append(line.toContentString());
            first = false;
        }
        for (int i = 0; i < height; i++) {
            if (!first) sb.append('\n');
            sb.append(screen.get(i).toContentString());
            first = false;
        }
        return sb.toString();
    }

    public int getWidth()          { return width; }
    public int getHeight()         { return height; }
    public int getScrollbackSize() { return scrollback.size(); }

    private void scroll() {
        Line evicted = screen.remove(0);

        if (scrollbackMaxSize > 0) {
            scrollback.addLast(new Line(evicted));
            if (scrollback.size() > scrollbackMaxSize) {
                scrollback.removeFirst();
            }
        }

        evicted.clear();
        screen.add(evicted);
    }

    private Line resolveGlobalRow(int globalRow) {
        int sbSize    = scrollback.size();
        int totalRows = sbSize + height;

        if (globalRow < 0 || globalRow >= totalRows) {
            throw new IndexOutOfBoundsException(
                    "Global row " + globalRow + " is out of range [0, " + totalRows + ")");
        }

        if (globalRow < sbSize) {
            int idx = 0;
            for (Line line : scrollback) {
                if (idx == globalRow) return line;
                idx++;
            }
        }

        return screen.get(globalRow - sbSize);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}