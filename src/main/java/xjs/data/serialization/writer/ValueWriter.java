package xjs.data.serialization.writer;

import xjs.data.JsonValue;

import java.io.File;
import java.io.IOException;

/**
 * The basic writer type to be used for all sorts of JSON formats.
 */
public interface ValueWriter extends AutoCloseable {

    /**
     * The most essential function of all JSON writers, i.e.
     * to <em>write</em> them somewhere.
     *
     * @param value The value being written.
     * @throws IOException If an exception is thrown by the writer.
     */
    void write(final JsonValue value) throws IOException;

    /**
     * The expected constructor to be used when writing values
     * to the disk.
     */
    interface FileConstructor {

        /**
         * Builds a ValueWriter when given a file to write to and some
         * formatting options.
         *
         * @param file    The file being serialized into.
         * @param options The options used when formatting this file.
         * @return A new {@link ValueWriter}.
         * @throws IOException If an error occurs when opening the file.
         */
        ValueWriter construct(final File file, final JsonWriterOptions options) throws IOException;
    }
}
