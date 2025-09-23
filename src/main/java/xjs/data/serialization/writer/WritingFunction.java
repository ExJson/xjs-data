package xjs.data.serialization.writer;

import org.jetbrains.annotations.Nullable;
import xjs.data.JsonValue;
import xjs.data.serialization.JsonContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the entire procedure for serializing {@link JsonValue JSON values}
 * to the disk.
 */
@FunctionalInterface
public interface WritingFunction {

    /**
     * Writes a JSON value with default formatting.
     *
     * @param tw      The writer handling the output.
     * @param value   The value being written into the writer.
     * @throws IOException If an exception is thrown by the writer.
     * @see ElementWriter#ElementWriter(Writer, JsonWriterOptions)
     * @see ElementWriter#write(JsonValue)
     */
    default void write(final Writer tw, final JsonValue value) throws IOException {
        this.write(tw, value, JsonContext.getDefaultFormatting());
    }

    /**
     * The main function being represented by this interface.
     *
     * @param tw      The writer handling the output.
     * @param value   The value being written into the writer.
     * @param options The options used to indicate output formatting.
     * @throws IOException If an exception is thrown by the writer.
     * @see ElementWriter#write(JsonValue)
     */
    void write(final Writer tw, final JsonValue value, final @Nullable JsonWriterOptions options) throws IOException;

    /**
     * Writes a JSON value to the given file in this format.
     *
     * @param file    The output file where the value will be serialized.
     * @param value   The value being written into the writer.
     * @throws IOException If an exception is thrown in writing to the file.
     * @see ElementWriter#write(JsonValue)
     */
    default void write(final Path file, final JsonValue value) throws IOException {
        this.write(file, value, JsonContext.getDefaultFormatting());
    }

    /**
     * Writes a JSON value to the given file in this format.
     *
     * @param file    The output file where the value will be serialized.
     * @param value   The value being written into the writer.
     * @param options The options used to indicate output formatting.
     * @throws IOException If an exception is thrown in writing to the file.
     * @see ElementWriter#write(JsonValue)
     */
    default void write(final Path file, final JsonValue value, final @Nullable JsonWriterOptions options) throws IOException {
        try (final var writer = Files.newBufferedWriter(file)) {
            this.write(writer, value, options);
        }
    }

    /**
     * Writes a JSON value to the given OutputStream in this format.
     *
     * @param os      The stream consuming the output.
     * @param value   The value being written into the writer.
     * @throws IOException If an exception is thrown in writing to the stream.
     * @see ElementWriter#write(JsonValue)
     */
    default void write(final OutputStream os, final JsonValue value) throws IOException {
        this.write(os, value, JsonContext.getDefaultFormatting());
    }

    /**
     * Writes a JSON value to the given OutputStream in this format.
     *
     * @param os      The stream consuming the output.
     * @param value   The value being written into the writer.
     * @param options The options used to indicate output formatting.
     * @throws IOException If an exception is thrown in writing to the stream.
     * @see ElementWriter#write(JsonValue)
     */
    default void write(final OutputStream os, final JsonValue value, final @Nullable JsonWriterOptions options) throws IOException {
        try (final var writer = new OutputStreamWriter(os)) {
            this.write(writer, value, options);
        }
    }

    /**
     * Converts a JSON value to string in this format.
     *
     * @param value   The value being written into the writer.
     * @see ElementWriter#write(JsonValue)
     */
    default String stringify(final JsonValue value) {
        return this.stringify(value, JsonContext.getDefaultFormatting());
    }

    /**
     * Converts a JSON value to string in this format.
     *
     * @param value   The value being written into the writer.
     * @param options The options used to indicate output formatting.
     * @see ElementWriter#write(JsonValue)
     */
    default String stringify(final JsonValue value, final @Nullable JsonWriterOptions options)  {
        final var sw = new StringWriter();
        try {
            this.write(sw, value, options);
        } catch (final IOException e) {
            throw new IllegalStateException("Writer threw unexpected error", e);
        }
        return sw.toString();
    }

    /**
     * Builds a WritingFunction when given a reference to the constructor
     * of any {@link ValueWriter}.
     *
     * @param c The constructor used to build a {@link ValueWriter}.
     * @return A reusable {@link WritingFunction}.
     */
    static WritingFunction fromWriter(final ValueWriter.WriterConstructor c) {
        return (tw, value, options) -> c.construct(tw, options).write(value);
    }
}