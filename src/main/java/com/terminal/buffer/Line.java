package com.terminal.buffer;

/**
 * Represents a single row in the terminal grid.
 *
 * <p>A line holds exactly {@code width} {@link Cell} objects. Wide characters
 * occupy two adjacent cells: a {@link Cell.CellType#WIDE_LEAD} cell followed by
 * a {@link Cell.CellType#WIDE_CONT} placeholder. Callers use
 * {@link #setNarrowCell} for normal characters and {@link #setWideCell} for
 * wide characters; the line keeps the two cells consistent automatically.
 */
public class Line {

    private final int    width;
    private final Cell[] cells;

    /** Creates a line of {@code width} empty narrow cells. */
    public Line(int width) {
        if (width <= 0) throw new IllegalArgumentException("Width must be positive, got: " + width);
        this.width = width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            cells[i] = Cell.empty();
        }
    }

    /** Deep-copy constructor — used when moving a line from screen to scrollback. */
    public Line(Line other) {
        this.width = other.width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            Cell src = other.cells[i];
            this.cells[i] = new Cell(src.getCodepoint(), src.getAttributes(), src.getType());
        }
    }

    /** Returns the cell at {@code col}. */
    public Cell getCell(int col) {
        checkBounds(col);
        return cells[col];
    }

    /**
     * Writes a narrow (1-column) codepoint at {@code col}.
     * If this col previously held the lead or continuation of a wide char,
     * the partner cell is cleared to avoid a dangling half-char.
     */
    public void setNarrowCell(int col, int codepoint, TextAttributes attributes) {
        checkBounds(col);
        clearWidePartner(col);
        cells[col].set(codepoint, attributes, Cell.CellType.NORMAL);
    }

    /**
     * Writes a wide (2-column) character starting at {@code col}.
     *
     * <p>If there is no room for the continuation cell (i.e. {@code col == width-1}),
     * a space is written instead — matching the behaviour of most real terminals.
     *
     * @throws IndexOutOfBoundsException if col is outside [0, width)
     */
    public void setWideCell(int col, int codepoint, TextAttributes attributes) {
        checkBounds(col);
        clearWidePartner(col);

        if (col + 1 >= width) {
            // No room for continuation — write a space at this column instead.
            cells[col].set(Cell.EMPTY_CODEPOINT, attributes, Cell.CellType.NORMAL);
            return;
        }

        // Clear any wide partner that the continuation column might belong to.
        clearWidePartner(col + 1);

        cells[col].set(codepoint, attributes, Cell.CellType.WIDE_LEAD);
        cells[col + 1].set(Cell.EMPTY_CODEPOINT, attributes, Cell.CellType.WIDE_CONT);
    }

    /** Convenience overload accepting a {@code char} (narrow only). */
    public void setCell(int col, char character, TextAttributes attributes) {
        setNarrowCell(col, character, attributes);
    }

    /** Fills every cell with {@code codepoint} and {@code attributes} (narrow). */
    public void fill(int codepoint, TextAttributes attributes) {
        for (Cell cell : cells) {
            cell.set(codepoint, attributes, Cell.CellType.NORMAL);
        }
    }

    /** Fills every cell with {@code character} and {@code attributes} (narrow). */
    public void fill(char character, TextAttributes attributes) {
        fill((int) character, attributes);
    }

    /** Resets every cell to empty. */
    public void clear() {
        for (Cell cell : cells) {
            cell.clear();
        }
    }

    /**
     * Returns the line as a string.
     * {@link Cell.CellType#WIDE_LEAD} cells contribute their codepoint;
     * {@link Cell.CellType#WIDE_CONT} cells are skipped (the lead already
     * added the character); narrow cells contribute their codepoint.
     *
     * <p>The resulting string length in Unicode code-units may therefore be
     * less than {@code width} when wide characters are present.
     */
    public String toContentString() {
        StringBuilder sb = new StringBuilder(width);
        for (Cell cell : cells) {
            if (cell.isWideCont()) continue;          // skip — lead already added it
            sb.appendCodePoint(cell.getCodepoint());
        }
        return sb.toString();
    }

    public int getWidth() { return width; }

    /**
     * If the cell at {@code col} is part of a wide character pair, clears the
     * partner cell so no dangling lead or continuation remains.
     */
    private void clearWidePartner(int col) {
        Cell cell = cells[col];
        if (cell.isWideLead() && col + 1 < width) {
            cells[col + 1].clear();
        } else if (cell.isWideCont() && col - 1 >= 0) {
            cells[col - 1].clear();
        }
    }

    private void checkBounds(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " is out of bounds for line width " + width);
        }
    }
}