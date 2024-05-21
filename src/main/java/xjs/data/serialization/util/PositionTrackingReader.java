package xjs.data.serialization.util;

import xjs.data.comments.CommentStyle;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.CommentToken;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * A highly optimized counterpart to {@link Reader} which reduces the amount of
 * read operations to be performed on the underlying input stream. This
 * implementation is <b>not thread-safe</b>. It deliberately <b>deviates from
 * the contract</b> of {@link Reader}. In particular, it does not flush or close
 * the wrapped reader, nor does it ensure that the wrapped reader is open.
 */
public abstract class PositionTrackingReader implements Closeable {

    protected static final int DEFAULT_BUFFER_SIZE = 1024;
    protected static final int MIN_BUFFER_SIZE = 8;

    /**
     * The index of the current character, starting at 0.
     */
    public int index;

    /**
     * The line number, starting at 0.
     */
    public int line;

    /**
     * The column of the current character, starting at 0.
     */
    public int column;

    /**
     * The number of lines read since the last time whitespace was skipped.
     *
     * <p><b>Note:</b> This API may change in the future.
     */
    public int linesSkipped;

    /**
     * The current character, or else -1.
     */
    public int current;

    protected StringBuilder capture;
    protected int captureStart;

    protected PositionTrackingReader() {
        this.index = -1;
        this.line = 0;
        this.column = -1;
        this.captureStart = -1;
    }

    /**
     * Generates an efficient reader optimized for existing strings.
     *
     * @param s The <em>full</em> text being parsed.
     * @return A new reader for parsers and tokenizers.
     */
    public static PositionTrackingReader fromString(final String s) {
        return new DirectStringReader(s);
    }

    /**
     * Generates a reader optimized for {@link InputStream}s and disk IO.
     *
     * <p>Unlike {@link Reader}, users should be aware that read calls
     * are <b>not synchronized</b>. This utility is <b>not thread safe</b>,
     * does not flush the underlying {@link InputStream}, and deliberately
     * dodges some of the {@link Reader} API contract.
     *
     * @param is The source of characters being iterated over.
     * @return A new reader for parsers and tokenizers.
     * @throws IOException If the initial read operation fails.
     */
    public static PositionTrackingReader fromIs(final InputStream is) throws IOException {
        return fromIs(is, false);
    }

    /**
     * Variant of {@link #fromIs(InputStream)} specifying whether to capture
     * the full text of the input. This text can be returned at any point
     * by calling {@link #getFullText}. Callers should be aware that the
     * text output <em>will</em> change as the reader progresses.
     *
     * @param is              The source of characters being iterated over.
     * @param captureFullText Whether to preserve the full text input.
     * @return A new reader for parsers and tokenizers.
     * @throws IOException If the initial read operation fails.
     */
    public static PositionTrackingReader fromIs(
            final InputStream is, final boolean captureFullText) throws IOException {
        return fromIs(is, DEFAULT_BUFFER_SIZE, captureFullText);
    }

    /**
     * Variant of {@link #fromIs(InputStream, boolean)} specifying the size
     * of the buffer used by the reader.
     *
     * @param is              The source of characters being iterated over.
     * @param size            The size of the underlying character buffer.
     * @param captureFullText Whether to preserve the full text input.
     * @return A new reader for parsers and tokenizers.
     * @throws IOException If the initial read operation fails.
     */
    public static PositionTrackingReader fromIs(
            final InputStream is, final int size, final boolean captureFullText) throws IOException {
        return fromReader(new InputStreamReader(is, StandardCharsets.UTF_8), size, captureFullText);
    }

    /**
     * Generates a reader optimized for {@link Reader}s and disk IO.
     *
     * <p>Unlike {@link Reader}, users should be aware that read calls
     * are <b>not synchronized</b>. This utility is <b>not thread safe</b>,
     * does not flush the underlying {@link InputStream}, and deliberately
     * dodges some of the {@link Reader} API contract.
     *
     * @param reader The source of characters being iterated over.
     * @return A new reader for parsers and tokenizers.
     * @throws IOException If the initial read operation fails.
     */
    public static PositionTrackingReader fromReader(final Reader reader) throws IOException {
        return fromReader(reader, false);
    }

