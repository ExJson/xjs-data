package xjs.data.serialization.writer;

import xjs.data.JsonValue;

import java.io.File;
import java.io.IOException;

/**
 * Represents the entire procedure for serializing {@link JsonValue JSON values}
 * to the disk.
 */
@FunctionalInterface
public interface WritingFunction {

    /**
     * The main function being represented by this interface.
     *
     * @param file    The output file where the value will be serialized.
     * @param value   The value being written into the writer.
     * @param options The options used to indicate output formatting.
     * @throws IOException If an exception is thrown in writing to the file.
     * @see ElementWriter#ElementWriter(File, JsonWriterOptions)
     * @see ElementWriter#write(JsonValue)
     */
    void write(final File file, final JsonValue value, final JsonWriterOptions options) throws IOException;

    /**
     * Builds a WritingFunction when given a reference to the constructor
     * of any {@link ValueWriter}.
     *
     * @param c The constructor used to build a {@link ValueWriter}.
     * @return A reusable {@link WritingFunction}.
     */
    static WritingFunction fromWriter(final ValueWriter.FileConstructor c) {
        return (file, value, options) -> {
            final ValueWriter writer = c.construct(file, options);
            writer.write(value);

            try {
                writer.close();
            } catch (final Exception e) {
                throw new IOException(e);
            }
        };
    }
}