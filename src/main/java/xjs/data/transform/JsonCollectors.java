package xjs.data.transform;

import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonContainer;
import xjs.data.JsonObject;
import xjs.data.JsonReference;
import xjs.data.JsonValue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * A series of {@link Collector collectors} used for generating regular
 * {@link JsonContainer JSON containers} from {@link JsonValue JSON values}
 * and other miscellaneous data.
 *
 * <p>For example, to collect a {@link Stream} of integers into a {@link JsonArray}:
 *
 * <pre>{@code
 *   Stream.of(1, 2, 3).collect(JsonCollectors.any())
 * }</pre>
 *
 * <p>Or to convert a regular {@link Map} into a {@link JsonObject};
 *
 * <pre>{@code
 *   Map.of(
 *       Days.MONDAY, 1,
 *       Days.TUESDAY, 2,
 *       Days.WEDNESDAY, 3)
 *     .entrySet()
 *     .stream()
 *     .collect(
 *       JsonCollectors.toObject(
 *         Days::name,
 *         Json::any);
 * }</pre>
 *
 * <p>Or simply rely on automatic conversions:
 *
 * <pre>{@code
 *   Map.of(
 *       "a", 1,
 *       "b", 2,
 *       "c", 3)
 *     .entrySet()
 *     .stream()
 *     .collect(JsonCollectors.toObject());
 * }</pre>
 *
 * <p>Finally, these collectors may be used to transform between type of containers.
 * For example, to collect the values from an object into an array:
 *
 * <pre>{@code}
 *   final JsonArray collected =
 *     object.values()
 *       .stream()
 *       .collect(JsonCollectors.value());
 * }</pre>
 */
public final class JsonCollectors {

    private JsonCollectors() {}

    /**
     * Generates a {@link Collector} collecting {@link JsonValue values} into
     * {@link JsonArray arrays}.
     *
     * <p>For example, to filter the values in an array into a new array:
     *
     * <pre>{@code
     *   final JsonArray collected =
     *    array.stream()
     *      .filter(JsonValue::isNumber)
     *      .collect(Collectors.value());
     * }</pre>
     *
     * @return The collector.
     */
    public static Collector<JsonValue, JsonArray, JsonArray> value() {
        return Collector.of(
            JsonArray::new,
            JsonArray::add,
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Generates a {@link Collector} collecting {@link JsonReference references}
     * into {@link JsonArray arrays}.
     *
     * @return The collector.
     */
    public static Collector<JsonReference, JsonArray, JsonArray> reference() {
        return Collector.of(
            JsonArray::new,
            JsonArray::addReference,
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Generates a {@link Collector} collecting {@link JsonArray.Element elements}
     * into {@link JsonArray arrays}.
     *
     * @return The collector.
     */
    public static Collector<JsonArray.Element, JsonArray, JsonArray> element() {
        return Collector.of(
            JsonArray::new,
            (array, element) -> array.addReference(element.getReference()),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Generates a {@link Collector} collecting <em>any</em> simple values into
     * {@link JsonArray arrays}.
     *
     * @return The collector.
     * @see Json#any(Object)
     */
    public static Collector<Object, JsonArray, JsonArray> any() {
        return Collector.of(
            JsonArray::new,
            (array, any) -> array.add(Json.any(any)),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Collects a series of {@link JsonObject.Member object members} into a new
     * {@link JsonObject}.
     *
     * <p>For example, to filter the members in an object into a new object:
     *
     * <pre>{@code
     *   final JsonObject collected =
     *     object.stream()
     *       .filter(m -> m.visit().isNumber())
     *       .collect(JsonCollectors.member());
     * }</pre>
     *
     * @return The collector.
     */
    public static Collector<JsonObject.Member, JsonObject, JsonObject> member() {
        return toObject(JsonObject.Member::getKey, JsonObject.Member::getOnly);
    }

    /**
     * Generates of {@link Collector} collecting {@link Map.Entry map entries} into
     * {@link JsonObject objects}.
     *
     * <p>The exact behavior of this collector is based on {@link Json#any(Object)}.
     *
     * @return The collector.
     * @see Json#any(Object)
     */
    public static Collector<Map.Entry<?, ?>, JsonObject, JsonObject> toObject() {
        return Collector.of(
            JsonObject::new,
            (object, entry) -> object.add(entry.getKey().toString(), Json.any(entry.getValue())),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Generates of {@link Collector} collecting {@link Map.Entry map entries} into
     * {@link JsonObject objects}.
     *
     * <p>This implementation is driven by the mapper passed into the method. Keys
     * will be converted implicitly by {@link Object#toString}.
     *
     * @param valueMapper A mapper converting values into {@link JsonValue JSON values}.
     * @return The collector.
     * @see Json#any(Object)
     */
    public static <T> Collector<Map.Entry<String, T>, JsonObject, JsonObject> toObject(
            final Function<T, JsonValue> valueMapper) {
        return Collector.of(
            JsonObject::new,
            (object, entry) -> object.add(entry.getKey(), valueMapper.apply(entry.getValue())),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Generates of {@link Collector} collecting {@link Map.Entry map entries} into
     * {@link JsonObject objects}.
     *
     * <p>This implementation allows the caller to specify conversions of both keys
     * <em>and</em> values.
     *
     * @param keyMapper   A mapper converting keys into {@link String strings}.
     * @param valueMapper A mapper converting values into {@link JsonValue JSON values}.
     * @param <T> The type of entry being mapped into an object.
     * @return The collector.
     */
    public static <T> Collector<T, JsonObject, JsonObject> toObject(
            final Function<T, String> keyMapper, final Function<T, JsonValue> valueMapper) {
        return Collector.of(
            JsonObject::new,
            (object, t) -> object.add(keyMapper.apply(t), valueMapper.apply(t)),
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH
        );
    }
}