    /**
     * Variant of {@link #fromReader(Reader)} specifying whether to capture
     * the full text of the input. This text can be returned at any point
     * by calling {@link #getFullText}. Callers should be aware that the
     * text output <em>will</em> change as the reader progresses.
     *
     * @param reader          The source of characters being iterated over.
     * @param captureFullText Whether to preserve the full text input.
     * @return A new reader for parsers and tokenizers.
     * @throws IOException If the initial read operation fails.
     */
    public static PositionTrackingReader fromReader(
            final Reader reader, final boolean captureFullText) throws IOException {
        return fromReader(reader, DEFAULT_BUFFER_SIZE, captureFullText);
    }

    /**
     * Variant of {@link #fromReader(Reader, boolean)} specifying the size
     * of the buffer used by the reader.
     *
     * @param reader          The source of characters being iterated over.
     * @param size            The size of the underlying character buffer.
     * @param captureFullText Whether to preserve the full text input.
     * @return A new reader for parsers and tokenizers.
     * @throws IOException If the initial read operation fails.
     */
    public static PositionTrackingReader fromReader(
            final Reader reader, final int size, final boolean captureFullText) throws IOException {
        if (size < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("buffer size < " + MIN_BUFFER_SIZE);
        }
        return new ReaderReader(reader, size, captureFullText);
    }

    /**
     * Returns the full text of the input, or as much as has been read up
     * to this point.
     *
     * <p>Users should be aware that the return value of this method may
     * <b>mutate</b> over time.
     *
     * @return The full text of the input.
     * @throws UnsupportedOperationException If the reader is not configured
     *                                       to capture the full text input.
     */
    public abstract CharSequence getFullText();

    /**
     * Indicates whether the reader is configured to retain the full text
     * from the input source.
     *
     * @return <code>true</code>, if the reader is configured to capture
     *         the full text.
     */
    public abstract boolean isCapturingFullText();

    protected abstract void appendToCapture();

    protected abstract String slice(final int s, final int e);

    /**
     * Advances the reader by a single character.
     *
     * @throws IOException If the underlying reader throws an exception.
     */
    public abstract void read() throws IOException;

    /**
     * Gets the next character after the current index, else -1.
     *
     * @return The next character, else -1
     * @throws IOException If the underlying reader throws an exception.
     */
    public abstract int peek() throws IOException;

    /**
     * Advances the reader by a single character, <em>if</em> the current
     * character equals the input.
     *
     * @param c The expected character at this position.
     * @return True, if the current character matches the input.
     * @throws IOException If the underlying reader throws an exception.
     */
    public boolean readIf(final char c) throws IOException {
        if (this.current != c) {
            return false;
        }
        this.read();
        return true;
    }

    /**
     * Advances the reader by a single character, <em>if</em> the current
     * character equals the input. If the input <em>does not</em> match,
     * a syntax exception will be throws.
     *
     * @param c The expected character at this position.
     * @throws IOException If the underlying reader throws an exception.
     */
    public void expect(final char c) throws IOException {
        if (!this.readIf(c)) {
            throw this.expected(c);
        }
    }

    /**
     * Advances the reader to the very end of the input.
     *
     * @return The full text input to the reader.
     * @throws IOException If the underlying reader throws an exception.
     */
    public String readToEnd() throws IOException {
        do {
            this.read();
        } while (!this.isEndOfText());
        return this.getFullText().toString();
    }

    /**
     * Marks the current position as the beginning of a <b>capture</b>.
     *
     * <p>The end of this capture can be exposed by calling {@link #endCapture}.
     */
    public void startCapture() {
        if (this.capture == null) {
            this.capture = new StringBuilder();
        }
        this.captureStart = this.index;
    }

    /**
     * Excludes a sequence of characters from the capture output.
     */
    public void pauseCapture() {
        this.appendToCapture();
        this.captureStart = -1;
    }

    /**
     * Terminates the capture, generating new string value from the contents.
     *
     * @return The full text of the capture buffer.
     */
    public String endCapture() {
        return this.endCapture(this.index);
    }

