package xjs.data.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.data.StringType;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Basic type responsible for streaming and containerizing tokens.
 */
public abstract class Tokenizer implements Closeable {

    /**
     * A reader tracking characters and positional data.
     */
    protected final PositionTrackingReader reader;

    /**
     * Indicates whether this tokenizer should generate
     * containers recursively on the fly.
     */
    protected final boolean containerized;

    /**
     * The index at which the current token starts.
     */
    protected int index;

    /**
     * The line on which the current token starts.
     */
    protected int line;

    /**
     * The first column or offset of the current token.
     */
    protected int column;

    /**
     * Begins parsing tokens when given a typically ongoing input.
     *
     * @param is            Any source of character bytes.
     * @param containerized Whether to generate containers on the fly.s
     * @throws IOException If the reader fails to parse any initial bytes.
     */
    public Tokenizer(final InputStream is, final boolean containerized) throws IOException {
        this(PositionTrackingReader.fromIs(is), containerized);
    }

    /**
     * Begins parsing tokens when given a full text as the source.
     *
     * @param text          The full text and source of tokens.
     * @param containerized Whether to generate containers on the fly.
     */
    public Tokenizer(final String text, final boolean containerized) {
        this(PositionTrackingReader.fromString(text), containerized);
    }

    /**
     * Begins parsing tokens from any other source.
     *
     * @param reader        A reader providing characters and positional data.
     * @param containerized Whether to generate containers on the fly.
     */
    public Tokenizer(final PositionTrackingReader reader, final boolean containerized) {
        this.reader = reader;
        this.containerized = containerized;
    }

    /**
     * Exposes the reader directly to provide additional context to any
     * callers and facilitate parsing exotic formats.
     *
     * @return The underlying reader.
     */
    public PositionTrackingReader getReader() {
        return this.reader;
    }

    /**
     * Gets the next token available, containerizing if specified.
     *
     * @return The next possible token, or else <code>null</code>.
     * @throws IOException If the given reader throws an exception.
     */
    public final @Nullable Token next() throws IOException {
        if (this.containerized) {
            return this.containerize(this.single());
        }
        return this.single();
    }

    protected Token containerize(final Token t) {
        final TokenType type = this.getOpenerType(t);
        if (type != null) {
            return new TokenStream(this, t, type);
        }
        return t;
    }

    protected @Nullable TokenType getOpenerType(final Token t) {
        if (t == null) {
            return null;
        } else if (t.isSymbol('(')) {
            return TokenType.PARENTHESES;
        } else if (t.isSymbol('{')) {
            return TokenType.BRACES;
        } else if (t.isSymbol('[')) {
            return TokenType.BRACKETS;
        }
        return null;
    }

    /**
     * Reads a single, non-container token from the given reader.
     *
     * <p><b>Note:</b> there is a <b>known bug</b> with this method.
     * Numbers with incomplete exponents will <em>not</em> be returned
     * as multiple symbols and will instead be returned as a single
     * word. This violates the contract that symbol characters--including
     * <code>-</code> and <code>+</code>--will always be represented as
     * {@link SymbolToken symbol tokens}. An eventual fix is expected,
     * but the exact solution has not yet been determined.
     *
     * @return The next possible token, or else <code>null</code>.
     * @throws IOException If the given reader throws an exception.
     */
    protected abstract @Nullable Token single() throws IOException;

    protected void startReading() {
        final PositionTrackingReader reader = this.reader;
        this.index = reader.index;
        this.line = reader.line;
        this.column = reader.column;
    }

    protected Token quote(final char quote) throws IOException {
        final String parsed = this.reader.readQuoted(quote);
        if (parsed.isEmpty() && quote == '\'' && this.reader.readIf('\'')) {
            final String multi = this.reader.readMulti(false);
            return this.newStringToken(multi, StringType.MULTI);
        }
        final StringType type = quote == '\'' ? StringType.SINGLE : StringType.DOUBLE;
        return this.newStringToken(parsed, type);
    }

