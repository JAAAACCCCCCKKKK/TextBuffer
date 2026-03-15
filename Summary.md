# TextBuffer

## Solution Overview

**TextBuffer** is a Unicode-aware terminal text buffer library in Java. It models the memory layer of a terminal emulator — the part that stores what is currently displayed on screen and what has scrolled off the top.

---

## Architecture

The design follows a strict layered hierarchy:

```
TerminalBuffer  (facade — manages screen, scrollback, cursor, operations)
    └── Line    (a single row — manages cells and wide-char pairs)
          └── Cell  (atomic unit — codepoint + TextAttributes + CellType)
                        TextAttributes (immutable record — colors, bold, italic, underline)
WideCharUtil    (static utility — Unicode width classification)
```

Each layer has a single responsibility. `TerminalBuffer` is the only public interface a consumer needs to interact with.

---

## Key Design Decisions

### 1. Wide Character Representation (WIDE_LEAD / WIDE_CONT)

CJK ideographs, fullwidth forms, and most emoji occupy **two terminal columns**. Rather than storing them outside the grid, each wide character occupies two adjacent `Cell` slots:

- `WIDE_LEAD` — holds the real codepoint and attributes
- `WIDE_CONT` — a blank placeholder; its sole purpose is column reservation

**Trade-off:** Doubles cell usage for wide characters, but keeps the grid rectangular and index-predictable. The alternative (sparse storage with variable-width slots) would complicate all cursor arithmetic.

When either half of a wide pair is overwritten, the other half is automatically cleared to a narrow space. This prevents "half-character" visual artifacts — a correctness requirement any emulator must satisfy.

**Edge case handled:** If a wide character lands exactly on the last column of a line, there is no room for `WIDE_CONT`. The implementation writes a space at that column and wraps the wide character to column 0 of the next line.

---

### 2. Deferred Wrap (`pendingWrap`)

When a character lands on the last column, the cursor **does not move to the next line immediately**. Instead, `pendingWrap = true` is set. The line break only fires when the *next* character is written.

**Why:** This matches the behavior of real terminals (VT100/xterm). A character at the last column followed immediately by a `\n` should produce one newline, not two. Without this, every line that fills exactly would generate a spurious blank row.

---

### 3. Immutable `TextAttributes`

`TextAttributes` is a Java `record`. Cell attributes are captured by value at write-time and cannot be mutated after the fact.

**Why:** In a real terminal, changing the current SGR (color/bold/etc.) only affects *subsequent* characters. Previously written text is frozen. The record type enforces this at the language level and makes attributes safe to compare by value.

---

### 4. Global Row Addressing

All content — scrollback and screen — is addressable through a single integer index:

```
[0 .. scrollback.size()-1]   → scrollback (oldest first)
[scrollback.size() .. total] → screen (top to bottom)
```

**Why:** A caller rendering all visible history (e.g., a scroll-back pane) can iterate a contiguous range rather than managing two separate data structures and offset arithmetic. It simplifies content-access APIs like `getCharAt(row, col)`.

---

### 5. No External Unicode Library

`WideCharUtil` uses hardcoded Unicode block ranges to determine whether a codepoint is wide. No third-party dependencies.

**Trade-off:** The coverage is good for common cases (CJK Unified Ideographs, Hangul Syllables, Japanese kana, emoji supplementary plane, fullwidth ASCII). However, it does not handle:

- Variation selectors (e.g., U+FE0F making a narrow emoji wide)
- Fitzpatrick skin-tone modifiers
- Zero-width joiners (ZWJ sequences used in family/flag emoji)
- Unicode version updates automatically

For an isolated, dependency-free library this is a practical trade-off. A production emulator would use a library like `icu4j` or call platform APIs.

---

### 6. Resize Semantics

On `resize(newWidth, newHeight)`:

- **Width increase**: Each line is padded with blank cells on the right.
- **Width decrease**: Lines are truncated. A `WIDE_LEAD` at the new last column (whose `WIDE_CONT` was just cut off) is replaced with a blank — avoiding a dangling half-pair.
- **Height increase**: Blank lines are added at the bottom of the screen.
- **Height decrease**: Lines are removed from the bottom. If active screen lines overflow, the oldest are pushed to scrollback.
- **Cursor clamping**: Cursor is clamped to `(min(cx, newWidth-1), min(cy, newHeight-1))`.
- **Pending wrap cancellation**: If the cursor is no longer at the last column after resize, the deferred-wrap flag is cleared.

**Trade-off:** Height shrink pushes content to scrollback rather than discarding it. This is correct behavior for a user who has content in their terminal and resizes the window — text should not silently vanish.

---

## Trade-offs Summary

