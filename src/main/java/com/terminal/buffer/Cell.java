package com.terminal.buffer;

/**
 * Represents a single character cell in the terminal grid.
 *
 * <p>Wide characters (CJK ideographs, emoji) occupy 2 columns. This is modelled
 * using two adjacent cells:
 * <ul>
 *   <li>{@link CellType#WIDE_LEAD} — the left cell; holds the actual codepoint.</li>
 *   <li>{@link CellType#WIDE_CONT} — the right placeholder; its codepoint is
 *       {@code EMPTY_CODEPOINT} and it must never be written to directly.</li>
 * </ul>
 * Normal (narrow) cells use {@link CellType#NORMAL}.
 *
 * <p>Codepoints are stored as {@code int} so characters outside the Basic
 * Multilingual Plane (emoji, etc.) are represented correctly.
 */
public class Cell {

    public static final int  EMPTY_CODEPOINT = ' ';

    /** Describes the role of this cell in the grid. */
    public enum CellType { NORMAL, WIDE_LEAD, WIDE_CONT }

    private int            codepoint;
    private TextAttributes attributes;
    private CellType       type;

    public Cell(int codepoint, TextAttributes attributes, CellType type) {
        this.codepoint  = codepoint;
        this.attributes = attributes;
        this.type       = type;
    }

    /** Convenience constructor for narrow cells. */
    public Cell(int codepoint, TextAttributes attributes) {
        this(codepoint, attributes, CellType.NORMAL);
    }

    /** Returns a blank narrow cell: space codepoint with default attributes. */
    public static Cell empty() {
        return new Cell(EMPTY_CODEPOINT, TextAttributes.DEFAULT, CellType.NORMAL);
    }

    public int getCodepoint() { return codepoint; }

    /**
     * Returns the cell content as a {@code char}.
     * For supplementary-plane codepoints (> 0xFFFF) this returns {@code '?'}
     * as a safe fallback; use {@link #getCodepoint()} for full fidelity.
     */
    public char getCharacter() {
        return Character.isBmpCodePoint(codepoint) ? (char) codepoint : '?';
    }

    public TextAttributes getAttributes() { return attributes; }
    public CellType       getType()       { return type; }

    public boolean isWideLead() { return type == CellType.WIDE_LEAD; }
    public boolean isWideCont() { return type == CellType.WIDE_CONT; }

    public void setCodepoint(int codepoint)           { this.codepoint  = codepoint;  }
    public void setAttributes(TextAttributes attrs)   { this.attributes = attrs;      }
    public void setType(CellType type)                { this.type       = type;       }

    /** Sets codepoint, attributes, and type in one call. */
    public void set(int codepoint, TextAttributes attributes, CellType type) {
        this.codepoint  = codepoint;
        this.attributes = attributes;
        this.type       = type;
    }

    /** Convenience for narrow cells. */
    public void set(int codepoint, TextAttributes attributes) {
        set(codepoint, attributes, CellType.NORMAL);
    }

    /** Resets this cell to empty narrow in-place. */
    public void clear() {
        this.codepoint  = EMPTY_CODEPOINT;
        this.attributes = TextAttributes.DEFAULT;
        this.type       = CellType.NORMAL;
    }

    @Override
    public String toString() {
        return "Cell{cp=" + codepoint + ", type=" + type + ", attributes=" + attributes + "}";
    }
}