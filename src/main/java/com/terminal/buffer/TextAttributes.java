package com.terminal.buffer;

import java.util.Objects;

/**
 * Immutable value object representing the display attributes of a single cell.
 *
 * <p>
 * Immutability is intentional: when {@code currentAttributes} is updated on the
 * buffer, already-written cells keep their original snapshot and are unaffected.
 *
 * <p>Use the {@link Builder} to create instances with only the fields you care about;
 * everything else defaults to {@link TerminalColor#DEFAULT} / {@code false}.
 */
public record TextAttributes(
        TerminalColor foreground,
        TerminalColor background,
        boolean bold,
        boolean italic,
        boolean underline)
{

    /**
     * Shared default instance: no color overrides, no style flags.
     */
    public static final TextAttributes DEFAULT = new TextAttributes(
            TerminalColor.DEFAULT, TerminalColor.DEFAULT, false, false, false);

    public TextAttributes{
        Objects.requireNonNull(foreground, "foreground must not be null");
        Objects.requireNonNull(background, "background must not be null");
    }

    @Override
    public String toString() {
        return "TextAttributes{"
                + "fg=" + foreground
                + ", bg=" + background
                + ", bold=" + bold
                + ", italic=" + italic
                + ", underline=" + underline
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a builder pre-populated from an existing instance.
     */
    public static Builder builder(TextAttributes base) {
        return new Builder()
                .foreground(base.foreground)
                .background(base.background)
                .bold(base.bold)
                .italic(base.italic)
                .underline(base.underline);
    }

    public static final class Builder {
        private TerminalColor foreground = TerminalColor.DEFAULT;
        private TerminalColor background = TerminalColor.DEFAULT;
        private boolean bold = false;
        private boolean italic = false;
        private boolean underline = false;

        private Builder() {
        }

        public Builder foreground(TerminalColor foreground) {
            this.foreground = foreground;
            return this;
        }

        public Builder background(TerminalColor background) {
            this.background = background;
            return this;
        }

        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }

        public Builder italic(boolean italic) {
            this.italic = italic;
            return this;
        }

        public Builder underline(boolean underline) {
            this.underline = underline;
            return this;
        }

        public TextAttributes build() {
            return new TextAttributes(foreground, background, bold, italic, underline);
        }
    }
}