    /**
     * Terminates the capture, generating new string value from the contents.
     *
     * @param e The last index to include in the capture.
     * @return The full text of the capture buffer.
     */
    public String endCapture(final int e) {
        final String captured;
        if (this.capture.length() > 0) {
            this.appendToCapture();
            if (e < this.index) {
                this.capture.setLength(
                    this.capture.length() - (this.index - e));
            }
            captured = this.capture.toString();
            this.capture.setLength(0);
        } else {
            captured = this.slice(this.captureStart, e);
        }
        this.captureStart = -1;
        return captured;
    }

    /**
     * Terminates this capture, effectively destroying any data captured.
     */
    public void invalidateCapture() {
        if (this.capture != null) {
            this.capture.setLength(0);
        }
        this.captureStart = -1;
    }

    /**
     * Skips all whitespace from the current point.
     *
     * @throws IOException If the underlying reader throws an exception.
     */
    public void skipWhitespace() throws IOException {
        this.skipWhitespace(true);
    }

    /**
     * Skips whitespace and optionally resets the {@link #linesSkipped}
     * counter to 0.
     *
     * @param reset Whether to reset the {@link #linesSkipped} counter.
     * @throws IOException If the underlying reader throws an exception.
     */
    public void skipWhitespace(final boolean reset) throws IOException {
        if (reset) {
            this.linesSkipped = 0;
        }
        while (this.isWhitespace()) {
            this.read();
        }
    }

    /**
     * Skips all whitespace without advancing to the next line.
     *
     * @throws IOException If the underlying reader throws an exception.
     */
    public void skipLineWhitespace() throws IOException {
        while (this.isLineWhitespace()) {
            this.read();
        }
    }

    /**
     * Skips the given number of whitespace characters.
     *
     * @param offset The offset (e.g. expected column) to read to.
     * @return The number of whitespace characters skipped.
     * @throws IOException If the underlying reader throws an exception.
     */
    public int skipToOffset(final int offset) throws IOException {
        for (int i = 0; i < offset; i++) {
            if (!this.isLineWhitespace()) {
                return i + 1;
            }
            this.read();
        }
        return offset;
    }

    /**
     * Skips <em>all</em> characters until the next new line or end of input.
     *
     * @return The exclusive index of the last non-whitespace character
     * @throws IOException If the underlying reader throws an exception.
     */
    public int skipToNL() throws IOException {
        int last = this.index;
        while (this.current != '\n' && this.current != -1) {
            if (!this.isLineWhitespace()) {
                last = this.index + 1;
            }
            this.read();
        }
        return last;
    }

    /**
     * Advances the reader if the current character is a digit.
     *
     * @return true, if the current character is a digit.
     * @throws IOException If the underlying reader throws an exception.
     */
    public boolean readDigit() throws IOException {
        if (!this.isDigit()) {
            return false;
        }
        this.read();
        return true;
    }

    /**
     * Reads all possible digits from the current point.
     *
     * @throws IOException If the underlying reader throws an exception.
     */
    public void readAllDigits() throws IOException {
        while (this.isDigit()) {
            this.read();
        }
    }

    /**
     * Reads a formatted number from the current index. If the reader
     * encounters a syntax error, a {@link SyntaxException} will be
     * thrown.
     *
     * @return The parsed number.
     * @throws IOException If the underlying reader throws an exception.
     */
    public double readNumber() throws IOException {
        this.startCapture();
        this.readIf('-');

        final int firstDigit = this.current;
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        if (firstDigit != '0') {
            this.readAllDigits();
        }
        this.readDecimal();
        this.readExponent();

        return Double.parseDouble(this.endCapture());
    }

    /**
     * Reads a decimal value as part of a number. If a decimal
     * is found but further digits are <em>not</em>, a {@link
     * SyntaxException} will be thrown.
     *
     * @return true, if a decimal was found.
     * @throws IOException If the underlying reader throws an exception.
     */
    public boolean readDecimal() throws IOException {
        if (!this.readIf('.')) {
            return false;
        }
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        this.readAllDigits();
        return true;
    }

