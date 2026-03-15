package com.terminal.buffer;

/**
 * Represents a single row in the terminal grid.
 *
 * <p>A line holds exactly {@code width} {@link Cell} objects. The array is
 * fixed-length for the lifetime of the line, matching the terminal's column count.
 * All mutations happen in-place on existing {@code Cell} instances to avoid
 * unnecessary allocations during normal editing.
 */
public class Line {

    private final int width;
    private final Cell[] cells;

    /**
     * Creates a line of {@code width} empty cells.
     *
     * @throws IllegalArgumentException if width is not positive
     */
    public Line(int width) {
        if (width <= 0) throw new IllegalArgumentException("Width must be positive, got: " + width);
        this.width = width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            cells[i] = Cell.empty();
        }
    }

    /**
     * Copy constructor — produces an independent deep copy of {@code other}.
     * Used when moving a line from screen to scrollback so the two regions
     * never share mutable {@link Cell} instances.
     */
    public Line(Line other) {
        this.width = other.width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            Cell src = other.cells[i];
            this.cells[i] = new Cell(src.getCharacter(), src.getAttributes());
        }
    }

    /**
     * Returns the cell at column {@code col}.
     *
     * @throws IndexOutOfBoundsException if col is outside [0, width)
     */
    public Cell getCell(int col) {
        checkBounds(col);
        return cells[col];
    }

    /**
     * Overwrites the cell at column {@code col} in-place.
     *
     * @throws IndexOutOfBoundsException if col is outside [0, width)
     */
    public void setCell(int col, char character, TextAttributes attributes) {
        checkBounds(col);
        cells[col].set(character, attributes);
    }

    /**
     * Fills every cell in this line with {@code character} and {@code attributes}.
     */
    public void fill(char character, TextAttributes attributes) {
        for (Cell cell : cells) {
            cell.set(character, attributes);
        }
    }

    public void clear() {
        for (Cell cell : cells) {
            cell.clear();
        }
    }

    /**
     * Returns the line content as a plain string by concatenating each cell's
     * character. Trailing spaces are included so the string length always equals
     * {@code width}.
     */
    public String toContentString() {
        StringBuilder sb = new StringBuilder(width);
        for (Cell cell : cells) {
            sb.append(cell.getCharacter());
        }
        return sb.toString();
    }

    public int getWidth() {
        return width;
    }

    private void checkBounds(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " is out of bounds for line width " + width);
        }
    }
}
