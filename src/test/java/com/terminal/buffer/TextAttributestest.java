package com.terminal.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextAttributesTest {

    @Test
    void defaultAttributes_hasNoColorAndNoStyles() {
        // TextAttributes.DEFAULT represents unstyled text: both colors are the
        // terminal default and all style flags (bold, italic, underline) are off.
        TextAttributes attrs = TextAttributes.DEFAULT;
        assertEquals(TerminalColor.DEFAULT, attrs.foreground());
        assertEquals(TerminalColor.DEFAULT, attrs.background());
        assertFalse(attrs.bold());
        assertFalse(attrs.italic());
        assertFalse(attrs.underline());
    }

    @Test
    void constructor_storesAllFields() {
        // All five fields passed to the constructor are accessible through
        // the corresponding accessors without mutation.
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
        // A null foreground color is illegal; callers must use TerminalColor.DEFAULT
        // to represent the absence of an explicit foreground color.
        assertThrows(NullPointerException.class, () ->
                new TextAttributes(null, TerminalColor.DEFAULT, false, false, false));
    }

    @Test
    void constructor_rejectsNullBackground() {
        // A null background color is illegal; callers must use TerminalColor.DEFAULT
        // to represent the absence of an explicit background color.
        assertThrows(NullPointerException.class, () ->
                new TextAttributes(TerminalColor.DEFAULT, null, false, false, false));
    }

    @Test
    void equalityByValue_sameFields_areEqual() {
        // TextAttributes is a value type: two instances with identical fields
        // must be equal and produce the same hash code.
        TextAttributes a = new TextAttributes(TerminalColor.RED, TerminalColor.BLUE, true, false, true);
        TextAttributes b = new TextAttributes(TerminalColor.RED, TerminalColor.BLUE, true, false, true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalityByValue_differentFields_areNotEqual() {
        // Instances that differ in any field must not be equal.
        TextAttributes a = new TextAttributes(TerminalColor.RED,   TerminalColor.DEFAULT, false, false, false);
        TextAttributes b = new TextAttributes(TerminalColor.GREEN, TerminalColor.DEFAULT, false, false, false);
        assertNotEquals(a, b);
    }

    @Test
    void builder_defaultValues_matchDefaultConstant() {
        // A builder with no explicit settings must produce a value equal to
        // TextAttributes.DEFAULT, the canonical unstyled state.
        TextAttributes built = TextAttributes.builder().build();
        assertEquals(TextAttributes.DEFAULT, built);
    }

    @Test
    void builder_setsEachFieldIndependently() {
        // Fields not explicitly set on the builder remain at their defaults,
        // so only the specified fields differ from TextAttributes.DEFAULT.
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
        // builder(base) initialises every field from the given instance, so
        // building immediately produces a value equal to the original.
        TextAttributes base = new TextAttributes(
                TerminalColor.RED, TerminalColor.BLUE, true, true, true);
        TextAttributes copy = TextAttributes.builder(base).build();
        assertEquals(base, copy);
    }

    @Test
    void builderFromBase_overridesIndividualField() {
        // builder(base) followed by a single setter changes only that field;
        // all other fields are copied unchanged from the base.
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
        // Built instances are immutable; mutating the builder after a call to
        // build() must not retroactively change previously built instances.
        TextAttributes.Builder builder = TextAttributes.builder().foreground(TerminalColor.RED);
        TextAttributes first = builder.build();
        builder.foreground(TerminalColor.GREEN);
        TextAttributes second = builder.build();
        assertEquals(TerminalColor.RED,   first.foreground());
        assertEquals(TerminalColor.GREEN, second.foreground());
    }
}