    /**
     * Reads an exponent value as part of a number. If an exponent
     * indicator is found, but further digits are <em>not</em>, a
     * {@link SyntaxException} will be thrown.
     *
     * @return true, if an exponent was found.
     * @throws IOException If the underlying reader throws an exception.
     */
    public boolean readExponent() throws IOException {
        if (!this.readIf('e') && !this.readIf('E')) {
            return false;
        }
        if (!this.readIf('+')) {
            this.readIf('-');
        }
        if (!this.readDigit()) {
            throw this.expected("digit");
        }
        this.readAllDigits();
        return true;
    }

    /**
     * Reads a single or double-quoted string from the current index.
     * If the reader encounters a syntax error, a {@link SyntaxException}
     * will be thrown.
     *
     * @param quote The type of quote being read.
     * @return The parsed contents of this string.
     * @throws IOException If the underlying reader throws an exception.
     */
    public String readQuoted(final char quote) throws IOException {
        this.read();
        this.startCapture();
        while (this.current != quote) {
            if (this.current == '\\') {
                this.pauseCapture();
                this.readEscape(quote);
                this.startCapture();
            } else if (this.current < 0x20) {
                throw this.expected("valid string character");
            } else {
                this.read();
            }
        }
        final String string = this.endCapture();
        this.read();
        return string;
    }

    protected void readEscape(final char quote) throws IOException {
        this.read();
        if (this.current == quote) {
            this.capture.append(quote);
            this.read();
            return;
        }
        switch (this.current) {
            case '"', '/', '\\' -> this.capture.append((char) this.current);
            case 'b' -> this.capture.append('\b');
            case 'f' -> this.capture.append('\f');
            case 'n' -> this.capture.append('\n');
            case 'r' -> this.capture.append('\r');
            case 't' -> this.capture.append('\t');
            case 'u' -> {
                final char[] hexChars = new char[4];
                for (int i = 0; i < 4; i++) {
                    this.read();
                    if (!isHexDigit()) {
                        throw this.expected("hexadecimal digit");
                    }
                    hexChars[i] = (char) this.current;
                }
                this.capture.append((char) Integer.parseInt(new String(hexChars), 16));
            }
            default -> throw this.expected("valid escape sequence");
        }
        this.read();
    }

    /**
     * Reads a standard multiline string in DJS or Hjson format.
     *
     * @param readQuotes Whether to read the first three quotes of the token.
     * @return The parsed text of the token.
     * @throws IOException If the underlying reader throws an exception.
     */
    public String readMulti(final boolean readQuotes) throws IOException {
        if (readQuotes) {
            this.expect('\'');
            this.expect('\'');
            this.expect('\'');
        }
        final StringBuilder sb = new StringBuilder();
        final int offset = this.column - 3;

        this.skipLineWhitespace();
        if (this.current == '\n') {
            this.read();
            this.skipToOffset(offset);
        }

        int triple = 0;
        while (true) {
            if (this.current < 0) {
                throw this.expected("end of multiline string (''')");
            } else if (this.current == '\'') {
                triple++;
                this.read();
                if (triple == 3) {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    return sb.toString();
                } else {
                    continue;
                }
            } else {
                while (triple > 0) {
                    sb.append('\'');
                    triple--;
                }
            }
            if (this.current == '\n') {
                sb.append('\n');
                this.read();
                this.skipToOffset(offset);
            } else {
                if (this.current != '\r') {
                    sb.append((char) this.current);
                }
                this.read();
            }
        }
    }

    public CommentToken readLineComment() throws IOException {
        final int s = this.index - 1;
        final int o = this.column - 1;
        this.expect('/');
        if (this.readIf('/')) {
            return this.readSingleComment(CommentStyle.LINE_DOC, s, o);
        }
        return this.readSingleComment(CommentStyle.LINE, s, o);
    }

    public CommentToken readHashComment() throws IOException {
        this.expect('#');
        return this.readSingleComment(
            CommentStyle.HASH, this.index - 1, this.column - 1);
    }

    private CommentToken readSingleComment(
            final CommentStyle type, final int s, final int o) throws IOException {
        if (this.isLineWhitespace()) {
            this.read();
        }
        this.startCapture();
        final int e = this.skipToNL();
        return new CommentToken(s, e, this.line, o, type, this.endCapture(e));
    }

