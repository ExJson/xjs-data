package xjs.data.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;

import java.io.File;
import java.io.IOException;

/**
 * The basic writer type to be used for all sorts of JSON formats.
 */
public interface ValueParser extends AutoCloseable {

    /**
     * Reads any type of {@link JsonValue} from the input of this object.
     *
     * @return A definite, non-null {@link JsonValue}.
     * @throws IOException If the reader throws an {@link IOException}.
     * @throws SyntaxException If the data is syntactically invalid.
     */
    @NotNull JsonValue parse() throws IOException;

    /**
     * The expected constructor to be used when reading values
     * from the disk.
     */
    interface FileConstructor {

        /**
         * Builds a ValueParser when given a file to read from.
         *
         * @param file The file being deserialized.
         * @return A new {@link ValueParser}.
         * @throws IOException If an error occurs when opening the file.
         */
        ValueParser construct(final File file) throws IOException;
    }
}
