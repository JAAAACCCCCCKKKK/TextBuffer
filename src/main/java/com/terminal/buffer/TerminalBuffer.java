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
    private TextAttributes currentAttributes;

    /**
     * Creates a new buffer with the given dimensions.
     *
     * @param initialWidth      number of columns (must be positive)
     * @param initialHeight     number of rows visible on screen (must be positive)
     * @param scrollbackMaxSize maximum number of scrollback lines retained (0 = disabled)
     */
    public TerminalBuffer(int initialWidth, int initialHeight, int scrollbackMaxSize) {
        if (initialWidth <= 0)  throw new IllegalArgumentException("Width must be positive, got: " + initialWidth);
        if (initialHeight <= 0) throw new IllegalArgumentException("Height must be positive, got: " + initialHeight);
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

    /**
     * Moves the cursor to the given screen position.
     * Out-of-bounds values are clamped to the nearest valid position.
     */
    public void setCursorPosition(int col, int row) {
        cursorRow = clamp(row, height - 1);
        cursorCol = clamp(col, width - 1);
    }

    public void moveCursorUp(int n) {
        cursorRow = clamp(cursorRow - n, height - 1);
    }

    public void moveCursorDown(int n) {
        cursorRow = clamp(cursorRow + n, height - 1);
    }

    public void moveCursorLeft(int n) {
        cursorCol = clamp(cursorCol - n, width - 1);
    }

    public void moveCursorRight(int n) {
        cursorCol = clamp(cursorCol + n, width - 1);
    }

    /**
     * Writes {@code text} starting at the current cursor position, overwriting
     * existing content. Wraps to subsequent lines when the end of a line is
     * reached. Scrolls the screen upward if writing past the last line.
     */
    public void writeText(String text) {
        if (text == null || text.isEmpty()) return;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            screen.get(cursorRow).setCell(cursorCol, ch, currentAttributes);
            cursorCol++;

            if (cursorCol >= width) {
                // reached end of line — wrap to next line
                cursorCol = 0;
                cursorRow++;

                if (cursorRow >= height) {
                    // past bottom — scroll up and stay on last line
                    scroll();
                    cursorRow = height - 1;
                }
            }
        }
    }

    /**
     * Inserts {@code text} at the current cursor position, shifting existing
     * content to the right. Overflow cascades to subsequent lines.
     * Scrolls the screen when the bottom line overflows.
     */
    public void insertText(String text) {
        if (text == null || text.isEmpty()) return;

        Deque<Cell> pending = new ArrayDeque<>();
        for (char ch : text.toCharArray()) {
            pending.addLast(new Cell(ch, currentAttributes));
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
                overflow.addLast(new Cell(src.getCharacter(), src.getAttributes()));
            }

            // Shift existing content right by toWrite positions to make room.
            for (int col = width - 1; col >= insertCol + toWrite; col--) {
                Cell src = line.getCell(col - toWrite);
                line.setCell(col, src.getCharacter(), src.getAttributes());
            }

            // Write pending characters into the freed slots.
            for (int col = insertCol; col < insertCol + toWrite; col++) {
                Cell c = pending.pollFirst();
                line.setCell(col, c.getCharacter(), c.getAttributes());
            }

            insertCol += toWrite;

            if (insertCol >= width) {
                // Prepend overflow to pending so displaced cells lead on the next line.
                Cell[] overflowArr = overflow.toArray(new Cell[0]);
                for (int i = overflowArr.length - 1; i >= 0; i--) {
                    pending.addFirst(overflowArr[i]);
                }

                insertCol = 0;
                insertRow++;
                if (insertRow >= height) {
                    scroll();
                    insertRow = height - 1;
                }
            } else {
                break; // everything fit on this line
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
     * Returns {@code ' '} (space) for empty cells.
     *
     * @throws IndexOutOfBoundsException if the position is outside the buffer
     */
    public char getCharAt(int globalRow, int col) {
        return resolveGlobalRow(globalRow).getCell(col).getCharacter();
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
     * Returns the content of a single line as a plain string (trailing spaces included).
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

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public int getScrollbackSize() { return scrollback.size(); }

    /**
     * Scrolls the screen up by one line:
     * moves screen[0] into scrollback and appends a fresh empty line at the bottom.
     */
    private void scroll() {
        Line evicted = screen.remove(0);

        if (scrollbackMaxSize > 0) {
            // Deep-copy before storing so future screen edits don't corrupt history.
            scrollback.addLast(new Line(evicted));
            if (scrollback.size() > scrollbackMaxSize) {
                scrollback.removeFirst();
            }
        }
        // Reuse the evicted line object (cleared) as the new bottom line.
        evicted.clear();
        screen.add(evicted);
    }

    /**
     * Maps a global row index to the corresponding {@link Line}.
     * Global rows 0...scrollback.size()-1 address scrollback (oldest first);
     * the remainder address the screen.
     *
     * @throws IndexOutOfBoundsException if globalRow is out of range
     */
    private Line resolveGlobalRow(int globalRow) {
        int sbSize = scrollback.size();
        int totalRows = sbSize + height;

        if (globalRow < 0 || globalRow >= totalRows) {
            throw new IndexOutOfBoundsException(
                    "Global row " + globalRow + " is out of range [0, " + totalRows + ")");
        }

        if (globalRow < sbSize) {
            // Walk the deque — ArrayDeque does not support random access.
            int idx = 0;
            for (Line line : scrollback) {
                if (idx == globalRow) return line;
                idx++;
            }
        }

        return screen.get(globalRow - sbSize);
    }

    private void checkScreenRowBounds(int screenRow) {
        if (screenRow < 0 || screenRow >= height) {
            throw new IndexOutOfBoundsException(
                    "Screen row " + screenRow + " is out of range [0, " + height + ")");
        }
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }
}
