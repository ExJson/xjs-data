package xjs.data;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.JsonContext;
import xjs.data.transform.JsonCollectors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Core utility methods for starting common, critical JSON workflows in this API.
 *
 * <p>For example, to create a new JSON value:
 *
 * <pre>{@code
 *   final JsonNumber value = Json.value(1234);
 * }</pre>
 *
 * <p>Or an array of values:
 *
 * <pre>{@code
 *   final JsonArray array = Json.array(1, 2, 3, 4);
 * }</pre>
 *
 * <p>To parse JSON, DJS, or JSON-C data from string:
 *
 * <pre>{@code
 *   final JsonObject object = Json.parse("k:v,a:[x,y,z]").asObject();
 * }</pre>
 *
 * <p>To generate an unformatted JSON string from a map of regular data:
 *
 * <pre>{@code
 *   final Map<String, Object> map = Map.of(
 *     "a", 1,
 *     "b", List.of(2, 3, 4),
 *     "c", Map.of("d", "e", "f", "g");
 *   );
 *   final String JSON = Json.object(map).toString();
 * }</pre>
 *
 * <p>To view a JSON file (any format) and update it in place:
 *
 * <pre>{@code
 *   Json.view(new File("data.djs", json ->
 *       json.setComment("Value updated in place!"));
 * }</pre>
 *
 * <p>Alternatively, use {@link JsonCollectors} to transform data directly into a
 * {@link JsonValue}:
 *
 * <pre>{@code
 *   final JsonArray array = Stream.of(1, 2, 3).collect(JsonCollectors.any());
 * }</pre>
 */
public final class Json {

    private Json() {}

    /**
     * Constructs a new {@link JsonNumber} for the given value.
     *
     * @param value The number being wrapped as a {@link JsonValue}.
     * @return A new {@link JsonNumber} wrapping the given value.
     */
    public static JsonNumber value(final long value) {
        return new JsonNumber(value);
    }

    /**
     * Constructs a new {@link JsonNumber} for the given value.
     *
     * @param value The number being wrapped as a {@link JsonValue}.
     * @return A new {@link JsonNumber} wrapping the given value.
     */
    public static JsonNumber value(final double value) {
        return new JsonNumber(value);
    }

    /**
     * Constructs a new {@link JsonLiteral} for the given value.
     *
     * @param value The boolean being wrapped as a {@link JsonValue}.
     * @return A new {@link JsonLiteral} wrapping the given value.
     */
    public static JsonLiteral value(final boolean value) {
        return value ? JsonLiteral.jsonTrue() : JsonLiteral.jsonFalse();
    }

    /**
     * Constructs a new {@link JsonString} for the given value.
     *
     * @param value The string being wrapped as a {@link JsonValue}.
     * @return A new {@link JsonString} wrapping the given value, or else {@link JsonLiteral#jsonNull}.
     */
    public static JsonValue value(final @Nullable String value) {
        return value != null ? new JsonString(value) : JsonLiteral.jsonNull();
    }

    /**
     * Wraps the given value as {@link JsonLiteral#jsonNull} if null, or else leaves it unwrapped.
     *
     * @param value The JSON value being wrapped.
     * @return {@link JsonLiteral#jsonNull} if null, or else the input value.
     */
    public static JsonValue nonnull(final @Nullable JsonValue value) {
        return value != null ? value : JsonLiteral.jsonNull();
    }

    /**
     * Constructs a new {@link JsonArray} with no contents.
     *
     * @return An empty {@link JsonArray}.
     */
    public static JsonArray array() {
        return new JsonArray();
    }

    /**
     * Constructs a new {@link JsonArray} with the given contents.
     *
     * <p>Note that the values in this collection may any primitive wrapper, enum, basic Java
     * {@link Collection} type, or a {@link Map}.
     *
     * @param collection A collection containing the values being copied into a {@link JsonArray}.
     * @return A new {@link JsonArray} containing wrapped variants of the same values.
     */
    public static JsonArray array(final Collection<?> collection) {
        return collection.stream().collect(JsonCollectors.any());
    }

    /**
     * Constructs a new {@link JsonArray} containing each of the given integers.
     *
     * @param values An array of integer values to be collected into a {@link JsonArray}.
     * @return A new {@link JsonArray} containing wrapped variants of the same values.
     */
    public static JsonArray array(final long... values) {
        return LongStream.of(values).mapToObj(Json::value).collect(JsonCollectors.value());
    }

    /**
     * Constructs a new {@link JsonArray} containing each of the given decimals.
     *
     * @param values An array of decimal values to be collected into a {@link JsonArray}.
     * @return A new {@link JsonArray} containing wrapped variants of the same values.
     */
    public static JsonArray array(final double... values) {
        return DoubleStream.of(values).mapToObj(Json::value).collect(JsonCollectors.value());
    }

    /**
     * Constructs a new {@link JsonArray} containing each of the given strings.
     *
     * @param values An array of string values to be collected into a {@link JsonArray}.
     * @return A new {@link JsonArray} containing wrapped variants of the same values.
     */
    public static JsonArray array(final String... values) {
        return Stream.of(values).map(Json::value).collect(JsonCollectors.value());
    }

    /**
     * Constructs a new {@link JsonObject} with no contents.
     *
     * @return An empty {@link JsonObject}.
     */
    public static JsonObject object() {
        return new JsonObject();
    }

    /**
     * Constructs a new {@link JsonObject} with the given contents.
     *
     * <p>Note that the values in this map may any primitive wrapper, enum, basic Java
     * {@link Collection} type, or a {@link Map}.
     *
     * <p>Also note that, while any type is allowed as the key in the input, these keys
     * will be implicitly converted into string values via {@link Object#toString}.
     *
     * @param map A map containing the values being copied into a {@link JsonObject}.
     * @return A new {@link JsonArray} containing wrapped variants of the same values.
     */
    public static JsonObject object(final Map<?, ?> map) {
        return map.entrySet().stream().collect(JsonCollectors.toObject());
    }

    /**
     * Constructs a new {@link JsonValue} from the input value of an unknown type.
     *
     * <p>Supported types for this method include
     * <ul>
     *   <li>Primitive wrappers</li>
     *   <li>Arrays</li>
     *   <li>Enums (Via {@link Enum#name()})</li>
     *   <li>Collections</li>
     *   <li>Maps</li>
     * </ul>
     *
     * <p>Additionally, if the given value is already a {@link JsonValue}, it will simply
     * be returned directly.
     *
     * <p>In the future, it is possible that the implementation of this method will be
     * expanded upon very slightly. However, callers should always assume that supported
     * inputs will be limited to primitive wrappers and other <em>essential Java types</em>.
     *
     * @param unknown Any basic Java type (or enum constant) which may correspond directly to
     *                some type of JSON value.
     * @return A {@link JsonValue} matching the above conditions.
     * @throws UnsupportedOperationException If the type does not directly translate to JSON.
     */
    public static JsonValue any(final @Nullable Object unknown) {
        if (unknown == null) return JsonLiteral.jsonNull();
        if (unknown instanceof JsonValue) return (JsonValue) unknown;
        if (unknown instanceof JsonReference) return ((JsonReference) unknown).get();
        if (unknown instanceof Number) return value(((Number) unknown).doubleValue());
        if (unknown instanceof Boolean) return value((boolean) unknown);
        if (unknown instanceof String) return value((String) unknown);
        if (unknown instanceof Enum) return value(((Enum<?>) unknown).name());
        if (unknown instanceof Collection) return array((Collection<?>) unknown);
        if (unknown instanceof Map) return object((Map<?, ?>) unknown);
        if (unknown.getClass().isArray()) return arrayPointer(unknown);
        throw new UnsupportedOperationException("Unsupported type: " + unknown.getClass());
    }

    /**
     * Variant of {@link #any(Object)} which implicitly delegates to the original when given
     * an array of unknown values.
     *
     * @param unknowns An array of basic Java types which may correspond to JSON values.
     * @return A new {@link JsonArray} matching the conditions of {@link #any(Object)}.
     */
    public static JsonArray any(final Object... unknowns) {
        return Stream.of(unknowns).collect(JsonCollectors.any());
    }

    /**
     * Converts a standard Java array into a {@link JsonArray}.
     *
     * @param unknown An unknown type of array (e.g. int[], String[], etc.).
     * @return A new {@link JsonArray} wrapping these values.
     */
    private static JsonArray arrayPointer(final Object unknown) {
        final JsonArray array = new JsonArray();
        final int length = Array.getLength(unknown);
        for (int i = 0; i < length; i++) {
            array.add(any(Array.get(unknown, i)));
        }
        return array;
    }

    /**
     * Parses a file automatically based on its extension. Aliases and additional parsers can
     * be registered via the {@link JsonContext}.
     *
     * @param file A file containing JSON data.
     * @return A new {@link JsonValue} representing the contents of this file.
     * @throws IOException If the file cannot be read.
     * @throws SyntaxException If the file is syntactically invalid.
     */
    public static JsonValue parse(final File file) throws IOException {
        return JsonContext.autoParse(file);
    }

    /**
     * Parses a string of JSON, DJS, or JSONC text, returning a new {@link JsonValue} to
     * represent it.
     *
     * @param djs The raw string contents in JSON, DJS, or JSON-C format.
     * @return A new {@link JsonValue} representing the input.
     */
    public static JsonValue parse(final String djs) {
        return JsonContext.getDefaultParser().parse(djs);
    }
    /**
     * Parses a string of JSON, DJS, or JSONC text, returning a new {@link JsonValue} to
     * represent it.
     *
     * @param format The expected format, e.g. <code>json</code> or <code>djs</code>.
     * @param djs    The raw string contents in JSON, DJS, or JSON-C format.
     * @return A new {@link JsonValue} representing the input.
     */
    public static JsonValue parse(final String format, final String djs) {
        return JsonContext.getDefaultParser().parse(djs);
    }


    /**
     * Parses a stream of JSON, DJS, or JSONC text as bytes, returning a new value to
     * represent it.
     *
     * @param djs A reader providing the raw contents  in JSON, DJS, or JSON-C format.
     * @return A new {@link JsonValue} representing the input.
     * @throws IOException If any error occurs in reading from the stream.
     */
    public static JsonValue parse(final InputStream djs) throws IOException {
        return JsonContext.getDefaultParser().parse(djs);
    }

    /**
     * Exposes the contents of a JSON file to the given consumer. The file's extension will
     * determine which parser gets used. Note that aliases and additional parsers can be
     * registered via the {@link JsonContext}.
     *
     * <p>For example, to update a field in the given file:
     *
     * <pre>{@code
     *   Json.view(new File("data.djs"), json ->
     *       json.asObject().set("key", "value"));
     * }</pre>
     *
     * @param file A file containing JSON data.
     * @param f    A consumer manipulating the contents of this file.
     * @throws IOException If an IO error occurs when reading or writing the file.
     * @throws SyntaxException If the contents of the file are syntactically invalid.
     */
    public static void view(final File file, final Consumer<JsonValue> f) throws IOException {
        update(file, value -> { f.accept(value); return value; });
    }

    /**
     * Variant of {@link Json#view} which writes the return value of the given operator.
     *
     * <p>For example, to update a field in the given file, converting it <em>into</em>
     * an {@link JsonObject object} if necessary:
     *
     * <pre>{@code
     *   Json.update(new File("data.djs"), json ->
     *     json.intoObject().set("key", "value"));
     * }</pre>
     *
     * @param file A file containing JSON data.
     * @param f    A consumer transforming the contents of this file.
     * @throws IOException If an IO error occurs when reading or writing the file.
     * @throws SyntaxException If the contents of the file are syntactically invalid.
     */
    public static void update(final File file, final UnaryOperator<JsonValue> f) throws IOException {
        f.apply(parse(file)).write(file);
    }

    /**
     * Generates a copy of the given value without requiring an explicit cast at the
     * source. This operation is considered safe for any values correctly following
     * the contract of {@link JsonValue#copy(int)}.
     *
     * @param value   The value being copied.
     * @param options Any {@link JsonCopy} flags for which data to copy.
     * @param <V>     The type of {@link JsonValue value} being copied.
     * @return A copy of <code>value</code> with similar or identical properties.
     */
    @SuppressWarnings("unchecked")
    public static <V extends JsonValue> V copy(
            final V value, final @MagicConstant(flagsFromClass = JsonCopy.class) int options) {
        return (V) value.copy(options);
    }
}
