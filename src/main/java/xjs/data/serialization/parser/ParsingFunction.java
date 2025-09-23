package xjs.data.serialization.parser;

import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.util.PositionTrackingReader;
import xjs.data.serialization.writer.WritingFunction;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents an entire procedure for converting data into
 * {@link JsonValue JSON values}.
 */
@FunctionalInterface
public interface ParsingFunction {

    /**
     * The main function being represented by this interface.
     *
     * @param reader The source of data being parsed.
     * @return The {@link JsonValue} being represented by the file.
     * @throws IOException If the underlying {@link PositionTrackingReader} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     * @see JsonParser#JsonParser(PositionTrackingReader)
     * @see JsonParser#parse()
     */
    JsonValue parse(final PositionTrackingReader reader) throws IOException;

    /**
     * Parse a JSON value for this format from string.
     *
     * @param s The data being parsed.
     * @return The {@link JsonValue} being represented by the file.
     * @throws SyntaxException if the data is syntactically invalid.
     * @see JsonParser#parse()
     */
    default JsonValue parse(final String s) {
        try {
            return this.parse(PositionTrackingReader.fromString(s));
        } catch (IOException e) {
            throw new IllegalStateException("Parser threw unexpected error", e);
        }
    }

    /**
     * Parse a JSON value for this format from Reader.
     *
     * @param reader The source of data being parsed.
     * @return The {@link JsonValue} being represented by the file.
     * @throws IOException If the underlying {@link Reader} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     * @see JsonParser#parse()
     */
    default JsonValue parse(final Reader reader) throws IOException {
        return this.parse(PositionTrackingReader.fromReader(reader));
    }

    /**
     * Parse a JSON value for this format from InputStream.
     *
     * @param is The data being parsed.
     * @return The {@link JsonValue} being represented by the file.
     * @throws IOException If the underlying {@link InputStream} throws an exception.
     * @throws SyntaxException if the data is syntactically invalid.
     * @see JsonParser#parse()
     */
    default JsonValue parse(final InputStream is) throws IOException {
        try (final var reader = PositionTrackingReader.fromIs(is)) {
            return this.parse(reader);
        }
    }

    /**
     * Parse a JSON value for this format from file.
     *
     * @param file The file being parsed.
     * @return The {@link JsonValue} being represented by the file.
     * @throws IOException If the underlying {@link Reader} throws an exception.
     * @throws SyntaxException if the file is syntactically invalid.
     * @see JsonParser#parse()
     */
    default JsonValue parse(final Path file) throws IOException {
        try (final var reader = Files.newBufferedReader(file)) {
            return this.parse(reader);
        }
    }

    /**
     * Builds a ParsingFunction when given a reference to the constructor
     * of any {@link ValueParser}.
     *
     * @param c The constructor used to build a {@link ValueParser}.
     * @return A reusable {@link WritingFunction}.
     */
    static ParsingFunction fromParser(final ValueParser.ReaderConstructor c) {
        return reader -> c.construct(reader).parse();
    }
}