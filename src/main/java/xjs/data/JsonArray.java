package xjs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.data.transform.JsonCollectors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link JsonContainer} providing updaters by <em>index</em>.
 *
 * <p>For example, to insert additional values into this container, callers may
 * use {@link JsonArray#add} or one of its overloads.
 *
 * <pre>{@code
 *   Json.array().add("Hello, World!");
 * }</pre>
 *
 * <p>Alternatively, to update an <em>existing</em> value, callers may use
 * {@link JsonArray#set} or one of its overloads.
 *
 * <pre>{@code
 *   JsonArray().add(false).set(0, true);
 * }</pre>
 *
 * <p>The default {@link JsonContainer.View view} of this container is a regular,
 * {@link JsonReference#get accessing} view operating directly on the underlying
 * {@link JsonValue values}. This means that the values in this container can be
 * iterated or streamed to allow transformations via the {@link Stream} API.
 *
 * <p>For example, to filter the contents of this array into an array containing
 * exclusively {@link JsonNumber numbers}:
 *
 * <pre>{@code
 *   final JsonArray array = Json.array().add(1).add(2).add("3").add(4);
 *   final JsonArray numbers = array.stream()
 *     .filter(JsonValue::isNumber).collect(JsonCollectors.value());
 *
 *   assert Json.array(1, 2, 4).equals(numbers);
 * }</pre>
 *
 * <p>However, this does imply that streaming or iterating over the values will flag
 * their references as <b>being used</b>. In the previous example, the filtered value,
 * <code>Json.value("3")</code>, will be flagged as needed by the application. This is
 * significant for any application reporting unused values to the user.
 *
 * <p>To avoid this problem, users may call {@link JsonContainer#visitAll} and iterate
 * over the return value instead.
 *
 * <pre>{@code
 *   final JsonArray array = Json.array().add(1).add(2).add("3").add(4);
 *
 *   final JsonArray numbers = array.visitAll().stream()
 *     .filter(JsonValue::isNumber).collect(JsonCollectors.value());
 *
 *   assert !array.getReference(2).isAccessed();
 * }</pre>
 */
public class JsonArray extends JsonContainer implements JsonContainer.View<JsonValue> {

    private ElementView elements;

    /**
     * Constructs a new JSON array containing no contents.
     */
    public JsonArray() {}

    /**
     * Constructs a new JSON array containing the given references.
     *
     * @param references A list of existing {@link JsonReference references}.
     */
    public JsonArray(final List<JsonReference> references) {
        super(references);
    }

    /**
     * Variant of {@link #set(int, JsonValue)} wrapping an integer value.
     *
     * @param index The index of the <b>existing</b> value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray set(final int index, final long value) {
        return this.set(index, Json.value(value));
    }

    /**
     * Variant of {@link #set(int, JsonValue)} wrapping a decimal value.
     *
     * @param index The index of the <b>existing</b> value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray set(final int index, final double value) {
        return this.set(index, Json.value(value));
    }

    /**
     * Variant of {@link #set(int, JsonValue)} wrapping a boolean value.
     *
     * @param index The index of the <b>existing</b> value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray set(final int index, final boolean value) {
        return this.set(index, Json.value(value));
    }

    /**
     * Variant of {@link #set(int, JsonValue)} wrapping a string value.
     *
     * @param index The index of the <b>existing</b> value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray set(final int index, final String value) {
        return this.set(index, Json.value(value));
    }

    /**
     * Replaces a value at the given index.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index The index of the <b>existing</b> value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray set(final int index, final @Nullable JsonValue value) {
        this.references.get(index).apply(og ->
            Json.nonnull(value).setDefaultMetadata(og));
        return this;
    }

    /**
     * Replaces a reference at the given index.
     *
     * @param index The index of the <b>existing</b> value being replaced.
     * @param reference The reference being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray setReference(final int index, final JsonReference reference) {
        this.references.set(index, reference);
        return this;
    }

    /**
     * Sets the header comment for the value at the given index.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index   The index of the <b>existing</b> value being updated.
     * @param comment The message of the generated comment.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonArray setComment(final int index, final String comment) {
        this.get(index).setComment(comment);
        return this;
    }

    /**
     * Variant of {@link #add(JsonValue)} wrapping an integer value.
     *
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final long value) {
        return this.add(Json.value(value));
    }

    /**
     * Variant of {@link #add(JsonValue)} wrapping a decimal value.
     *
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final double value) {
        return this.add(Json.value(value));
    }

    /**
     * Variant of {@link #add(JsonValue)} wrapping a boolean value.
     *
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final boolean value) {
        return this.add(Json.value(value));
    }

    /**
     * Variant of {@link #add(JsonValue)} wrapping a string value.
     *
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final String value) {
        return this.add(Json.value(value));
    }

    /**
     * Adds a {@link JsonValue} into this container.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final JsonValue value) {
        return this.addReference(new JsonReference(value));
    }

    /**
     * Variant of {@link #add(JsonValue, String)} wrapping an integer value.
     *
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final long value, final String comment) {
        return this.add(Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(JsonValue, String)} wrapping a decimal value.
     *
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final double value, final String comment) {
        return this.add(Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(JsonValue, String)} wrapping a boolean value.
     *
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final boolean value, final String comment) {
        return this.add(Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(JsonValue, String)} wrapping a string value.
     *
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final String value, final String comment) {
        return this.add(Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(JsonValue)} which appends a header comment to the
     * given value.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray add(final JsonValue value, final String comment) {
        return this.add(Json.nonnull(value).setComment(comment));
    }

    /**
     * Adds each {@link JsonReference reference} from the given container into this
     * array.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param container Some other container housing references to be copied.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray addAll(final JsonContainer container) {
        container.references.forEach(this::addReference);
        return this;
    }

    /**
     * Adds a single {@link JsonReference reference} directly into this container.
     *
     * @param reference An existing reference being copied into this container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonArray addReference(final JsonReference reference) {
        this.references.add(reference);
        return this;
    }

    /**
     * Adds a single {@link JsonValue value} directly into this container when
     * given a specific index.
     *
     * @param index The index at which to insert the given reference.
     * @param value An existing value being copied into this container.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    public JsonArray insert(final int index, final JsonValue value) {
        return this.insertReference(index, new JsonReference(value).setAccessed(true));
    }

    /**
     * Adds a single {@link JsonReference reference} directly into this container
     * when given a specific index for the value.
     *
     * @param index     The index at which to insert the given reference.
     * @param reference An existing reference being copied into this container.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    public JsonArray insertReference(final int index, final JsonReference reference) {
        this.references.add(index, reference);
        return this;
    }

    /**
     * Converts this array into a regular {@link List}, containing its
     * {@link JsonValue#unwrap unwrapped} contents.
     *
     * @return A new {@link List} containing the unwrapped values.
     */
    public List<Object> toList() {
        return this.stream().map(JsonValue::unwrap).collect(Collectors.toList());
    }

    /**
     * Returns a {@link View} of the elements in this array. The {@link Element
     * View implementation} for {@link JsonArray} exposes the element's index
     * in addition to the reference and value being wrapped.
     *
     * @return An {@link ElementView element view} of the contents in this array.
     */
    public View<Element> elements() {
        if (this.elements == null) {
            return this.elements = new ElementView();
        }
        return this.elements;
    }

    /**
     * Clears every {@link JsonReference reference} from the underlying array.
     *
     * @return <code>this</code>, for method chaining.
     */
    @Override
    public JsonArray clear() {
        return (JsonArray) super.clear();
    }

    /**
     * Removes a single value from this container by value.
     *
     * @param value The value being purged from this container.
     * @return <code>this</code>, for method chaining.
     */
    @Override
    public JsonArray remove(final JsonValue value) {
        return (JsonArray) super.remove(value);
    }

    /**
     * Removes a single value from this container by index.
     *
     * @param index The value being purged from this container.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException If the given index is not in this array.
     */
    public JsonArray remove(final int index) {
        this.references.remove(index);
        return this;
    }

    /**
     * Removes a collection or array of values from this container by equality.
     *
     * @param values The values being purged from this container.
     * @return <code>this</code>, for method chaining.
     */
    @Override
    public JsonArray removeAll(final Iterable<JsonValue> values) {
        return (JsonArray) super.removeAll(values);
    }

    @Override
    public JsonArray copy(final int options) {
        final JsonArray copy = new JsonArray(this.copyReferences(options));
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.setLinesTrailing(this.linesTrailing);
        }
        return withMetadata(copy, this, options);
    }

    /**
     * Override indicating that this value is an array type.
     *
     * @return {@link JsonType#ARRAY}, always.
     */
    @Override
    public final JsonType getType() {
        return JsonType.ARRAY;
    }

    /**
     * Override returning a regular {@link List} representing this array.
     *
     * @return A new {@link List} containing recursively unwrapped values.
     */
    @Override
    public List<Object> unwrap() {
        return this.toList();
    }

    /**
     * Override indicating that this value is an array type.
     *
     * @return <code>true</code>, always.
     */
    @Override
    public final boolean isArray() {
        return true;
    }

    /**
     * Override allowing this value to be cast into a {@link JsonArray}.
     *
     * @return <code>this</code>, always.
     */
    @Override
    public JsonArray asArray() {
        return this;
    }

    /**
     * Override returning this value directly instead of constructing a new container.
     *
     * @return <code>this</code>, always.
     */
    @Override
    public JsonArray intoArray() {
        return this;
    }

    /**
     * Generates a regular, {@link JsonReference#get accessing} iterator over the values
     * in this array.
     *
     * @return A new accessing value iterator for the values in this container.
     */
    @Override
    public Iterator<JsonValue> iterator() {
        return this.values().iterator();
    }

    @Override
    public int valueHashCode() {
        int result = 1;
        for (final JsonReference reference : this.references) {
            result = 31 * reference.getOnly().valueHashCode();
        }
        return result;
    }

    @Override
    public int hashCode() {
        return 31 * this.metaHashCode() + this.references.hashCode();
    }

    @Override
    public boolean matches(final JsonValue other) {
        if (!(other instanceof JsonArray)) {
            return false;
        }
        final JsonArray array = (JsonArray) other;
        if (this.size() != array.size()) {
            return false;
        }
        for (int i = 0; i < this.size(); i++) {
            if (!this.references.get(i).getOnly().matches(array.references.get(i).getOnly())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof JsonArray) {
            return this.references.equals(((JsonArray) o).references);
        }
        return false;
    }

    @Override
    public JsonArray freeze(final boolean recursive) {
        return new JsonArray(this.freezeReferences(recursive));
    }

    @Override
    public List<String> getPaths(final PathFilter filter) {
        final List<String> paths = new ArrayList<>();
        for (final Element element : this.elements()) {
            if (!filter.test(element.getReference())) {
                continue;
            }
            final String key = "[" + element.getIndex() + "]";
            paths.add(key);
            if (!element.getOnly().isContainer()) {
                continue;
            }
            final String prefix = element.getOnly().isObject() ? key + "." : key;
            for (final String inner : element.getOnly().asContainer().getPaths(filter)) {
                paths.add(prefix + inner);
            }
        }
        return paths;
    }

    private class ElementView implements View<Element> {
        @Override
        public @NotNull ElementIterator iterator() {
            return new ElementIterator();
        }
    }

    private class ElementIterator implements Iterator<Element> {
        final Iterator<JsonReference> references =
            JsonArray.this.references.iterator();
        int index = 0;

        @Override
        public boolean hasNext() {
            return this.references.hasNext();
        }

        @Override
        public Element next() {
            final JsonReference reference = this.references.next();
            return new Element(index++, reference);
        }

        @Override
        public void remove() {
            this.references.remove();
        }
    }

    /**
     * This object represents a handle on a single value in this container, as well as
     * its accessor--e.g. its index or key--if applicable. It may be used to reflect on
     * the contents of a container and, additionally, can be {@link Collector collected}
     * in to a new container via {@link JsonCollectors}.
     */
    public static class Element {
        protected JsonReference reference;
        protected int index;

        /**
         * Constructs a new container element by providing its only essential data piece:
         * the {@link JsonReference}.
         *
         * @param reference The reference being wrapped by this accessor.
         */
        public Element(final JsonReference reference) {
            this(-1, reference);
        }

        /**
         * Constructs a new container element from a reference and its index.
         *
         * @param index     The index of the value, or else -1
         * @param reference A reference pointing to the value at this location.
         */
        public Element(final int index, final JsonReference reference) {
            this.reference = reference;
            this.index = index;
        }

        /**
         * Returns the value being wrapped by this Element.
         *
         * <p>This is an {@link JsonReference#get accessing} operation.
         *
         * @return The value being wrapped.
         */
        public @NotNull JsonValue getValue() {
            return this.reference.get();
        }

        /**
         * Updates the value being wrapped by this accessor. This operation will cascade and
         * consequently replace the original value in the parent container.
         *
         * <p>This is an {@link JsonReference#get accessing} operation.
         *
         * @param value The new value to be wrapped.
         * @return <code>this</code>, for method chaining.
         */
        public Element setValue(final @Nullable JsonValue value) {
            this.reference.set(value);
            return this;
        }

        /**
         * Returns the value being wrapped by this element <em>without</em> updating its
         * access flags.
         *
         * <p>This is a {@link JsonReference#getOnly visiting} operation.
         *
         * @return The value being wrapped.
         */
        public @NotNull JsonValue getOnly() {
            return this.reference.getOnly();
        }

        /**
         * Updates the value being wrapped by this element <em>without</em> updating its
         * access flags.
         *
         * <p>This is a {@link JsonReference#getOnly visiting} operation.
         *
         * @param value The new value to be wrapped.
         * @return <code>this</code>, for method chaining.
         */
        public Element setOnly(final @Nullable JsonValue value) {
            this.reference.setOnly(value);
            return this;
        }

        /**
         * Directly exposes the reference to the value being wrapped by this element.
         *
         * @return A reference to the wrapped value.
         */
        public JsonReference getReference() {
            return this.reference;
        }

        /**
         * Exposes this element's index to the caller.
         *
         * @return The index of the original value.
         */
        public int getIndex() {
            return this.index;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + this.index;
            result = 31 * result + this.reference.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Element) {
                final Element other = (Element) o;
                return this.index == other.index && this.reference.equals(other.reference);
            }
            return false;
        }

        @Override
        public String toString() {
            return "([" + this.index + "]=" + this.reference + ")";
        }
    }
}
