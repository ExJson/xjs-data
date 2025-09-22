package xjs.data.serialization.token;

import xjs.data.exception.SyntaxException;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;

/**
 * Represents an entire procedure for generating {@link TokenStream token streams}
 * from arbitrary data.
 */
@FunctionalInterface
public interface TokenizingFunction {

    /**
     * The main function being represented by this interface.
     *
     * <p>Callers <em>must be aware</em> that the stream produces
     * {@link SyntaxException syntax exceptions} <em>and</em> {@link
     * UncheckedIOException unchecked IO exceptions} on the fly.
     *
     * @param reader       The source of data being parsed.
     * @param containerize Whether to generate containers on the fly.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If an error occurs constructing the {@link Tokenizer}.
     */
    TokenStream stream(final PositionTrackingReader reader, final boolean containerize) throws IOException;

    /**
     * Generate a -containerized {@link TokenStream stream} from
     * the given reader.
     *
     * @param reader The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If an error occurs constructing the {@link Tokenizer}.
     */
    default TokenStream stream(final PositionTrackingReader reader) throws IOException {
        return this.stream(reader, false);
    }

    /**
     * Generate a regular, non-containerized {@link TokenStream stream} from
     * the given reader.
     *
     * @param s The data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws SyntaxException if the data is syntactically invalid.
     */
    default TokenStream stream(final String s) {
        try {
            return this.stream(PositionTrackingReader.fromString(s), false);
        } catch (final IOException e) {
            throw new IllegalStateException("Tokenizer threw unexpected error", e);
        }
    }

    /**
     * Generate a regular, non-containerized {@link TokenStream stream} from
     * the given reader.
     *
     * @param reader The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     */
    default TokenStream stream(final Reader reader) throws IOException {
        return this.stream(PositionTrackingReader.fromReader(reader), false);
    }

    /**
     * Generate a regular, non-containerized {@link TokenStream stream} from
     * the given reader.
     *
     * @param is The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     */
    default TokenStream stream(final InputStream is) throws IOException {
        return this.stream(PositionTrackingReader.fromIs(is), false);
    }

    /**
     * Generate a regular, non-containerized {@link TokenStream stream} from
     * the given reader.
     *
     * @param file The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the file is syntactically invalid.
     */
    default TokenStream stream(final File file) throws IOException {
        return this.stream(new FileReader(file));
    }

    /**
     * Generate a containerized {@link TokenStream stream} from the given reader.
     *
     * @param reader The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If an error occurs constructing the {@link Tokenizer}.
     */
    default TokenStream containerize(final PositionTrackingReader reader) throws IOException {
        return this.stream(reader, true);
    }

    /**
     * Generate a containerized {@link TokenStream stream} from the given reader.
     *
     * @param s The data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws SyntaxException if the data is syntactically invalid.
     */
    default TokenStream containerize(final String s) {
        try {
            return this.stream(PositionTrackingReader.fromString(s), true);
        } catch (final IOException e) {
            throw new IllegalStateException("Tokenizer threw unexpected error", e);
        }
    }

    /**
     * Generate a containerized {@link TokenStream stream} from the given reader.
     *
     * @param reader The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     */
    default TokenStream containerize(final Reader reader) throws IOException {
        return this.stream(PositionTrackingReader.fromReader(reader), false);
    }

    /**
     * Generate a containerized {@link TokenStream stream} from the given reader.
     *
     * @param is The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     */
    default TokenStream containerize(final InputStream is) throws IOException {
        return this.stream(PositionTrackingReader.fromIs(is), false);
    }

    /**
     * Generate a containerized {@link TokenStream stream} from the given reader.
     *
     * @param file The source of data being parsed.
     * @return A lazily-evaluated {@link TokenStream stream} of tokens.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the file is syntactically invalid.
     */
    default TokenStream containerize(final File file) throws IOException {
        return this.containerize(new FileReader(file));
    }
    
    /**
     * Builds a WritingFunction when given a reference to the constructor
     * of any {@link Tokenizer}.
     *
     * @param c The constructor used to build a {@link Tokenizer}.
     * @return A reusable {@link TokenizingFunction}.
     */
    static TokenizingFunction fromTokenizer(final Tokenizer.ReaderConstructor c) {
        return (reader, containerize) -> c.construct(reader, containerize).stream();
    }
}
