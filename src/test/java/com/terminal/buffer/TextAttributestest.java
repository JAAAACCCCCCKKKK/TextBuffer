package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextAttributesTest {

    @Test
    void defaultAttributes_hasNoColorAndNoStyles() {
        TextAttributes attrs = TextAttributes.DEFAULT;
        assertEquals(TerminalColor.DEFAULT, attrs.foreground());
        assertEquals(TerminalColor.DEFAULT, attrs.background());
        assertFalse(attrs.bold());
        assertFalse(attrs.italic());
        assertFalse(attrs.underline());
    }

    @Test
    void constructor_storesAllFields() {
        TextAttributes attrs = new TextAttributes(
                TerminalColor.RED, TerminalColor.BLUE, true, true, true);
        assertEquals(TerminalColor.RED,  attrs.foreground());
        assertEquals(TerminalColor.BLUE, attrs.background());
        assertTrue(attrs.bold());
        assertTrue(attrs.italic());
        assertTrue(attrs.underline());
    }

    @Test
    void constructor_rejectsNullForeground() {
        assertThrows(NullPointerException.class, () ->
                new TextAttributes(null, TerminalColor.DEFAULT, false, false, false));
    }

    @Test
    void constructor_rejectsNullBackground() {
        assertThrows(NullPointerException.class, () ->
                new TextAttributes(TerminalColor.DEFAULT, null, false, false, false));
    }

    @Test
    void equalityByValue_sameFields_areEqual() {
        TextAttributes a = new TextAttributes(TerminalColor.RED, TerminalColor.BLUE, true, false, true);
        TextAttributes b = new TextAttributes(TerminalColor.RED, TerminalColor.BLUE, true, false, true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalityByValue_differentFields_areNotEqual() {
        TextAttributes a = new TextAttributes(TerminalColor.RED,   TerminalColor.DEFAULT, false, false, false);
        TextAttributes b = new TextAttributes(TerminalColor.GREEN, TerminalColor.DEFAULT, false, false, false);
        assertNotEquals(a, b);
    }

    @Test
    void builder_defaultValues_matchDefaultConstant() {
        TextAttributes built = TextAttributes.builder().build();
        assertEquals(TextAttributes.DEFAULT, built);
    }

    @Test
    void builder_setsEachFieldIndependently() {
        TextAttributes attrs = TextAttributes.builder()
                .foreground(TerminalColor.CYAN)
                .bold(true)
                .build();
        assertEquals(TerminalColor.CYAN,    attrs.foreground());
        assertEquals(TerminalColor.DEFAULT, attrs.background());
        assertTrue(attrs.bold());
        assertFalse(attrs.italic());
        assertFalse(attrs.underline());
    }

    @Test
    void builderFromBase_copiesAllFields() {
        TextAttributes base = new TextAttributes(
                TerminalColor.RED, TerminalColor.BLUE, true, true, true);
        TextAttributes copy = TextAttributes.builder(base).build();
        assertEquals(base, copy);
    }

    @Test
    void builderFromBase_overridesIndividualField() {
        TextAttributes base = new TextAttributes(
                TerminalColor.RED, TerminalColor.BLUE, true, false, false);
        TextAttributes modified = TextAttributes.builder(base)
                .foreground(TerminalColor.GREEN)
                .build();
        assertEquals(TerminalColor.GREEN, modified.foreground());
        assertEquals(TerminalColor.BLUE,  modified.background());
        assertTrue(modified.bold());
    }

    @Test
    void immutability_changingBuilderAfterBuild_doesNotAffectBuiltInstance() {
        TextAttributes.Builder builder = TextAttributes.builder().foreground(TerminalColor.RED);
        TextAttributes first = builder.build();
        builder.foreground(TerminalColor.GREEN);
        TextAttributes second = builder.build();
        assertEquals(TerminalColor.RED,   first.foreground());
        assertEquals(TerminalColor.GREEN, second.foreground());
    }
}