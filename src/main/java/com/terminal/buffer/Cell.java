package com.terminal.buffer;

/**
 * Represents a single character cell in the terminal grid.
 * Each cell holds a character and its display attributes.
 * <p>
 * An "empty" cell is represented as a space character with default attributes,
 * matching how real terminals treat blank grid positions.
 */
public class Cell {

    public static final char EMPTY_CHAR = ' ';

    private char character;
    private TextAttributes attributes;

    public Cell(char character, TextAttributes attributes) {
        this.character = character;
        this.attributes = attributes;
    }

    /** Returns a blank cell: space character with default attributes. */
    public static Cell empty() {
        return new Cell(EMPTY_CHAR, TextAttributes.DEFAULT);
    }

    public char getCharacter() {
        return character;
    }

    public void setCharacter(char character) {
        this.character = character;
    }

    public TextAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(TextAttributes attributes) {
        this.attributes = attributes;
    }

    public void set(char character, TextAttributes attributes) {
        this.character = character;
        this.attributes = attributes;
    }

    /** Resets this cell to empty in-place (avoids allocation). */
    public void clear() {
        this.character = EMPTY_CHAR;
        this.attributes = TextAttributes.DEFAULT;
    }

    @Override
    public String toString() {
        return "Cell{char='" + character + "', attributes=" + attributes + "}";
    }
}