    public CommentToken readBlockComment() throws IOException {
        final int s = this.index - 1;
        final int o = this.column - 1;
        this.expect('*');
        if (this.readIf('*')) {
            return this.readMultiComment(CommentStyle.MULTILINE_DOC, s, o);
        }
        return this.readMultiComment(CommentStyle.BLOCK, s, o);
    }

    private CommentToken readMultiComment(
            final CommentStyle type, final int s, final int o) throws IOException {
        final int line = this.line;
        this.skipLineWhitespace();
        this.readIf('\n');

        final StringBuilder output = new StringBuilder();

        while (true) {
            if (this.current == -1) {
                throw this.expected("end of comment (*/)");
            }
            final int lineStart = this.skipToBlockLineStart();
            if (lineStart == -1) {
                this.expect('/');
                break;
            }
            this.appendLine(output, lineStart);
            if (this.readIf('/')) {
                break;
            }
            if (this.current != -1) {
                this.read();
            }
        }
        final int len = output.length();
        if (len > 0 && output.charAt(len - 1) == '\n') {
            output.setLength(len - 1);
        }
        return new CommentToken(
            s, this.index, line, this.line, o, type, output.toString());
    }

    protected int skipToBlockLineStart() throws IOException {
        this.skipLineWhitespace();
        if (this.current == '*') {
            this.read();
            if (this.current == '/') {
                return -1;
            }
            if (this.isLineWhitespace()) {
                this.read();
            }
        }
        return this.index;
    }

    protected void appendLine(final StringBuilder output, final int lineStart) throws IOException {
        int lastChar = output.length();
        while (this.current != -1) {
            if (this.current == '\n') {
                output.setLength(lastChar);
                output.append('\n');
                return;
            } else if (this.current == '*') {
                this.read();
                if (this.current == '/') {
                    output.setLength(lastChar);
                    return;
                }
                lastChar = output.length();
            } else if (!this.isWhitespace()) {
                lastChar = output.length() + 1;
            }
            output.append((char) this.current);
            this.read();
        }
    }

    /**
     * Returns whether the current character is any form of non line break
     * character.
     *
     * @return true, if the current character is line whitespace.
     */
    public boolean isLineWhitespace() {
        return this.current == ' ' || this.current == '\t' || this.current == '\r';
    }

    /**
     * Returns whether the current character is <em>any kind</em> of
     * whitespace character.
     *
     * @return true, if the current character is whitespace.
     */
    public boolean isWhitespace() {
        return this.current == ' '
            || this.current == '\t'
            || this.current == '\n'
            || this.current == '\r';
    }

    /**
     * Returns whether the current character is a digit.
     *
     * @return true, if the current character is a digit.
     */
    public boolean isDigit() {
        return this.current >= '0' && this.current <= '9';
    }

    /**
     * Returns whether the current character is a <em>hex</em> digit.
     *
     * @return true, if the current character is a hex digit.
     */
    public boolean isHexDigit() {
        return this.current >= '0' && this.current <= '9'
            || this.current >= 'a' && this.current <= 'f'
            || this.current >= 'A' && this.current <= 'F';
    }

    /**
     * Returns whether the reader has run out of text to read.
     *
     * @return true, if the reader has run out of text to read.
     */
    public boolean isEndOfText() {
        return this.current == -1;
    }

    /**
     * Returns a syntax exception indicating that a character was
     * expected at the current position.
     *
     * @param expected The expected character.
     * @return A new syntax exception.
     */
    public SyntaxException expected(final char expected) {
        return SyntaxException.expected(expected, this.line, this.column);
    }

    /**
     * Returns a syntax exception indicating that <em>something</em>
     * was expected.
     *
     * @param expected A description of the expected characters.
     * @return A new syntax exception.
     */
    public SyntaxException expected(final String expected) {
        return SyntaxException.expected(expected, this.line, this.column);
    }

    /**
     * Returns a syntax exception indicating that the current character
     * was not expected.
     *
     * @return A new syntax exception.
     */
    public SyntaxException unexpected() {
        return this.unexpected((char) this.current);
    }

    /**
     * Returns a syntax exception indicating that a character was
     * <em>not</em> expected at the current position.
     *
     * @param unexpected The expected character.
     * @return A new syntax exception.
     */
    public SyntaxException unexpected(final char unexpected) {
        return SyntaxException.unexpected(unexpected, this.line, this.column);
    }

