package xjs.data.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of {@link Tokenizer} providing tokens in DJS format.
 */
public class DjsTokenizer extends Tokenizer {

    /**
     * Begins parsing tokens when given a typically ongoing input.
     *
     * @param is            Any source of character bytes.
     * @param containerized Whether to generate containers on the fly.
     * @throws IOException If the reader fails to parse any initial bytes.
     */
    public DjsTokenizer(final InputStream is, final boolean containerized) throws IOException {
        super(is, containerized);
    }

    /**
     * Begins parsing tokens when given a full text as the source.
     *
     * @param text          The full text and source of tokens.
     * @param containerized Whether to generate containers on the fly.
     */
    public DjsTokenizer(final String text, final boolean containerized) {
        super(text, containerized);
    }

    /**
     * Begins parsing tokens from any other source.
     *
     * @param reader        A reader providing characters and positional data.
     * @param containerized Whether to generate containers on the fly.
     */
    public DjsTokenizer(final PositionTrackingReader reader, final boolean containerized) {
        super(reader, containerized);
    }

    /**
     * Generates a lazily-evaluated {@link TokenStream stream of
     * tokens} from the input text.
     *
     * @param text The full reference and source of tokens.
     * @return A new {@link TokenStream}.
     */
    public static TokenStream stream(final String text) {
        return new TokenStream(new DjsTokenizer(text, false), TokenType.OPEN);
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
        return new TokenStream(new DjsTokenizer(is, false), TokenType.OPEN);
    }

    /**
     * Generates a lazily-evaluated {@link TokenStream stream of tokens}
     * wrapping a {@link PositionTrackingReader}.
     *
     * @param reader The source of tokens being parsed.
     * @return A new {@link TokenStream}.
     */
    public static TokenStream stream(final PositionTrackingReader reader) {
        return new TokenStream(new DjsTokenizer(reader, false), TokenType.OPEN);
    }

    /**
     * Generates a recursive {@link TokenStream} data structure from
     * the given full text.
     *
     * @param text The full reference and source of tokens.
     * @return A recursive {@link Token} data structure.
     */
    public static TokenStream containerize(final String text) {
        return new TokenStream(new DjsTokenizer(text, true), TokenType.OPEN);
    }

    /**
     * Generates a recursive {@link TokenStream} data structure from
     * the given source.
     *
     * @param is The source of characters being decoded.
     * @return A recursive {@link Token} data structure.
     * @throws IOException If the reader throws an exception at any point.
     */
    public static TokenStream containerize(final InputStream is) throws IOException {
        return new TokenStream(new DjsTokenizer(is, true), TokenType.OPEN);
    }

    /**
     * Generates a recursive {@link TokenStream} data structure from
     * the given source.
     *
     * @param reader The source of characters being decoded.
     * @return A recursive {@link Token} data structure.
     */
    public static TokenStream containerize(final PositionTrackingReader reader) {
        return new TokenStream(new DjsTokenizer(reader, true), TokenType.OPEN);
    }

    @Override
    protected @Nullable Token single() throws IOException {
        this.reader.skipLineWhitespace();
        if (this.reader.isEndOfText()) {
            return null;
        }
        final char c = (char) this.reader.current;
        this.startReading();
        return switch (c) {
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> this.number();
            case '/', '#' -> this.comment(c);
            case '\'', '"' -> this.quote(c);
            case '\n' -> this.newLine();
            case '.' -> this.dot();
            default -> this.word();
        };
    }
}
