package xjs.data.serialization;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a range of characters in a file.
 *
 * @param <T> A type representing the kind of character in this range.
 */
public class Span<T> implements Comparable<Span<?>> {

    /**
     * The inclusive start index of this token.
     */
    protected int start;

    /**
     * The exclusive end index of this token.
     */
    protected int end;

    /**
     * The inclusive line number of this token.
     */
    protected int line;

    /**
     * The inclusive line number at the end of this token.
     */
    protected int lastLine;

    /**
     * The column of the start index.
     */
    protected int offset;

    /**
     * The specific type for this kind of span.
     */
    protected T type;

    /**
     * Constructs a new Span with implicit lastLine.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of span.
     */
    public Span(final int start, final int end, final int line, final int offset, final T type) {
        this(start, end, line, line, offset, type);
    }

    /**
     * Constructs a new Span given complete ranges.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive end line number of this token.
     * @param offset   The column of the start index.
     * @param type     The type of span.
     */
    public Span(
            final int start, final int end, final int line, final int lastLine, final int offset, final T type) {
        this.start = start;
        this.end = end;
        this.line = line;
        this.lastLine = lastLine;
        this.offset = offset;
        this.type = type;
    }

    /**
     * Constructs a new span given no index information. It is assumed to
     * be filled on post init.
     *
     * @param type The type of span.
     */
    protected Span(final T type) {
        this.type = type;
    }

    /**
     * Creates a slice of the given {@link CharSequence} representing
     * the region described by this span.
     *
     * @param reference The reference text being sliced.
     * @return The string subsequence of the given reference text.
     */
    public final String textOf(final CharSequence reference) {
        return reference.subSequence(this.start, this.end).toString();
    }

    /**
     * Immutable public accessor.
     */
    public int start() {
        return this.start;
    }

    /**
     * Immutable public accessor.
     */
    public int end() {
        return this.end;
    }

    /**
     * Immutable public accessor.
     */
    public int line() {
        return this.line;
    }

    /**
     * Immutable public accessor.
     */
    public int lastLine() {
        return this.lastLine;
    }

    /**
     * Immutable public accessor.
     */
    public int offset() {
        return this.offset;
    }

    /**
     * Immutable public accessor.
     */
    public T type() {
        return this.type;
    }

    /**
     * Get the number of characters represented by this span.
     */
    public int length() {
        return this.end - this.start;
    }

    /**
     * Get the number of lines covered by this token.
     */
    public int lines() {
        return this.lastLine - this.line;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Span) {
            return this.spanEquals((Span<?>) other);
        }
        return false;
    }

    /**
     * Indicate whether the given span covers the same region of text.
     *
     * @param other The span being compared to.
     * @return <code>true</code>, if the regions are the same.
     */
    public boolean spanEquals(final Span<?> other) {
        return this.start == other.start
            && this.end == other.end
            && this.line == other.line
            && this.lastLine == other.lastLine
            && this.offset == other.offset
            && this.type == other.type;
    }

    @Override
    public String toString() {
        return this.type + "(start:" + this.start + ",end:" + this.end + ",line:" +
                this.line + ",lastLine:" + lastLine + ",offset:" + this.offset + ")";
    }

    @Override
    public int compareTo(final @NotNull Span<?> o) {
        final int s = Integer.compare(this.start, o.start);
        if (s != 0) {
            return s;
        }
        return Integer.compare(this.end, o.end);
    }
}