    /**
     * Returns a syntax exception indicating that something was not
     * expected at the current position.
     *
     * @param unexpected A description of the unexpected characters.
     * @return A new syntax exception.
     */
    public SyntaxException unexpected(final String unexpected) {
        return SyntaxException.unexpected(unexpected, this.line, this.column);
    }

    private static class ReaderReader extends PositionTrackingReader {
        final Reader reader;
        final char[] buffer;

        StringBuilder out;
        int bufferIndex;
        int fill;

        ReaderReader(
                final Reader reader, final int size, final boolean captureFullText) throws IOException {
            this.reader = reader;
            this.buffer = new char[size];
            this.bufferIndex = 0;
            this.fill = 0;
            if (captureFullText) this.out = new StringBuilder();
            this.read();
        }

        @Override
        public CharSequence getFullText() {
            if (this.out == null) {
                throw new UnsupportedOperationException("output not configured");
            }
            return this.out;
        }

        @Override
        public boolean isCapturingFullText() {
            return this.out != null;
        }

        public void startCapture() {
            if (this.capture == null) {
                this.capture = new StringBuilder();
            }
            this.captureStart = this.bufferIndex - 1;
        }

        @Override
        protected void appendToCapture() {
            final int end = this.current == -1 ? this.bufferIndex : this.bufferIndex - 1;
            this.capture.append(this.buffer, this.captureStart, end - this.captureStart);
        }

        @Override
        protected String slice(final int s, final int e) {
            return new String(this.buffer, s, e - s);
        }

        @Override
        public int peek() throws IOException {
            if (this.current == -1) {
                return -1;
            }
            if (this.bufferIndex < this.fill) {
                return this.buffer[this.bufferIndex];
            }
            if (this.captureStart != -1) {
                this.appendToCapture();
                this.captureStart = 0;
            }
            this.fill = 1 + this.reader.read(this.buffer, 1, this.buffer.length - 1);
            this.buffer[0] = (char) this.current;
            this.bufferIndex = 1;
            if (this.fill == 0) {
                return -1;
            }
            if (this.out != null) {
                this.out.append(this.buffer, 1, this.fill - 1);
            }
            return this.buffer[this.bufferIndex];
        }

        public void read() throws IOException {
            if (this.bufferIndex == this.fill) {
                if (this.captureStart != -1) {
                    this.bufferIndex++; // Force the last index into capture
                    this.appendToCapture();
                    this.captureStart = 0;
                }
                this.fill = this.reader.read(this.buffer, 0, this.buffer.length);
                this.bufferIndex = 0;
                if (this.fill == -1) {
                    this.index++;
                    this.current = -1;
                    return;
                }
                if (this.out != null) {
                    this.out.append(this.buffer, 0, this.fill);
                }
            }
            if (this.current == '\n') {
                this.line++;
                this.linesSkipped++;
                this.column = -1;
            }
            this.index++;
            this.column++;
            this.current = this.buffer[this.bufferIndex++];
        }

        @Override
        public void close() throws IOException {
            this.reader.close();
        }
    }

    private static class DirectStringReader extends PositionTrackingReader {
        final String s;

        DirectStringReader(final String s) {
            this.s = s;
            this.read();
        }

        @Override
        public String getFullText() {
            return this.s;
        }

        @Override
        public boolean isCapturingFullText() {
            return true;
        }

        @Override
        protected void appendToCapture() {
            this.capture.append(this.s, this.captureStart, this.index);
        }

        @Override
        protected String slice(final int s, final int e) {
            return this.s.substring(s, e);
        }

        @Override
        public int peek() {
            final int i = this.index;
            final String s = this.s;
            return i < s.length() - 1 ? s.charAt(i + 1) : -1;
        }

        @Override
        public void read() {
            if (this.index == this.s.length() - 1) {
                this.index = this.s.length();
                this.current = -1;
                return;
            }
            if (this.current == '\n') {
                this.line++;
                this.linesSkipped++;
                this.column = -1;
            }
            this.column++;
            this.current = this.s.charAt(++this.index);
        }

        @Override
        public void close() {}
    }
}
