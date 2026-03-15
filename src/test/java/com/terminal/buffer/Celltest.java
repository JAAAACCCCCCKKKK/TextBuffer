package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    @Test
    void emptyCell_hasSpaceCodepointAndDefaultAttributes() {
        // A freshly created empty cell represents a blank terminal column:
        // space codepoint, default styling, and NORMAL (non-wide) type.
        Cell cell = Cell.empty();
        assertEquals(Cell.EMPTY_CODEPOINT, cell.getCodepoint());
        assertEquals(' ',                  cell.getCharacter());
        assertEquals(TextAttributes.DEFAULT, cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL,   cell.getType());
    }

    @Test
    void constructor_storesCodepointAndAttributes() {
        // The two-arg constructor creates a narrow cell; type defaults to NORMAL.
        TextAttributes attrs = TextAttributes.builder().bold(true).build();
        Cell cell = new Cell('A', attrs);
        assertEquals('A',              cell.getCharacter());
        assertEquals((int) 'A',        cell.getCodepoint());
        assertEquals(attrs,            cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void constructor_withExplicitCellType_storesType() {
        // Wide characters occupy two cells: a WIDE_LEAD holding the codepoint
        // and a WIDE_CONT placeholder to its right. Each cell reports its own role.
        Cell lead = new Cell('中', TextAttributes.DEFAULT, Cell.CellType.WIDE_LEAD);
        Cell cont = new Cell(' ',  TextAttributes.DEFAULT, Cell.CellType.WIDE_CONT);
        assertTrue(lead.isWideLead());
        assertTrue(cont.isWideCont());
        assertFalse(lead.isWideCont());
        assertFalse(cont.isWideLead());
    }

    @Test
    void set_updatesAllFields() {
        // set() mutates codepoint, attributes and type in one call,
        // replacing all prior state of the cell.
        Cell cell = Cell.empty();
        TextAttributes attrs = TextAttributes.builder().italic(true).build();
        cell.set('Z', attrs, Cell.CellType.NORMAL);
        assertEquals('Z',   cell.getCharacter());
        assertEquals(attrs, cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void clear_resetsToEmpty() {
        // clear() returns any cell to the blank state regardless of its prior content
        // or type, equivalent to overwriting it with Cell.empty().
        TextAttributes attrs = TextAttributes.builder().bold(true).build();
        Cell cell = new Cell('X', attrs, Cell.CellType.WIDE_LEAD);
        cell.clear();
        assertEquals(Cell.EMPTY_CODEPOINT,   cell.getCodepoint());
        assertEquals(TextAttributes.DEFAULT,  cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL,    cell.getType());
    }

    @Test
    void getCharacter_bmpCodepoint_returnsChar() {
        // BMP codepoints (U+0000–U+FFFF) fit in a Java char and are returned directly.
        Cell cell = new Cell('€', TextAttributes.DEFAULT);
        assertEquals('€', cell.getCharacter());
    }

    @Test
    void getCharacter_supplementaryPlaneCodepoint_returnsFallback() {
        // Supplementary-plane codepoints cannot be represented as a single char;
        // getCharacter() returns '?' as a safe stand-in while getCodepoint()
        // always preserves the full Unicode value.
        int emoji = 0x1F600;
        Cell cell = new Cell(emoji, TextAttributes.DEFAULT);
        assertEquals(emoji, cell.getCodepoint());
        assertEquals('?',   cell.getCharacter());
    }

    @Test
    void setCodepoint_updatesCodepointOnly() {
        // setCodepoint mutates the stored codepoint in place while leaving
        // attributes and type unchanged.
        Cell cell = new Cell('A', TextAttributes.DEFAULT, Cell.CellType.WIDE_LEAD);
        cell.setCodepoint('B');
        assertEquals('B',                  cell.getCharacter());
        assertEquals(TextAttributes.DEFAULT, cell.getAttributes());
        assertEquals(Cell.CellType.WIDE_LEAD, cell.getType());
    }

    @Test
    void setAttributes_updatesAttributesOnly() {
        // setAttributes mutates the stored attributes in place while leaving
        // codepoint and type unchanged.
        TextAttributes bold = TextAttributes.builder().bold(true).build();
        Cell cell = new Cell('X', TextAttributes.DEFAULT);
        cell.setAttributes(bold);
        assertEquals('X',  cell.getCharacter());
        assertTrue(cell.getAttributes().bold());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void setType_updatesTypeOnly() {
        // setType mutates the stored cell type in place while leaving
        // codepoint and attributes unchanged.
        Cell cell = new Cell('Z', TextAttributes.DEFAULT, Cell.CellType.NORMAL);
        cell.setType(Cell.CellType.WIDE_CONT);
        assertEquals('Z',                  cell.getCharacter());
        assertEquals(TextAttributes.DEFAULT, cell.getAttributes());
        assertTrue(cell.isWideCont());
    }

    @Test
    void set_twoArg_setsCodepointAndAttributesWithNormalType() {
        // The two-argument set() convenience overload must set codepoint and
        // attributes while defaulting the cell type to NORMAL.
        TextAttributes italic = TextAttributes.builder().italic(true).build();
        Cell cell = new Cell('A', TextAttributes.DEFAULT, Cell.CellType.WIDE_LEAD);
        cell.set('Q', italic);
        assertEquals('Q',              cell.getCharacter());
        assertTrue(cell.getAttributes().italic());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void toString_containsCodepointAndType() {
        // toString() is used for debugging; it must include at least the codepoint
        // value and the cell type so that log output is meaningful.
        Cell cell = new Cell('A', TextAttributes.DEFAULT, Cell.CellType.NORMAL);
        String s = cell.toString();
        assertTrue(s.contains(String.valueOf((int) 'A')));
        assertTrue(s.contains("NORMAL"));
    }
}