| Decision | Chosen Approach | Alternative | Reason |
|---|---|---|---|
| Wide char storage | Two-cell pair in grid | Sparse/variable-width grid | Keeps column arithmetic O(1) |
| Line wrapping trigger | Deferred (`pendingWrap`) | Immediate | Matches VT100 spec |
| Unicode width | Hardcoded ranges | `icu4j` or OS API | Dependency-free |
| Attributes | Immutable record | Mutable per-cell | Matches terminal semantics |
| Scrollback | `Deque<Line>` with cap | Unlimited or file-backed | Simple, bounded memory |
| Color model | 17 enum values | 256 / truecolor | Scope-appropriate simplicity |

---

## Gap Between This Implementation and a Real Emulator

This library implements the **buffer/model layer only**. A complete terminal emulator needs several more components:

### 1. ANSI / VT Escape Sequence Parser

The biggest gap. A real terminal receives a raw byte stream from a PTY and must parse sequences like:

- `ESC[H` (cursor home), `ESC[2J` (erase screen)
- `ESC[1;31m` (SGR — set bold + red)
- `ESC[?1049h` / `ESC[?1049l` (alternate screen buffer switch)
- `ESC[S` / `ESC[T` (scroll up/down), `ESC[L` / `ESC[M` (insert/delete lines)
- OSC sequences for window title, hyperlinks
- DEC private modes

None of this is present. This library consumes **already-parsed, semantic operations** (e.g., "write this string at cursor"). A real integration needs a parser layer (e.g., a port of `libvte`'s state machine) that translates the raw byte stream into calls on `TerminalBuffer`.

### 2. Alternate Screen Buffer

Many terminal applications (vim, less, htop) switch to a secondary screen buffer (`?1049h`) that is discarded on exit, restoring the original screen. This would require maintaining **two independent `TerminalBuffer` instances** and tracking which is active.

### 3. Line Discipline / Soft vs. Hard Wrap

The current implementation always hard-wraps. Real terminals track whether a line was wrapped by the terminal (soft wrap, joinable on resize) vs. terminated by an explicit `\r\n` (hard wrap, not joinable). This is needed so that `resize()` can **reflow** text — rewrapping lines at the new width rather than just truncating/padding. Without reflow, long lines in a resized terminal look broken.

### 4. 256-Color and Truecolor Support

`TerminalColor` has 17 enum values. Real terminals support:

- **256-color**: `ESC[38;5;Nm` indexed palette
- **Truecolor**: `ESC[38;2;R;G;Bm` full 24-bit RGB

`TextAttributes` would need to represent these (e.g., using an int for the color value alongside a type discriminator).

### 5. Selection and Clipboard

A terminal emulator needs to track a mouse-driven text selection region, compute the selected string accounting for wide chars, and put it on the clipboard. `TerminalBuffer` has no selection state.

### 6. Hyperlinks (OSC 8)

Modern terminals support `ESC]8;;url\aBEL` sequences that attach a URL to a span of cells. Cells would need a `url` field in `TextAttributes`.

### 7. Thread Safety

Terminal output from a PTY arrives on a background I/O thread while the UI renders on another. The buffer currently has no synchronization. A production implementation needs either a lock or a command queue (single-writer, single-reader).

### 8. Line Insertion / Deletion (`ESC[L` / `ESC[M`)

The VT100 `IL` (insert line) and `DL` (delete line) operations shift lines within a scroll region. `TerminalBuffer` has no scroll region concept and no insert/delete-line operations, which are required for full-screen apps like `vim`.

### 9. Emoji and ZWJ Sequence Width

As noted, `WideCharUtil` does not handle:

- Variation selectors (U+FE0F) that promote a codepoint to wide
- ZWJ sequences (multiple codepoints rendered as one glyph, e.g., `👨‍👩‍👧`)
- Regional indicator pairs (flags)

These would require grapheme-cluster-level processing, not codepoint-by-codepoint processing.

---

## Potential Improvements Not Yet Implemented

1. **Soft-wrap flag per line** — enables correct text reflow on resize
2. **Scroll regions** — `DECSTBM` sets top/bottom margins; scrolling and line-insert/delete operate within the region
3. **Alternate screen buffer** — second `TerminalBuffer` instance + toggle
4. **256-color / truecolor in `TextAttributes`** — change color representation from enum to a sealed class or int
5. **`insertLine` / `deleteLine` operations** — needed for full VT100 compliance
6. **Unicode grapheme cluster segmentation** — correct handling of emoji ZWJ sequences
7. **Selection model** — mouse selection, double-click word selection, rectangle selection
8. **OSC hyperlink metadata in cells** — per-cell URL via an extra `TextAttributes` field
9. **Concurrent access** — a `ReentrantLock` or copy-on-write snapshot for the renderer