    protected Token comment(final char c) throws IOException {
        final PositionTrackingReader reader = this.reader;
        if (c == '#') {
            return reader.readHashComment();
        }
        reader.read();

        final int next = reader.current;
        if (next == '/') {
            return reader.readLineComment();
        } else if (next == '*') {
            return reader.readBlockComment();
        }
        return this.newSymbolToken(c);
    }

    protected Token word() throws IOException {
        final PositionTrackingReader reader = this.reader;
        final int i = this.index;
        reader.startCapture();
        do {
            final char c = (char) reader.current;
            if (this.isLegalWordCharacter(c)) {
                reader.read();
            } else if (reader.index - i == 0) {
                reader.read();
                reader.invalidateCapture();
                return this.newSymbolToken(c);
            } else {
                break;
            }
        } while (!reader.isEndOfText());
        return this.newWordToken(reader.endCapture());
    }

    protected boolean isLegalWordCharacter(final char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    protected Token dot() throws IOException {
        this.reader.read();
        if (this.reader.isDigit()) {
            return this.number();
        }
        return this.newSymbolToken('.');
    }

    protected Token number() throws IOException {
        final PositionTrackingReader reader = this.reader;
        reader.startCapture();
        if (reader.current == '0') { // disallow octal number format
            reader.read();
            if (Character.isDigit(reader.current)) {
                reader.invalidateCapture();
                return this.word();
            } else if (reader.current == '.') {
                reader.read();
                if (!reader.isDigit()) {
                    return this.newNumberToken(reader.endCapture(), 0);
                }
            } else {
                return this.newNumberToken(reader.endCapture(), 0);
            }
        } else if (reader.current == '-') {
            reader.read();
            if (!reader.isDigit()) {
                if (reader.readInfinity()) {
                    return this.newNumberToken(reader.endCapture(), Double.NEGATIVE_INFINITY);
                }
                reader.invalidateCapture();
                return this.newSymbolToken('-');
            }
        }
        reader.readAllDigits();
        if (reader.readIf('.')) {
            if (!reader.isDigit()) {
                return this.parseNumber(reader.endCapture());
            }
            reader.readAllDigits();
        }
        if (reader.readIf('e') || reader.readIf('E')) {
            if (!reader.readIf('+')) {
                reader.readIf('-'); // if no other numbers, result is ignored
            }
            if (!reader.readDigit()) {
                return this.newWordToken(reader.endCapture());
            }
            reader.readAllDigits();
        }
        return this.parseNumber(reader.endCapture());
    }

    protected Token newLine() throws IOException {
        this.reader.read();
        return new SymbolToken(this.index, this.reader.index, this.line, this.column, TokenType.BREAK, '\n');
    }

    protected Token newStringToken(final String parsed, final StringType type) {
        return new StringToken(this.index, this.reader.index, this.line, this.reader.line, this.column, type, parsed);
    }

    protected Token parseNumber(final String capture) {
        return this.newNumberToken(capture, Double.parseDouble(capture));
    }

    protected Token newNumberToken(final String capture, final double number) {
        return new NumberToken(this.index, this.reader.index, this.line, this.column, number, capture);
    }

    protected Token newWordToken(final String capture) {
        return new ParsedToken(this.index, this.reader.index, this.line, this.column, TokenType.WORD, capture);
    }

    protected Token newSymbolToken(final char symbol) {
        return new SymbolToken(this.index, this.reader.index, this.line, this.column, symbol);
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    protected void updateSpan(final Token t, final int s, final int e, final int l, final int o) {
        t.setStart(s);
        t.setEnd(e);
        t.setLine(l);
        t.setOffset(o);
    }

    protected void updateSpan(final Token t, final int s, final int e, final int l, final int ll, final int o) {
        this.updateSpan(t, s, e, l, o);
        t.setLastLine(ll);
    }
}
