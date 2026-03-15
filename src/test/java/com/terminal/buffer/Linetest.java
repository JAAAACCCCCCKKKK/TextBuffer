package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineTest {

    @Test
    void newLine_allCellsAreEmpty() {
        // A freshly constructed Line has every cell set to the empty codepoint
        // with NORMAL type, representing blank terminal columns with no content.
        Line line = new Line(5);
        for (int col = 0; col < 5; col++) {
            assertEquals(Cell.EMPTY_CODEPOINT, line.getCell(col).getCodepoint());
            assertEquals(Cell.CellType.NORMAL,  line.getCell(col).getType());
        }
    }

    @Test
    void constructor_rejectsNonPositiveWidth() {
        // A line width of zero or negative has no valid meaning in a terminal;
        // the constructor must reject such values with an IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> new Line(0));
        assertThrows(IllegalArgumentException.class, () -> new Line(-1));
    }

    @Test
    void setNarrowCell_storesCharacterAndAttributes() {
        // setNarrowCell writes a single-column character with the given attributes
        // at the specified column and marks the cell type as NORMAL.
        Line line = new Line(5);
        TextAttributes attrs = TextAttributes.builder().bold(true).build();
        line.setNarrowCell(2, 'X', attrs);
        Cell cell = line.getCell(2);
        assertEquals('X',  cell.getCharacter());
        assertEquals(attrs, cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void setWideCell_setsLeadAndContCells() {
        // A wide character occupies two adjacent columns: the first cell is
        // WIDE_LEAD and holds the codepoint, the second is WIDE_CONT and acts
        // as a placeholder so that column-counting remains consistent.
        Line line = new Line(6);
        line.setWideCell(2, '中', TextAttributes.DEFAULT);
        assertEquals(Cell.CellType.WIDE_LEAD, line.getCell(2).getType());
        assertEquals('中',                    line.getCell(2).getCharacter());
        assertEquals(Cell.CellType.WIDE_CONT, line.getCell(3).getType());
    }

    @Test
    void setWideCell_atLastColumn_writesSpaceInstead() {
        // When a wide character is requested at the last column there is no room
        // for the continuation cell, so the implementation degrades gracefully by
        // writing a space (EMPTY_CODEPOINT, NORMAL type) in that column.
        Line line = new Line(5);
        line.setWideCell(4, '中', TextAttributes.DEFAULT);
        assertEquals(Cell.CellType.NORMAL,    line.getCell(4).getType());
        assertEquals(Cell.EMPTY_CODEPOINT,    line.getCell(4).getCodepoint());
    }

    @Test
    void setNarrowCell_overwritingWideLead_clearsContCell() {
        // Writing a narrow character over a WIDE_LEAD would leave an orphaned
        // WIDE_CONT to its right. The implementation must clear that continuation
        // cell to avoid a dangling placeholder with no matching lead.
        Line line = new Line(6);
        line.setWideCell(2, '中', TextAttributes.DEFAULT);
        line.setNarrowCell(2, 'A', TextAttributes.DEFAULT);
        assertEquals(Cell.CellType.NORMAL, line.getCell(2).getType());
        assertEquals(Cell.CellType.NORMAL, line.getCell(3).getType());
        assertEquals(Cell.EMPTY_CODEPOINT, line.getCell(3).getCodepoint());
    }

    @Test
    void setNarrowCell_overwritingWideCont_clearsLeadCell() {
        // Writing a narrow character over a WIDE_CONT would leave an orphaned
        // WIDE_LEAD to its left. The implementation must clear that lead cell
        // so no half-wide character remains visible in the line.
        Line line = new Line(6);
        line.setWideCell(2, '中', TextAttributes.DEFAULT);
        line.setNarrowCell(3, 'B', TextAttributes.DEFAULT);
        assertEquals(Cell.CellType.NORMAL, line.getCell(3).getType());
        assertEquals(Cell.CellType.NORMAL, line.getCell(2).getType());
        assertEquals(Cell.EMPTY_CODEPOINT, line.getCell(2).getCodepoint());
    }

    @Test
    void fill_setsAllCellsToGivenChar() {
        // fill() overwrites every cell in the line with the given character and
        // attributes, and sets all cell types to NORMAL (no wide characters remain).
        Line line = new Line(4);
        TextAttributes attrs = TextAttributes.builder().underline(true).build();
        line.fill('-', attrs);
        for (int col = 0; col < 4; col++) {
            assertEquals('-',  line.getCell(col).getCharacter());
            assertEquals(attrs, line.getCell(col).getAttributes());
            assertEquals(Cell.CellType.NORMAL, line.getCell(col).getType());
        }
    }

    @Test
    void clear_resetsAllCellsToEmpty() {
        // clear() resets every cell to the empty codepoint with default attributes,
        // erasing any content or styling that was previously written to the line.
        Line line = new Line(4);
        line.fill('X', TextAttributes.DEFAULT);
        line.clear();
        for (int col = 0; col < 4; col++) {
            assertEquals(Cell.EMPTY_CODEPOINT,  line.getCell(col).getCodepoint());
            assertEquals(TextAttributes.DEFAULT, line.getCell(col).getAttributes());
        }
    }

    @Test
    void toContentString_narrowChars_lengthEqualsWidth() {
        // toContentString() always returns a string whose length equals the line
        // width. Columns not written to are represented as spaces so the result
        // is always fully padded to the declared width.
        Line line = new Line(5);
        line.setNarrowCell(0, 'h', TextAttributes.DEFAULT);
        line.setNarrowCell(1, 'i', TextAttributes.DEFAULT);
        String s = line.toContentString();
        assertEquals(5, s.length());
        assertEquals("hi   ", s);
    }

    @Test
    void toContentString_wideChar_skipsContinuationCell() {
        // WIDE_CONT cells are placeholder slots and carry no printable character,
        // so toContentString() skips them. The result length is width-1 for a
        // line containing one wide character because the continuation is omitted.
        Line line = new Line(6);
        line.setWideCell(0, '中', TextAttributes.DEFAULT);
        String s = line.toContentString();
        assertEquals("中    ", s);
        assertEquals(5, s.length());
    }

    @Test
    void deepCopy_isIndependent() {
        // The copy constructor creates a deep copy: mutating the copy must not
        // affect the original, and mutating the original must not affect the copy.
        Line original = new Line(4);
        original.setNarrowCell(0, 'A', TextAttributes.DEFAULT);
        Line copy = new Line(original);
        copy.setNarrowCell(0, 'Z', TextAttributes.DEFAULT);
        assertEquals('A', original.getCell(0).getCharacter());
        assertEquals('Z', copy.getCell(0).getCharacter());
    }

    @Test
    void getCell_outOfBounds_throwsIndexOutOfBoundsException() {
        // Accessing a column index that is either negative or beyond the last
        // valid column must throw IndexOutOfBoundsException to prevent silent
        // memory corruption from out-of-range writes or reads.
        Line line = new Line(5);
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(5));
    }

    @Test
    void getWidth_returnsConstructedWidth() {
        // getWidth() must return the exact width value passed to the constructor
        // so that callers can safely iterate over columns without out-of-bounds access.
        Line line = new Line(7);
        assertEquals(7, line.getWidth());
    }

    @Test
    void setCell_charConvenience_storesNarrowChar() {
        // setCell(col, char, attrs) is a convenience overload for setNarrowCell.
        // It must store the character with NORMAL type and the given attributes.
        Line line = new Line(5);
        TextAttributes bold = TextAttributes.builder().bold(true).build();
        line.setCell(1, 'M', bold);
        assertEquals('M',              line.getCell(1).getCharacter());
        assertTrue(line.getCell(1).getAttributes().bold());
        assertEquals(Cell.CellType.NORMAL, line.getCell(1).getType());
    }
}
