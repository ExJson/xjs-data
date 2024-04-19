package xjs.data;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import xjs.data.comments.CommentHolder;
import xjs.data.comments.CommentStyle;
import xjs.data.comments.CommentType;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.writer.JsonWriter;
import xjs.data.serialization.writer.JsonWriterOptions;
import xjs.data.serialization.writer.DjsWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a JSON value. This may be a primitive value, such as a
 *
 * <ul>
 *   <li>{@link JsonNumber JSON number}, or a</li>
 *   <li>{@link JsonString JSON string}</li>
 * </ul>
 *
 * <p>or one of the {@link JsonLiteral literals}:
 *
 * <ul>
 *   <li><code>true</code></li>
 *   <li><code>false</code>, or</li>
 *   <li><code>null</code></li>
 * </ul>
 *
 * <p>or even a type of {@link JsonContainer container}, such as a
 *
 * <ul>
 *   <li>{@link JsonArray JSON array}, or a</li>
 *   <li>{@link JsonObject JSON object}</li>
 * </ul>
 *
 * <p>Callers should be aware that this type hierarchy is intentionally
 * open to extenders and foreign implementors. For this reason, callers
 * should avoid using <code>instanceof</code> checks and instead prefer
 * a method such as:
 *
 * <ul>
 *   <li>{@link #isArray}</li>
 *   <li>{@link #isNumber}</li>
 *   <li>etc</li>
 * </ul>
 *
 * <p>Likewise, to <em>cast</em> this value into some other type, callers
 * may use an <code>as</code> method, such as:
 *
 * <ul>
 *   <li>{@link #asArray}</li>
 *   <li>{@link #asDouble}</li>
 *   <li>etc</li>
 * </ul>
 *
 * <p>Finally, callers may optionally <em>convert</em> values between types
 * using the <code>into</code> pattern:
 *
 * <ul>
 *   <li>{@link #intoArray}</li>
 *   <li>{@link #intoDouble}</li>
 *   <li>etc</li>
 * </ul>
 *
 * <p>To get started using the provided type hierarchy for this ecosystem,
 * callers should defer to the factory methods in {@link Json} when possible.
 */
public abstract class JsonValue implements Serializable {

    protected int linesAbove;
    protected int linesBetween;
    protected @Nullable CommentHolder comments;

    protected JsonValue() {
        this.linesAbove = -1;
        this.linesBetween = -1;
        this.comments = null;
    }

    /**
     * Gets the number of newline characters <em>above</em> this value.
     *
     * <p>For example, in the following JSON data:
     *
     * <pre>{@code
     *   {
     *     "a": 1,
     *
     *     "b": 2
     *   }
     * }</pre>
     *
     * <p><code>b</code> has <em>2</em> newlines above it.
     *
     * @return The number of newline characters.
     */
    public int getLinesAbove() {
        return this.linesAbove;
    }

    /**
     * Sets the number of newline characters above this value.
     *
     * @param linesAbove The number of newline characters above the value.
     * @return <code>this</code>, for method chaining.
     * @see #getLinesAbove()
     */
    public JsonValue setLinesAbove(final int linesAbove) {
        this.linesAbove = linesAbove;
        return this;
    }

    /**
     * Gets the number of newline characters between this value and its key, if
     * applicable.
     *
     * <p>For example, the in the following JSON data:
     *
     * <pre>{@code
     *   {
     *     "k":
     *       "v"
     *   }
     * }</pre>
     *
     * <p><code>k</code> has <em>1</em> line between.
     *
     * @return The number of newline characters between this value and its key.
     */
    public int getLinesBetween() {
        return this.linesBetween;
    }

    /**
     * Sets the number of newline characters between this value and its key, if
     * applicable.
     *
     * @param linesBetween The number of newline characters between this value
     *                     and its key.
     * @return <code>this</code>, for method chaining.
     * @see #getLinesBetween()
     */
    public JsonValue setLinesBetween(final int linesBetween) {
        this.linesBetween = linesBetween;
        return this;
    }

    /**
     * Gets a handle on the comments used by this value. This handle can be to
     * append additional comments or view messages inside the comments already
     * configured for this value.
     *
     * @return The {@link CommentHolder} used by this value.
     */
    public CommentHolder getComments() {
        if (this.comments == null) {
            return this.comments = new CommentHolder();
        }
        return this.comments;
    }

    /**
     * Sets the entire handle on any comments appended to this value.
     *
     * @param comments The new comments and their data being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue setComments(final @Nullable CommentHolder comments) {
        this.comments = comments;
        return this;
    }

    /**
     * Indicates whether any comments have been appended to this value.
     *
     * @return <code>true</code>, if there are any comments.
     */
    public boolean hasComments() {
        return this.comments != null && this.comments.hasAny();
    }

    /**
     * Indicates whether a specific type of comment has been appended to this
     * value.
     *
     * @param type The type of comment being queried.
     * @return <code>true</code>, if the given type of comment does exist.
     */
    public boolean hasComment(final CommentType type) {
        return this.comments != null && this.comments.has(type);
    }

    /**
     * Sets the <em>message</em> of the header comment attached to this value.
     *
     * <p>For example, when appending <code>"Header"</code> to this value, it
     * will be printed as follows:
     *
     * <pre>{@code
     *   // Header
     *   key: value
     * }</pre>
     *
     * @param text The message being appended to this value.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue setComment(final String text) {
        return this.setComment(CommentType.HEADER, JsonContext.getDefaultCommentStyle(), text);
    }

    /**
     * Sets the <em>message</em> for the given type of comment attached to this
     * value.
     *
     * @param type The type of comment being set.
     * @param text The message of the comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue setComment(final CommentType type, final String text) {
        return this.setComment(type, JsonContext.getDefaultCommentStyle(), text);
    }

    /**
     * Sets the <em>message</em> for the given type of comment, while also
     * selecting a specific style for the comment.
     *
     * <p>Callers should be aware that the <em>style</em> of this comment may
     * not be valid, depending on the format. In this case, it will simply be
     * overwritten, which may be expensive.
     *
     * @param type  The position of the comment being set.
     * @param style The comment style being appended.
     * @param text  The message of the comment being set.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue setComment(final CommentType type, final CommentStyle style, final String text) {
        this.getComments().set(type, style, text);
        return this;
    }

    /**
     * Sets a comment for this value while appending a number of empty lines at
     * the end.
     *
     * @param type  The position of the comment being set.
     * @param style The comment style being appended.
     * @param text  The message of the comment being set.
     * @param lines The number of new line characters to append.
     * @return <code>this</code>, for method chaining.
     * @see CommentHolder#set(CommentType, CommentStyle, String, int)
     */
    public JsonValue setComment(final CommentType type, final CommentStyle style, final String text, final int lines) {
        this.getComments().set(type, style, text, lines);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>above</em> the existing header
     * comment.
     *
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue prependComment(final String text) {
        this.getComments().prepend(CommentType.HEADER, JsonContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>above</em> the existing comment
     * at this position.
     *
     * @param type The type of comment being inserted.
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue prependComment(final CommentType type, final String text) {
        this.getComments().prepend(type, JsonContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>below</em> the existing header
     * comment.
     *
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue appendComment(final String text) {
        this.getComments().append(CommentType.HEADER, JsonContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Inserts the given message as a comment <em>below</em> the existing comment
     * at this position.
     *
     * @param type The type of comment being inserted.
     * @param text The message to insert as a comment.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue appendComment(final CommentType type, final String text) {
        this.getComments().append(type, JsonContext.getDefaultCommentStyle(), text);
        return this;
    }

    /**
     * Gets the <em>message</em> of the comment for the given position.
     *
     * @param type The position of the comment being queried.
     * @return The message of the comment.
     */
    public String getComment(final CommentType type) {
        return this.getComments().get(type);
    }

    /**
     * Transfers all metadata from the given value into this value, <em>without
     * </em> overwriting any custom settings.
     *
     * @param metadata The value containing metadata to be reused.
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue setDefaultMetadata(final JsonValue metadata) {
        if (this.linesAbove < 0) this.linesAbove = metadata.linesAbove;
        if (this.linesBetween < 0) this.linesBetween = metadata.linesBetween;

        if (this.comments == null) {
            if (metadata.comments != null) {
                this.comments = metadata.comments.copy();
            }
        } else if (metadata.comments != null) {
            this.comments.appendAll(metadata.comments);
        }
        return this;
    }

    /**
     * Gets the type of being represented by this wrapper.
     *
     * @return A {@link JsonType}, e.g. a string, number, etc.
     */
    public abstract JsonType getType();

    /**
     * "Unwraps" this value by returning e.g. a raw {@link Number},
     * {@link Map}, etc.
     *
     * @return The "Java" counterpart to this wrapper.
     */
    public abstract Object unwrap();

    /**
     * Indicates whether this value represents some simple type, such as a
     *
     * <ul>
     *   <li>String</li>
     *   <li>Number</li>
     *   <li>Boolean</li>
     *   <li>null</li>
     * </ul>
     *
     * @return Whether this value represents one of the above types.
     */
    public boolean isPrimitive() {
        return true;
    }

    /**
     * Indicates whether this value represents a number.
     *
     * @return <code>true</code>, if this value is a number.
     */
    public boolean isNumber() {
        return false;
    }

    /**
     * Indicates whether this value represents a boolean value.
     *
     * @return <code>true</code>, if this value is a boolean value.
     */
    public boolean isBoolean() {
        return false;
    }

    /**
     * Indicates whether this value represents the literal <code>true</code>.
     *
     * @return <code>true</code>, if this value is <code>true</code>.
     */
    public boolean isTrue() {
        return false;
    }

    /**
     * Indicates whether this value represents the literal <code>false</code>.
     *
     * @return <code>true</code>, if this value is <code>false</code>.
     */
    public boolean isFalse() {
        return false;
    }

    /**
     * Indicates whether this value represents a string value.
     *
     * @return <code>true</code>, if this value is a string value.
     */
    public boolean isString() {
        return false;
    }

    /**
     * Indicates whether this value is additionally a container of many values,
     * such as an {@link JsonArray array} or {@link JsonObject object}.
     *
     * @return <code>true</code>, if this value is a container.
     */
    public boolean isContainer() {
        return false;
    }

    /**
     * Indicates whether this value <em>represents</em> a {@link JsonObject}.
     *
     * @return <code>true</code>, if this value can be treated as an object.
     */
    public boolean isObject() {
        return false;
    }

    /**
     * Indicates whether this value <em>represents</em> a {@link JsonArray}.
     *
     * @return <code>true</code>, if this value can be treated as an array.
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Indicates whether this value represents the literal <code>null</code>.
     *
     * @return <code>true</code>, if this value is <code>null</code>.
     */
    public boolean isNull() {
        return false;
    }

    /**
     * Returns the long value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public long asLong() {
        throw new UnsupportedOperationException("Not a long: " + this);
    }

    /**
     * Returns the int value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public int asInt() {
        throw new UnsupportedOperationException("Not an int: " + this);
    }

    /**
     * Returns the double value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public double asDouble() {
        throw new UnsupportedOperationException("Not a double: " + this);
    }

    /**
     * Returns the float value being represented by this wrapper.
     *
     * @return The number being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a number.
     */
    public float asFloat() {
        throw new UnsupportedOperationException("Not a float: " + this);
    }

    /**
     * Returns the boolean value being represented by this wrapper.
     *
     * @return The boolean value being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a boolean value.
     */
    public boolean asBoolean() {
        throw new UnsupportedOperationException("Not a boolean: " + this);
    }

    /**
     * Returns the string value being represented by this wrapper.
     *
     * @return The string value being wrapped.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a string.
     */
    public String asString() {
        throw new UnsupportedOperationException("Not a string: " + this);
    }

    /**
     * Gets this value as a {@link JsonContainer container}, if applicable.
     *
     * @return This value as a {@link JsonContainer container}.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a container.
     */
    public JsonContainer asContainer() {
        throw new UnsupportedOperationException("Not a container: " + this);
    }

    /**
     * Gets this value as an {@link JsonObject object}, if applicable.
     *
     * @return This value as an {@link JsonObject object}.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a {@link JsonObject}.
     */
    public JsonObject asObject() {
        throw new UnsupportedOperationException("Not an object: " + this);
    }

    /**
     * Gets this value as an {@link JsonArray array}, if applicable.
     *
     * @return This value as an {@link JsonArray array}.
     * @throws UnsupportedOperationException If this value cannot be treated as
     *                                       a {@link JsonArray}.
     */
    public JsonArray asArray() {
        throw new UnsupportedOperationException("Not an array: " + this);
    }

    /**
     * Coerces this value in to a long, even if it is not a number.
     *
     * @return This value as a number.
     */
    public long intoLong() {
        return (long) this.intoDouble();
    }

    /**
     * Coerces this value in to an int, even if it is not a number.
     *
     * @return This value as a number.
     */
    public int intoInt() {
        return (int) this.intoDouble();
    }

    /**
     * Coerces this value in to a double, even if it is not a number.
     *
     * @return This value as a number.
     */
    public abstract double intoDouble();

    /**
     * Coerces this value in to a float, even if it is not a number.
     *
     * @return This value as a number.
     */
    public float intoFloat() {
        return (float) this.intoDouble();
    }

    /**
     * Coerces this value in to a boolean, even if it is not a boolean value.
     *
     * @return This value as a boolean value.
     */
    public boolean intoBoolean() {
        return this.intoDouble() != 0;
    }

    /**
     * Coerces this value in to a string, even if it is not a string value.
     *
     * @return This value as a string.
     */
    public String intoString() {
        return this.toString();
    }

    /**
     * Coerces this value into a container (usually an array), even if it is
     * not a container.
     *
     * @return This value as a {@link JsonContainer}.
     */
    public JsonContainer intoContainer() {
        return this.intoArray();
    }

    /**
     * Coerces this value into a JSON object, even if it is not an object.
     *
     * @return This value as a {@link JsonObject}.
     */
    public JsonObject intoObject() {
        return new JsonObject().add("value", this);
    }

    /**
     * Coerces this value into a JSON array, even if it is not an array.
     *
     * @return This value as a {@link JsonArray}.
     */
    public JsonArray intoArray() {
        return new JsonArray().add(this);
    }

    /**
     * Generates a shallow copy of this value. What this essentially means is
     * that existing references will be reused, but new containers will be
     * constructed at any point recursively inside this value.
     *
     * @return A shallow copy of this value.
     */
    public JsonValue shallowCopy() {
        return this.copy(JsonCopy.NEW_CONTAINERS);
    }

    /**
     * Generates a deep copy of this value, including all metadata.
     *
     * <p>Note that access tracking will be reset in the output of the method.
     *
     * @return A deep copy of this value.
     */
    public JsonValue deepCopy() {
        return this.copy(JsonCopy.DEEP);
    }

    /**
     * Generates a deep copy of this value, including all metadata.
     *
     * @param tracking Whether to additionally persist access tracking data.
     * @return A deep copy of this value.
     */
    public JsonValue deepCopy(final boolean tracking) {
        return this.copy(tracking ? JsonCopy.DEEP_TRACKING : JsonCopy.DEEP);
    }

    /**
     * Generates a deep copy of this value without any formatting options.
     *
     * <p>Note that access tracking will be reset in the output of the method.
     *
     * @return A deep, unformatted copy of this value.
     */
    public JsonValue unformatted() {
        return this.copy(JsonCopy.UNFORMATTED);
    }

    /**
     * Trims any whitespace above or below this value.
     *
     * <p>This method is ideal when adding formatted values into an already-
     * formatted container. For example, to add a parsed value into a parsed
     * container:
     *
     * <pre>{@code
     *   object.add("key", Json.parse("1234").trim());
     * }</pre>
     *
     * <p>This convention avoids disrupting the established-formatting in an
     * existing container.
     *
     * @return <code>this</code>, for method chaining.
     */
    public JsonValue trim() {
        return this.setLinesAbove(-1).setLinesBetween(-1);
    }

    /**
     * Generates a copy of this value given a series of copy options.
     *
     * @param options Any {@link JsonCopy} flags for which data to copy.
     * @return A new instance of this value with similar or identical data.
     */
    public abstract JsonValue copy(final @MagicConstant(flagsFromClass = JsonCopy.class) int options);

    /**
     * Copies the common metadata from a source value into its clone.
     *
     * @param copy    The value being copied into.
     * @param source  The value being copied out of.
     * @param options Any {@link JsonCopy} options from {@link #copy(int)}.
     * @param <V>     The type of value being copied.
     * @return <code>copy</code>
     */
    protected static <V extends JsonValue> V withMetadata(final V copy, final V source, final int options) {
        if ((options & JsonCopy.COMMENTS) == JsonCopy.COMMENTS) {
            if (source.comments != null) copy.comments = source.comments.copy();
        }
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.linesAbove = source.linesAbove;
            copy.linesBetween = source.linesBetween;
        }
        return copy;
    }

    /**
     * Generates a hash code accounting for the value being wrapped by this
     * object. This ignores any metadata associated with the value.
     *
     * @return An integer which should always change if the value is updated.
     */
    public int valueHashCode() {
        return this.unwrap().hashCode();
    }

    /**
     * Generates a hash code accounting for the metadata of the value being
     * wrapped by this object. This ignores the value itself.
     *
     * @return An integer which should always change if the metadata are updated.
     */
    public int metaHashCode() {
        int result = 1;
        result = 31 * result + this.linesAbove;
        result = 31 * result + this.linesBetween;

        if (this.comments != null) {
            result = 31 * result + this.comments.hashCode();
        }
        return result;
    }

    @Override
    public int hashCode() {
        return 31 * this.metaHashCode() + this.valueHashCode();
    }

    /**
     * Indicates whether the given data wraps the same <em>value</em>, thus
     * ignoring its metadata.
     *
     * @param other The value being compared to.
     * @return <code>true</code>, if the two values contain the same primary data.
     */
    public boolean matches(final JsonValue other) {
        return Objects.equals(this.unwrap(), other.unwrap());
    }

    /**
     * Indicates whether the metadata associated with this value matches that
     * of the input.
     *
     * @param other The value being compared to.
     * @return <code>true</code>, if the values contain the same metadata.
     * @apiNote The behavior of this method is unlikely to change, but the
     *          exact implementation <em>might</em>. Implementors should be
     *          aware that any exact methods required could potentially be
     *          impacted by such a change.
     */
    protected boolean matchesMetadata(final JsonValue other) {
        return this.linesAbove == other.linesAbove
            && this.linesBetween == other.linesBetween
            && Objects.equals(this.comments, other.comments);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (this.getClass().equals(o.getClass())) {
            final JsonValue other = (JsonValue) o;
            return this.matches(other) && this.matchesMetadata(other);
        }
        return false;
    }

    /**
     * Writes this value to the disk, selecting which format to use based on
     * the extension of this file.
     *
     * @param file The file being written into.
     * @throws IOException If the involved {@link FileWriter} throws an exception.
     */
    public void write(final File file) throws IOException {
        JsonContext.autoWrite(file, this);
    }

    /**
     * Writes this value into the given writer.
     *
     * @param writer The writer being written into.
     * @throws IOException If this writer throws an exception.
     */
    public void write(final Writer writer) throws IOException {
        new DjsWriter(writer, JsonContext.getDefaultFormatting()).write(this);
    }

    /**
     * Converts this value into a string. For most values, this means
     * printing it as a regular, unformatted. JSON string.
     *
     * @return This value in string format.
     */
    @Override
    public String toString() {
        if (this.isPrimitive()) {
            return this.unwrap().toString();
        }
        return this.toString(JsonFormat.JSON);
    }

    /**
     * Converts this value into a string in the given format.
     *
     * @param format The expected format in which to write this value.
     * @return This value as a JSON string, formatted or otherwise.
     */
    public String toString(final JsonFormat format) {
        final StringWriter sw = new StringWriter();
        try {
            switch (format) {
                case JSON -> new JsonWriter(sw, false).write(this);
                case JSON_FORMATTED -> new JsonWriter(sw, true).write(this);
                case DJS -> new DjsWriter(sw, false).write(this);
                case DJS_FORMATTED -> new DjsWriter(sw, true).write(this);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Encoding error", e);
        }
        return sw.toString();
    }

    /**
     * Converts this value to a formatted DJS string using the given options.
     *
     * @param options Formatting options indicating how to output this value.
     * @return This value as a formatted DJS string.
     */
    public String toString(final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        try {
            new DjsWriter(sw, options).write(this);
        } catch (final IOException e) {
            throw new UncheckedIOException("Encoding error", e);
        }
        return sw.toString();
    }
}
