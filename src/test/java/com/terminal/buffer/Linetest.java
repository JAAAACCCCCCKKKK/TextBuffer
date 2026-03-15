package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineTest {

    @Test
    void newLine_allCellsAreEmpty() {
        Line line = new Line(5);
        for (int col = 0; col < 5; col++) {
            assertEquals(Cell.EMPTY_CODEPOINT, line.getCell(col).getCodepoint());
            assertEquals(Cell.CellType.NORMAL,  line.getCell(col).getType());
        }
    }

    @Test
    void constructor_rejectsNonPositiveWidth() {
        assertThrows(IllegalArgumentException.class, () -> new Line(0));
        assertThrows(IllegalArgumentException.class, () -> new Line(-1));
    }

    @Test
    void setNarrowCell_storesCharacterAndAttributes() {
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
        Line line = new Line(6);
        line.setWideCell(2, '中', TextAttributes.DEFAULT);
        assertEquals(Cell.CellType.WIDE_LEAD, line.getCell(2).getType());
        assertEquals('中',                    line.getCell(2).getCharacter());
        assertEquals(Cell.CellType.WIDE_CONT, line.getCell(3).getType());
    }

    @Test
    void setWideCell_atLastColumn_writesSpaceInstead() {
        Line line = new Line(5);
        line.setWideCell(4, '中', TextAttributes.DEFAULT);
        // No room for continuation — should degrade to a space
        assertEquals(Cell.CellType.NORMAL,    line.getCell(4).getType());
        assertEquals(Cell.EMPTY_CODEPOINT,    line.getCell(4).getCodepoint());
    }

    @Test
    void setNarrowCell_overwritingWideLead_clearsContCell() {
        Line line = new Line(6);
        line.setWideCell(2, '中', TextAttributes.DEFAULT);
        line.setNarrowCell(2, 'A', TextAttributes.DEFAULT);
        assertEquals(Cell.CellType.NORMAL, line.getCell(2).getType());
        assertEquals(Cell.CellType.NORMAL, line.getCell(3).getType()); // cont cleared
        assertEquals(Cell.EMPTY_CODEPOINT, line.getCell(3).getCodepoint());
    }

    @Test
    void setNarrowCell_overwritingWideCont_clearsLeadCell() {
        Line line = new Line(6);
        line.setWideCell(2, '中', TextAttributes.DEFAULT);
        line.setNarrowCell(3, 'B', TextAttributes.DEFAULT); // overwrite cont
        assertEquals(Cell.CellType.NORMAL, line.getCell(3).getType());
        assertEquals(Cell.CellType.NORMAL, line.getCell(2).getType()); // lead cleared
        assertEquals(Cell.EMPTY_CODEPOINT, line.getCell(2).getCodepoint());
    }

    @Test
    void fill_setsAllCellsToGivenChar() {
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
        Line line = new Line(5);
        line.setNarrowCell(0, 'h', TextAttributes.DEFAULT);
        line.setNarrowCell(1, 'i', TextAttributes.DEFAULT);
        String s = line.toContentString();
        assertEquals(5, s.length());
        assertEquals("hi   ", s);
    }

    @Test
    void toContentString_wideChar_skipsContinuationCell() {
        Line line = new Line(6);
        line.setWideCell(0, '中', TextAttributes.DEFAULT);
        String s = line.toContentString();
        // '中' = 1 char in Java, then 4 spaces (cont cell skipped)
        assertEquals("中    ", s);
        assertEquals(5, s.length()); // 1 wide lead + 4 spaces (cont skipped)
    }

    @Test
    void deepCopy_isIndependent() {
        Line original = new Line(4);
        original.setNarrowCell(0, 'A', TextAttributes.DEFAULT);
        Line copy = new Line(original);
        copy.setNarrowCell(0, 'Z', TextAttributes.DEFAULT);
        assertEquals('A', original.getCell(0).getCharacter()); // original unaffected
        assertEquals('Z', copy.getCell(0).getCharacter());
    }

    @Test
    void getCell_outOfBounds_throwsIndexOutOfBoundsException() {
        Line line = new Line(5);
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> line.getCell(5));
    }
}