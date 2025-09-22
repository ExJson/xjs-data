package xjs.data.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;

/**
 * Implementation of {@link Tokenizer} providing tokens in DJS format.
 */
public class DjsTokenizer extends Tokenizer {

    /**
     * Begins parsing tokens from any source.
     *
     * @param reader        A reader providing characters and positional data.
     */
    public DjsTokenizer(final PositionTrackingReader reader) {
        this(reader, false);
    }

    /**
     * Begins parsing tokens from any source.
     *
     * @param reader        A reader providing characters and positional data.
     * @param containerized Whether to generate containers on the fly.
     */
    public DjsTokenizer(final PositionTrackingReader reader, final boolean containerized) {
        super(reader, containerized);
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
            case '-', '+', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> this.number();
            case '/', '#' -> this.comment(c);
            case '\'', '"' -> this.quote(c);
            case '\n' -> this.newLine();
            default -> this.word();
        };
    }
}
