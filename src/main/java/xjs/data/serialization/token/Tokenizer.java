package xjs.data.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.data.StringType;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.Span;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic type responsible for streaming and containerizing tokens.
 */
public class Tokenizer implements Closeable {

    /**
     * A reader tracking characters and positional data.
     */
    protected final PositionTrackingReader reader;

    /**
     * Begins parsing tokens when given a typically ongoing input.
     *
     * @param is Any source of character bytes.
     * @throws IOException If the reader fails to parse any initial bytes.
     */
    public Tokenizer(final InputStream is) throws IOException {
        this(PositionTrackingReader.fromIs(is, true));
    }

    /**
     * Begins parsing tokens when given a full text as the source.
     *
     * @param text The full text and source of tokens.
     */
    public Tokenizer(final String text) {
        this(PositionTrackingReader.fromString(text));
    }

    /**
     * Begins parsing tokens from any other source.
     *
     * @param reader A reader providing characters and positional data.
     */
    public Tokenizer(final PositionTrackingReader reader) {
        this.reader = reader;
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
     * Generates a lazily-evaluated {@link TokenStream stream of
     * tokens} from the input text.
     *
     * @param text The full reference and source of tokens.
     * @return A new {@link TokenStream}.
     */
    public static TokenStream stream(final String text) {
        return new TokenStream(new Tokenizer(text), TokenType.OPEN);
    }

    /**
     * Generates a lazily-evaluated {@link TokenStream stream of tokens}
     * wrapping an {@link InputStream}.
     *
     * @param is The source of tokens being parsed.
     * @return A new {@link TokenStream}.
     * @throws IOException If the initial read operation throws an exception.
     */
    public static TokenStream stream(final InputStream is) throws IOException {
        return new TokenStream(new Tokenizer(is), TokenType.OPEN);
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
    protected @Nullable Token single() throws IOException {
        reader.skipLineWhitespace();
        if (reader.isEndOfText()) {
            return null;
        }
        final char c = (char) reader.current;
        final int s = reader.index;
        final int l = reader.line;
        final int o = reader.column;
        if (c == '/' || c == '#') {
            return this.comment(c, s, l, o);
        } else if (c == '\'' || c == '"') {
            return this.quote(s, l, o, c);
        } else if (c == '\n') {
            reader.read();
            return new SymbolToken(s, s + 1, l, o, TokenType.BREAK, '\n');
        } else if (c == '.') {
            return this.dot(s, l, o);
        } else if (c == '-' || Character.isDigit(c)) {
            return this.number(s, l, o);
        }
        return this.word(s, l, o);
    }

    protected Token quote(
            final int i, final int l, final int o, final char quote) throws IOException {
        final String parsed = reader.readQuoted(quote);
        if (parsed.isEmpty() && quote == '\'' && reader.readIf('\'')) {
            final String multi = reader.readMulti(false);
            return new StringToken(i, reader.index, l, reader.line, o, StringType.MULTI, multi);
        }
        final StringType type = quote == '\'' ? StringType.SINGLE : StringType.DOUBLE;
        return new StringToken(i, reader.index, l, o, type, parsed);
    }

    protected Token comment(char c, final int i, final int l, final int o) throws IOException {
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
        return new SymbolToken(i, i + 1, l, o, c);
    }

    protected Token word(final int i, final int l, final int o) throws IOException {
        do {
            final char c = (char) reader.current;
            if (this.isLegalWordCharacter(c)) {
                reader.read();
            } else if (reader.index - i == 0) {
                reader.read();
                return new SymbolToken(i, reader.index, l, o, c);
            } else {
                break;
            }
        } while (!reader.isEndOfText());
        return new Token(i, reader.index, l, o, TokenType.WORD);
    }

    protected boolean isLegalWordCharacter(final char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    protected Token dot(final int i, final int l, final int o) throws IOException {
        reader.read();
        if (reader.isDigit()) {
            return this.number(i, l, o);
        }
        return new SymbolToken(i, i + 1, l, o, '.');
    }

    protected Token number(int i, final int l, final int o) throws IOException {
        reader.startCapture();
        if (reader.current == '0') { // disallow octal number format
            reader.read();
            if (Character.isDigit(reader.current)) {
                reader.invalidateCapture();
                return word(i, l, o);
            } else if (reader.current == '.') {
                reader.read();
                if (!reader.isDigit()) {
                    reader.invalidateCapture();
                    return new NumberToken(i, i + 2, l, o, 0);
                }
            } else {
                reader.invalidateCapture();
                return new NumberToken(i, i + 1, l , o, 0);
            }
        } else if (reader.current == '-') {
            reader.read();
            if (!reader.isDigit()) {
                reader.invalidateCapture();
                return new SymbolToken(i, i + 1, l, o, '-');
            }
        }
        reader.readAllDigits();
        if (reader.readIf('.')) {
            if (!reader.isDigit()) {
                return this.parseNumber(i, l, o);
            }
            reader.readAllDigits();
        }
        if (reader.readIf('e') || reader.readIf('E')) {
            if (!reader.readIf('+')) {
                reader.readIf('-'); // if no other numbers, result is ignored
            }
            if (!reader.readDigit()) {
                return new Token(i, reader.index, l, o, TokenType.WORD);
            }
            reader.readAllDigits();
        }
        return this.parseNumber(i, l, o);
    }

    protected NumberToken parseNumber(final int i, final int l, final int o) {
        final double number = Double.parseDouble(reader.endCapture());
        return new NumberToken(i, reader.index, l, o, number);
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    /**
     * Generates a recursive {@link ContainerToken} data structure from
     * the given source.
     *
     * @param is The source of characters being decoded.
     * @return A recursive {@link Token} data structure.
     * @throws IOException If the reader throws an exception at any point.
     */
    public static ContainerToken containerize(final InputStream is) throws IOException {
        return containerize(stream(is));
    }

    /**
     * Generates a recursive {@link ContainerToken} data structure from
     * the given full text.
     *
     * @param text The full reference and source of tokens.
     * @return A recursive {@link Token} data structure.
     */
    public static ContainerToken containerize(final String text) {
        return containerize(stream(text));
    }

    /**
     * Generates a recursive {@link ContainerToken} data structure from
     * an existing {@link TokenStream stream of tokens}.
     *
     * @param stream An existing stream of tokens.
     * @return A recursive {@link Token} data structure.
     */
    public static ContainerToken containerize(final TokenStream stream) {
        if (stream instanceof ContainerToken) {
            return (ContainerToken) stream;
        }
        return container(stream.iterator(), stream, TokenType.OPEN, '\u0000');
    }

    private static ContainerToken container(
            final TokenStream.Itr itr, final Span<?> span, final TokenType type, final char closer) {
        final List<Token> tokens = new ArrayList<>();
        int e = span.start();
        int el = span.lastLine();
        while (itr.hasNext()) {
            final Token control = itr.next();
            if (control.isSymbol(closer)) {
                return new ContainerToken(
                    itr.getReference().toString(), span.start(), control.end(),
                        span.line(), control.lastLine(), span.offset(), type, tokens);
            }
            final Token next = next(itr, control);
            tokens.add(next);
            e = next.end();
            el = next.lastLine();
        }
        if (closer != '\u0000') {
            throw SyntaxException.expected(closer, span.line(), span.offset());
        }
        return new ContainerToken(
            itr.getReference().toString(), span.start(), e, span.line(), el, span.offset(), type, tokens);
    }

    private static Token next(final TokenStream.Itr itr, final Token control) {
        if (control.isSymbol('{')) {
            return container(itr, control, TokenType.BRACES, '}');
        } else if (control.isSymbol('[')) {
            return container(itr, control, TokenType.BRACKETS, ']');
        } else if (control.isSymbol('(')) {
            return container(itr, control, TokenType.PARENTHESES, ')');
        }
        return control;
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
