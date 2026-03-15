package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    @Test
    void emptyCell_hasSpaceCodepointAndDefaultAttributes() {
        Cell cell = Cell.empty();
        assertEquals(Cell.EMPTY_CODEPOINT, cell.getCodepoint());
        assertEquals(' ',                  cell.getCharacter());
        assertEquals(TextAttributes.DEFAULT, cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL,   cell.getType());
    }

    @Test
    void constructor_storesCodepointAndAttributes() {
        TextAttributes attrs = TextAttributes.builder().bold(true).build();
        Cell cell = new Cell('A', attrs);
        assertEquals('A',              cell.getCharacter());
        assertEquals((int) 'A',        cell.getCodepoint());
        assertEquals(attrs,            cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void constructor_withExplicitCellType_storesType() {
        Cell lead = new Cell('中', TextAttributes.DEFAULT, Cell.CellType.WIDE_LEAD);
        Cell cont = new Cell(' ',  TextAttributes.DEFAULT, Cell.CellType.WIDE_CONT);
        assertTrue(lead.isWideLead());
        assertTrue(cont.isWideCont());
        assertFalse(lead.isWideCont());
        assertFalse(cont.isWideLead());
    }

    @Test
    void set_updatesAllFields() {
        Cell cell = Cell.empty();
        TextAttributes attrs = TextAttributes.builder().italic(true).build();
        cell.set('Z', attrs, Cell.CellType.NORMAL);
        assertEquals('Z',   cell.getCharacter());
        assertEquals(attrs, cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL, cell.getType());
    }

    @Test
    void clear_resetsToEmpty() {
        TextAttributes attrs = TextAttributes.builder().bold(true).build();
        Cell cell = new Cell('X', attrs, Cell.CellType.WIDE_LEAD);
        cell.clear();
        assertEquals(Cell.EMPTY_CODEPOINT,   cell.getCodepoint());
        assertEquals(TextAttributes.DEFAULT,  cell.getAttributes());
        assertEquals(Cell.CellType.NORMAL,    cell.getType());
    }

    @Test
    void getCharacter_bmpCodepoint_returnsChar() {
        Cell cell = new Cell('€', TextAttributes.DEFAULT);
        assertEquals('€', cell.getCharacter());
    }

    @Test
    void getCharacter_supplementaryPlaneCodepoint_returnsFallback() {
        int emoji = 0x1F600; // 😀 — outside BMP
        Cell cell = new Cell(emoji, TextAttributes.DEFAULT);
        assertEquals(emoji, cell.getCodepoint());
        assertEquals('?',   cell.getCharacter()); // safe BMP fallback
    }
}
