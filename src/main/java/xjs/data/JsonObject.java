package xjs.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.data.serialization.util.HashIndexTable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link JsonContainer} providing updaters by <em>index</em>.
 *
 * <p>For example, to insert additional values into this container, callers may
 * use {@link JsonObject#add(String, JsonValue)} or one of its overloads.
 *
 * <pre>{@code
 *   Json.object().add("Hello", "World!");
 * }</pre>
 *
 * <p>Alternatively, to update an <em>existing</em> value, callers may use
 * {@link JsonObject#set(String, JsonValue)} or one of its overloads.
 *
 * <pre>{@code
 *   Json.object().add("Hello", "World!").set("Hello", "Mother");
 * }</pre>
 *
 * <p>The default {@link JsonContainer.View view} of this container exposes a
 * direct handle on the key and reference, known as the {@link JsonObject.Member}.
 * This means that keys and values may be streamed as a <em>zip</em> using the
 * {@link Stream} API.
 *
 * <p>For example, to filter the contents of this object into an object containing
 * exclusively {@link JsonNumber numbers}:
 *
 * <pre>{@code
 *   final JsonObject object =
 *     Json.object()
 *       .add("a", 1)
 *       .add("b", 2)
 *       .add("c", "3")
 *       .add("d", 4);
 *   final JsonObject numbers =
 *     object.stream()
 *       .filter(m -> m.visit().isNumber())
 *       .collect(JsonCollectors.member());
 *   final JsonObject expected =
 *       .add("a", 1)
 *       .add("b", 2)
 *       .add("d", 4);
 *   assert expected.equals(numbers);
 * }</pre>
 */
public class JsonObject extends JsonContainer implements JsonContainer.View<JsonObject.Member> {

    private final List<String> keys;
    private final transient HashIndexTable table;

    /**
     * Constructs a new JSON object containing no contents.
     */
    public JsonObject() {
        this.keys = new ArrayList<>();
        this.table = new HashIndexTable();
    }

    protected JsonObject(final List<String> keys, final List<JsonReference> references) {
        super(references);
        this.keys = keys;
        this.table = new HashIndexTable();
        this.table.init(this.keys);
    }

    /**
     * Returns an unmodifiable view of the keys in this object.
     *
     * <p>For example:
     *
     * <pre>{@code
     *   final JsonObject object =
     *     Json.object().add("a", 1).add("b", 2).add("c", 3);
     *   assert List.of("a", "b", "c").equals(object.keys());
     * }</pre>
     *
     * @return A collection containing every key in this object.
     */
    public List<String> keys() {
        return Collections.unmodifiableList(this.keys);
    }

    /**
     * Variant of {@link #set(String, JsonValue)} wrapping an integer value.
     *
     * @param key   The index of the value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject set(final String key, final long value) {
        return this.set(key, Json.value(value));
    }

    /**
     * Variant of {@link #set(String, JsonValue)} wrapping a decimal value.
     *
     * @param key   The index of the value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject set(final String key, final double value) {
        return this.set(key, Json.value(value));
    }

    /**
     * Variant of {@link #set(String, JsonValue)} wrapping a boolean value.
     *
     * @param key   The index of the value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject set(final String key, final boolean value) {
        return this.set(key, Json.value(value));
    }

    /**
     * Variant of {@link #set(String, JsonValue)} wrapping a String value.
     *
     * @param key   The index of the value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject set(final String key, final String value) {
        return this.set(key, Json.value(value));
    }

    /**
     * Replaces a value with the given key, or else adds it.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param key   The key of the value being replaced.
     * @param value The value being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonObject set(final String key, final @Nullable JsonValue value) {
        final int index = this.indexOf(key);
        if (index != -1) {
            this.references.get(index).apply(og ->
                Json.nonnull(value).setDefaultMetadata(og));
            return this;
        }
        return this.addReference(key, new JsonReference(value).setAccessed(true));
    }

    /**
     * Replaces a reference for the given key, or else adds it.
     *
     * @param key The key of the reference being replaced.
     * @param reference The reference being set at this index.
     * @return <code>this</code>, for method chaining.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public JsonObject setReference(final String key, final JsonReference reference) {
        final int index = this.indexOf(key);
        if (index != -1) {
            this.references.set(index, reference);
            return this;
        }
        return this.addReference(key, reference);
    }

    /**
     * Sets the header comment for a value with the given key.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param key     The key of the value being updated.
     * @param comment The message of the generated comment.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException if the value does not exist.
     */
    public JsonObject setComment(final String key, final String comment) {
        final JsonValue expected = this.get(key);
        if (expected == null) {
            throw new UnsupportedOperationException("Setting comment on null value (" + key + ")");
        }
        expected.setComment(comment);
        return this;
    }

    /**
     * Sets a variety of default values recursively, ignoring arrays.
     *
     * <p>For example,
     *
     * <pre>{@code
     *   final JsonObject object =
     *     Json.object().add("color", "orange");
     *   final JsonObject defaults =
     *     Json.object().add("color", "yellow").add("size", "small");
     *   final JsonObject expected =
     *     Json.object().add("color", "orange").add("size", "small");
     *
     *   object.setDefaults(defaults);
     *   assert expected.equals(object);
     * }</pre>
     *
     * @param defaultValues The default values being set in this object.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject setDefaults(final JsonObject defaultValues) {
        final Iterator<String> keyIterator = defaultValues.keys.iterator();
        final Iterator<JsonReference> referenceIterator = defaultValues.references.iterator();

        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            final String key = keyIterator.next();
            final JsonReference reference = referenceIterator.next();

            final JsonValue replaced = this.get(key, null);
            if (replaced == null) {
                this.add(key, reference.get());
            } else if (replaced.isObject() && reference.getOnly().isObject()) {
                replaced.asObject().setDefaults(reference.get().asObject());
            }
        }
        return this;
    }

    /**
     * Variant of {@link #add(String, JsonValue)} wrapping an integer value.
     *
     * @param key   The key of the value being added.
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final long value) {
        return this.add(key, Json.value(value));
    }

    /**
     * Variant of {@link #add(String, JsonValue)} wrapping a decimal value.
     *
     * @param key   The key of the value being added.
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final double value) {
        return this.add(key, Json.value(value));
    }

    /**
     * Variant of {@link #add(String, JsonValue)} wrapping a boolean value.
     *
     * @param key   The key of the value being added.
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final boolean value) {
        return this.add(key, Json.value(value));
    }

    /**
     * Variant of {@link #add(String, JsonValue)} wrapping a string value.
     *
     * @param key   The key of the value being added.
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final String value) {
        return this.add(key, Json.value(value));
    }

    /**
     * Adds a {@link JsonValue} into this container.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param key   The key of the value being added.
     * @param value The value being added into the container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final @Nullable JsonValue value) {
        return this.addReference(key, new JsonReference(value));
    }

    /**
     * Variant of {@link #add(String, JsonValue, String)} wrapping an integer value.
     *
     * @param key     The key of the value being added.
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final long value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(String, JsonValue, String)} wrapping a decimal value.
     *
     * @param key     The key of the value being added.
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final double value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(String, JsonValue, String)} wrapping a boolean value.
     *
     * @param key     The key of the value being added.
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final boolean value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(String, JsonValue, String)} wrapping a string value.
     *
     * @param key     The key of the value being added.
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final String value, final String comment) {
        return this.add(key, Json.value(value), comment);
    }

    /**
     * Variant of {@link #add(String, JsonValue)} which appends a header comment to the
     * given value.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param key     The key of the value being added.
     * @param value   The value being added into the container.
     * @param comment The message of the generated comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject add(final String key, final @Nullable JsonValue value, final String comment) {
        return this.add(key, Json.nonnull(value).setComment(comment));
    }

    /**
     * Adds each {@link JsonReference reference} from the given object into this
     * object.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param object Some other object housing keys and references to be copied.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject addAll(final JsonObject object) {
        final Iterator<String> keyIterator = object.keys.iterator();
        final Iterator<JsonReference> referenceIterator = object.references.iterator();
        while (keyIterator.hasNext() && referenceIterator.hasNext()) {
            this.addReference(keyIterator.next(), referenceIterator.next());
        }
        return this;
    }

    /**
     * Adds a single {@link JsonReference reference} directly into this container.
     *
     * @param key       The key of the reference being added.
     * @param reference An existing reference being copied into this container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject addReference(final String key, final JsonReference reference) {
        this.table.add(key, this.keys.size());
        this.keys.add(key);
        this.references.add(reference);
        return this;
    }

    /**
     * Variant of {@link #set(String, JsonValue)} which simultaneously replaces
     * the original key.
     *
     * @param index The index of the key and value being replaced.
     * @param key   The new key being set at this index.
     * @param value The new value being set at this index.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject set(final int index, final String key, final @Nullable JsonValue value) {
        if (!this.has(index)) {
            return this.add(key, value);
        }
        this.references.get(index).apply(og ->
            Json.nonnull(value).setDefaultMetadata(og));
        this.keys.set(index, key);
        this.table.clear();
        this.table.init(this.keys);
        return this;
    }

    /**
     * Replaces the key at the given index.
     *
     * @param index The index of the key being replaced.
     * @param key   The new key to set at this index.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject setKey(final int index, final String key) {
        if (index >= 0 && index < this.references.size()) {
            this.keys.set(index, key);
            this.table.clear();
            this.table.init(this.keys);
        }
        return this;
    }

    /**
     * Adds a new value into this object at the given index.
     *
     * @param index The index at which to insert the new key and value.
     * @param key   The key of the new member.
     * @param value The value of the new member.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject insert(final int index, final String key, final @Nullable JsonValue value) {
        return this.insertReference(index, key, new JsonReference(value).setAccessed(true));
    }

    /**
     * Adds an existing reference into this object at the given index.
     *
     * @param index     The index at which to insert the new key and reference.
     * @param key       The key of the new member.
     * @param reference The reference of the new member.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject insertReference(final int index, final String key, final JsonReference reference) {
        this.references.add(index, reference);
        this.keys.add(index, key);
        this.table.clear();
        this.table.init(this.keys);
        return this;
    }

    /**
     * Indicates whether any value exists with the given key in this object.
     *
     * @param key The key being queried for.
     * @return <code>true</code>, if the given key is found.
     */
    public boolean has(final String key) {
        return this.indexOf(key) != -1;
    }

    /**
     * Returns the <em>last</em> value paired with the given key, or else
     * <code>null</code>.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param key The name of the value being returned.
     * @return Whichever {@link JsonValue} is found, or else <code>null</code>.
     * @throws UnsupportedOperationException If the value is absent.
     */
    public @Nullable JsonValue get(final String key) {
        final int index = this.indexOf(key);
        if (index != -1) {
            return this.references.get(index).get();
        }
        return null;
    }

    /**
     * Returns the <em>last</em> value paired with the given key, or else
     * the <code>defaultValue</code>
     *
     * @param key The name of the value being returned.
     * @param defaultValue The default value to return, if absent.
     * @return The expected value, or else <code>defaultValue</code>.
     */
    @Contract("_, !null -> !null")
    public JsonValue get(final String key, final Object defaultValue) {
        final JsonValue expected = this.get(key);
        if (expected != null) {
            return expected;
        } else if (defaultValue == null) {
            return null;
        }
        return Json.any(defaultValue);
    }

    /**
     * Returns the <em>last</em> value paired with the given key, or else
     * throws an {@link UnsupportedOperationException}.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param key The name of the value being returned.
     * @return Whichever {@link JsonValue} is found.
     * @throws UnsupportedOperationException If the value is absent.
     */
    public @NotNull JsonValue getAsserted(final String key) {
        final JsonValue expected = this.get(key);
        if (expected == null) {
            throw new UnsupportedOperationException("Expected: " + key);
        }
        return expected;
    }

    /**
     * Returns the <em>last</em> value paired with the given key, if it matches the
     * filter. If the value exists, and it matches the given filter, a value will be
     * returned. Otherwise, this method returns {@link Optional#empty}.
     *
     * <p>For example, to retrieve string value at index 0:
     *
     * <pre>{@code
     *   final String s = object.getOptional("k", JsonValue::asString).orElse("");
     * }</pre>
     *
     * <p>In the previous example, <b>only the happy path will be acknowledged</b>.
     * Missing values and unexpected data types will both simply return empty.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param key    The key of the value being inspected.
     * @param filter A filter to extra the type of data being expected.
     * @param <T>    The type of value being returned by this method.
     * @return The expected data, or else {@link Optional#empty}.
     */
    public <T> Optional<T> getOptional(final String key, final Function<JsonValue, T> filter) {
        return this.getOptional(key).flatMap(value -> Optional.ofNullable(mapSuppressing(value, filter)));
    }

    /**
     * Returns the <em>last</em> paired with the given key. If the key is not found,
     * returns {@link Optional#empty}.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param key The key of the value being inspected.
     * @return The expected value, or else {@link Optional#empty}.
     */
    public Optional<JsonValue> getOptional(final String key) {
        return Optional.ofNullable(this.get(key));
    }

    /**
     * Returns a <em>{@link JsonReference reference}</em> to the <em>last</em> value
     * paired with the given key, or else <code>null</code>.
     *
     * @param key The key of the value being inspected.
     * @return A reference to this value, which can be reassigned or shared elsewhere.
     */
    public @Nullable JsonReference getReference(final String key) {
        final int index = this.indexOf(key);
        return index != -1 ? this.references.get(index) : null;
    }

    /**
     * Retrieves the key of the member at the giuen index.
     *
     * @param index The index of the key being inspected.
     * @return The key corresponding to the given index.
     * @throws IndexOutOfBoundsException If the index is out of bounds for this container.
     */
    public String getKey(final int index) {
        return this.keys.get(index);
    }

    /**
     * Removes a single value from this container by key.
     *
     * @param key The key of the value being purged.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject remove(final String key) {
        final int index = this.indexOf(key);
        if (index != -1) {
            this.references.remove(index);
            this.keys.remove(index);
            this.table.remove(index);
        }
        return this;
    }

    /**
     * Removes any number of values from this container by key.
     *
     * @param keys The keys of the values being purged.
     * @return <code>this</code>, for method chaining.
     */
    public JsonObject removeAllKeys(final Iterable<String> keys) {
        keys.forEach(this::remove);
        return this;
    }

    /**
     * Returns the index of the <em>last</em> value paired with the
     * given key.
     *
     * @param key The key of the value being inspected.
     * @return The index of this key, or else -1.
     */
    public int indexOf(final String key) {
        final int index = this.table.get(key);
        if (index != -1 && key.equals(this.keys.get(index))) {
            return index;
        }
        return this.keys.lastIndexOf(key);
    }

    @Override
    public JsonObject clear() {
        this.references.clear();
        this.keys.clear();
        this.table.clear();
        return this;
    }

    @Override
    public JsonObject remove(final JsonValue value) {
        final int index = this.indexOf(value);
        if (index != -1) {
            this.references.remove(index);
            this.keys.remove(index);
            this.table.remove(index);
        }
        return this;
    }

    @Override
    public JsonObject removeAll(final Iterable<JsonValue> values) {
        return (JsonObject) super.removeAll(values);
    }

    public <T> Map<String, T> toMap(final Function<JsonValue, T> mapper) {
        return this.stream().collect(Collectors.toMap(Member::getKey, m -> mapper.apply(m.getValue())));
    }

    /**
     * Converts this object into a vanilla {@link Map} of string -> unwrapped.
     *
     * @return A vanilla map representing the same values.
     */
    public Map<String, Object> toMap() {
        return this.toMap(JsonValue::unwrap);
    }

    public JsonObject copy(final int options) {
        final JsonObject copy =
            new JsonObject(new ArrayList<>(this.keys), this.copyReferences(options));
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.setLinesTrailing(this.linesTrailing);
        }
        return withMetadata(copy, this, options);
    }

    @Override
    public final JsonType getType() {
        return JsonType.OBJECT;
    }

    @Override
    public Map<String, Object> unwrap() {
        return this.toMap();
    }

    @Override
    public final boolean isObject() {
        return true;
    }

    @Override
    public JsonObject asObject() {
        return this;
    }

    @Override
    public JsonObject intoObject() {
        return this;
    }

    @Override
    public Iterator<Member> iterator() {
        return new MemberIterator();
    }

    private synchronized void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.table.clear();
        this.table.init(this.keys);
    }

    @Override
    public int valueHashCode() {
        int result = 1;
        result = 31 * result + this.keys.hashCode();
        for (final JsonReference reference : this.references) {
            result = 31 * reference.getOnly().valueHashCode();
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = this.metaHashCode();
        result = 31 * result + this.keys.hashCode();
        return 31 * result + this.references.hashCode();
    }

    @Override
    public boolean matches(final JsonValue other) {
        if (!(other instanceof JsonObject)) {
            return false;
        }
        final JsonObject object = (JsonObject) other;
        if (this.size() != object.size()) {
            return false;
        }
        if (!this.keys.equals((object.keys))) {
            return false;
        }
        for (int i = 0; i < this.size(); i++) {
            if (!this.references.get(i).getOnly().matches(object.references.get(i).getOnly())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof JsonObject) {
            return this.keys.equals(((JsonObject) o).keys)
                && this.references.equals(((JsonObject) o).references);
        }
        return false;
    }

    @Override
    public JsonObject freeze(final boolean recursive) {
        return new JsonObject(this.keys, this.freezeReferences(recursive));
    }

    @Override
    public List<String> getPaths(final PathFilter filter) {
        final List<String> paths = new ArrayList<>();
        for (final Member member : this) {
            if (!filter.test(member.getReference())) {
                continue;
            }
            paths.add(member.getKey());
            if (!member.getOnly().isContainer()) {
                continue;
            }
            final String prefix = member.getOnly().isObject()
                ? member.getKey() + "." : member.getKey();
            for (final String inner : member.getOnly().asContainer().getPaths(filter)) {
                paths.add(prefix + inner);
            }
        }
        return paths;
    }

    private class MemberIterator implements Iterator<Member> {
        final Iterator<String> keys =
            JsonObject.this.keys.iterator();
        final Iterator<JsonReference> references =
            JsonObject.this.references.iterator();
        int index = 0;

        @Override
        public boolean hasNext() {
            return this.keys.hasNext() && this.references.hasNext();
        }

        @Override
        public Member next() {
            return new Member(this.index++, this.keys.next(), this.references.next());
        }

        @Override
        public void remove() {
            this.references.remove();
            this.keys.remove();
        }
    }

    /**
     * The JSON object counterpart to {@link JsonArray.Element}. In addition to
     * exposing the underlying references within this container, Member exposes
     * each value's <em>key</em>, as this is the primary accessor for object values.
     */
    public static class Member extends JsonArray.Element {
        protected final String key;

        /**
         * Constructs a new member accessor when given its key and the new value.
         *
         * @param key   The key pointing to this value.
         * @param value The value being pointed to.
         */
        public Member(final String key, final JsonValue value) {
            this(-1, key, new JsonReference(value));
        }

        /**
         * Constructs a new member accessor when given a key and its reference.
         *
         * @param key       The key pointing to this value.
         * @param reference The reference pointing to the value.
         */
        public Member(final String key, final JsonReference reference) {
            this(-1, key, reference);
        }

        /**
         * Constructs a new member accessor when given an index and its reference.
         *
         * @param index     The index of this member in its object.
         * @param key       The key pointing to this value.
         * @param reference The reference pointing to the value.
         */
        public Member(final int index, final String key, final JsonReference reference) {
            super(index, reference);
            this.key = key;
        }

        /**
         * Returns the name of the value at this index.
         *
         * @return The name of the value.
         */
        public String getKey() {
            return this.key;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + this.index;
            result = 31 * result + this.key.hashCode();
            result = 31 * result + this.reference.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Member) {
                final Member other = (Member) o;
                return this.index == other.index
                    && this.key.equals(other.key)
                    && this.reference.equals(other.reference);
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + this.key + "=" + this.reference + ")";
        }
    }
}
