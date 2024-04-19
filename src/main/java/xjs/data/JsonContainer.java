package xjs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.writer.ElementWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The JSON container is an abstraction around JSON objects and JSON arrays which exposes
 * their contents in a unified or generic context.
 *
 * <p>For example, to get the first number from any JSON container,
 *
 * <pre>{@code
 *   final Optional<JsonValue> first =
 *     container.values()
 *       .stream()
 *       .filter(JsonValue::isNumber)
 *       .findFirst();
 * }</pre>
 *
 * <p>To get an element by index:
 *
 * <pre>{@code
 *   final JsonValue first = container.get(0);
 * }</pre>
 *
 * <p>To format the container onto a single line and print it:
 *
 * <pre>{@code
 *   final String formatted =
 *     container.condense()
 *       .toString(JsonFormat.DJS_FORMATTED);
 * }</pre>
 *
 * <p>Or increment each number by 1:
 *
 * <pre>{@code
 *   container.references()
 *     .stream()
 *     .filter(ref ->
 *       ref.getOnly().isNumber())
 *     .forEach(ref ->
 *       ref.apply(v -> v.asDouble() + 1));
 * }</pre>
 */
public abstract class JsonContainer extends JsonValue {

    protected final List<JsonReference> references;
    protected View<JsonValue> accessor;
    protected View<JsonValue> visitor;
    protected int linesTrailing;

    /**
     * Constructs a new container with no contents. To be used only by implementors.
     */
    protected JsonContainer() {
        this(new ArrayList<>());
    }

    /**
     * Constructs a new container from an existing set of {@link JsonReference references}.
     * To bne used only be implementors.
     *
     * @param references An existing set of references, thus preventing any access or
     *                   mutation.
     */
    protected JsonContainer(final List<JsonReference> references) {
        this.references = references;
        this.linesTrailing = -1;
    }

    /**
     * Gets the number of newline characters to be printed before the very end of this
     * container.
     *
     * <p>For example, an array with 0 lines trailing will be printed as follows:
     *
     * <pre>{@code
     *   []
     * }</pre>
     *
     * <p>However, an array with 2 lines trailing will be printed with a single empty
     * line after the final value:
     *
     * <pre>{@code
     *   [
     *     1, 2, 3
     *
     *   ]
     * }</pre>
     *
     * <p>Note that, when this value has not been configured, it will default to -1. In
     * this case, the formatting will be determined on a contextual basis by the
     * {@link ElementWriter writer}.
     *
     * @return The number of newline characters at the very end of this container.
     */
    public int getLinesTrailing() {
        return this.linesTrailing;
    }

    /**
     * Sets the number of newline characters to be printed before the very end of this
     * container.
     *
     * @param linesTrailing The number of newline characters print.
     * @return <code>this</code>, for method chaining.
     * @see #getLinesTrailing()
     */
    public JsonContainer setLinesTrailing(final int linesTrailing) {
        this.linesTrailing = linesTrailing;
        return this;
    }

    /**
     * Formats this array to display the given number of references on each line.
     *
     * <p>This operation is valid only when every expected value <b>already exists
     * inside of the container</b>. It is provided for convenience here, but due to its
     * nature, it may be moved into a dedicated JSON formatting utility in the future.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param lineLength The number of elements to output on each line.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer setLineLength(final int lineLength) {
        for (int i = 0; i < this.references.size(); i++) {
            final int linesAbove = i % lineLength == 0 ? 1 : 0;
            this.references.get(i).getOnly().setLinesAbove(linesAbove).setLinesBetween(0);
        }
        return this;
    }

    /**
     * Formats this container to place each element on the same line.
     *
     * <p>This operation is valid only when every expected value <b>already exists
     * inside of the array</b>. It is provided for convenience here, but due to its
     * nature, it may be moved into a dedicated JSON formatting utility in the future.
     *
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer condense() {
        this.references.forEach(ref -> ref.getOnly().setLinesAbove(0).setLinesBetween(0));
        return this;
    }

    /**
     * Override allowing this method to preserve {@link #linesTrailing} when copying
     * metadata.
     *
     * @param other Any other value being copied out of.
     * @return <code>this</code>, for method chaining.
     */
    @Override
    public JsonContainer setDefaultMetadata(final JsonValue other) {
        if (other instanceof JsonContainer) {
            final JsonContainer c = (JsonContainer) other;
            if (this.linesTrailing < 0) this.linesTrailing = c.linesTrailing;
        }
        return (JsonContainer) super.setDefaultMetadata(other);
    }

    /**
     * Returns a view of each reference in this container. Callers should be aware that
     * the return value of this method is <b>immutable</b>. However, callers may
     * <em>replace</em> values within this collection by mutating the references directly.
     *
     * <pre>{@code
     *   container.references()
     *     .get(0)
     *     .set(Json.value(1234))
     * }</pre>
     *
     * <p>Alternatively, the references can be collected into a <em>new</em> container:
     *
     * <pre>{@code
     *   final JsonContainer collected =
     *     container.references()
     *       .stream()
     *       .filter(ref -> ref.getOnly().isNumber())
     *       .collect(JsonCollectors.reference());
     * }</pre>
     *
     * @return An immutable view of the references being wrapped by this container.
     */
    public Collection<JsonReference> references() {
        return Collections.unmodifiableCollection(this.references);
    }

    /**
     * Performs a simple bounds check on the given index. If a value does exist, this
     * returns <code>true</code>, else it returns <code>false</code>.
     *
     * @param index The index of the value being inspected.
     * @return <code>true</code>, if the given index is in bounds.
     */
    public boolean has(final int index) {
        return index >= 0 && index < this.references.size();
    }

    /**
     * Returns whichever value is being stored at the given index, or else throws an
     * {@link IndexOutOfBoundsException}.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index The index of the value being inspected.
     * @return Whichever {@link JsonValue} is at the give index.
     * @throws IndexOutOfBoundsException If the given index falls out of bounds.
     */
    public JsonValue get(final int index) {
        return this.references.get(index).get();
    }

    /**
     * Returns whatever data is stored at the given index, if it matches the filter.
     * If the value exists, and it matches the given filter, a value will be returned.
     * Otherwise, this method returns {@link Optional#empty}.
     *
     * <p>For example, to retrieve string value at index 0:
     *
     * <pre>{@code
     *   final String s = container.getOptional(0, JsonValue::asString).orElse("");
     * }</pre>
     *
     * <p>In the previous example, <b>only the happy path will be acknowledged</b>.
     * Missing values and unexpected data types will both simply return empty.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index  The index of the value being inspected.
     * @param filter A filter to extra the type of data being expected.
     * @param <T>    The type of value being returned by this method.
     * @return The expected data, or else {@link Optional#empty}.
     */
    public <T> Optional<T> getOptional(final int index, final Function<JsonValue, T> filter) {
        return this.getOptional(index).flatMap(value -> Optional.ofNullable(mapSuppressing(value, filter)));
    }

    /**
     * Returns the value stored at the given index. If the given index is out of bounds,
     * returns {@link Optional#empty}.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @param index The index of the value being inspected.
     * @return The expected value, or else {@link Optional#empty}.
     */
    public Optional<JsonValue> getOptional(final int index) {
        return this.has(index) ? Optional.of(this.references.get(index).get()) : Optional.empty();
    }

    /**
     * Returns a <em>{@link JsonReference reference}</em> to the value at the given index,
     * or else throws an {@link IndexOutOfBoundsException}.
     *
     * @param index The index of the value being inspected.
     * @return A reference to this value, which can be reassigned or shared elsewhere.
     * @throws IndexOutOfBoundsException If the given index falls out of bounds.
     */
    public JsonReference getReference(final int index) {
        return this.references.get(index);
    }

    /**
     * Returns the number of values in this container.
     *
     * @return The number of values in this container.
     */
    public int size() {
        return this.references.size();
    }

    /**
     * Returns whether this container has no values in it.
     *
     * @return <code>true</code>, if the container has no values.
     */
    public boolean isEmpty() {
        return this.references.isEmpty();
    }

    /**
     * Returns whether any of the values in this container matches the given integer.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final long value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container matches the given decimal.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final double value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container matches the given boolean.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final boolean value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container matches the given string.
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if a match is found.
     * @see #contains(JsonValue)
     */
    public boolean contains(final @Nullable String value) {
        return this.contains(Json.value(value));
    }

    /**
     * Returns whether any of the values in this container equals the input.
     *
     * <p>Note that, due to the contract of {@link Object#equals}, whether a match is found
     * is sensitive to the metadata associated with the values.
     *
     * <p>To demonstrate, the following is an assertion of the expected behaviors:
     *
     * <pre>{@code
     *   assert Json.array(1, 2, 3).contains(1);
     *   assert !Json.parse("[1, 2, 3]").asArray().contains(1);
     *   assert Json.parse("[1, 2, 3]").asArray().unformatted().contains(1);
     * }</pre>
     *
     * @param value The value being queried in this container.
     * @return <code>true</code>, if an exact match is found.
     */
    public boolean contains(final JsonValue value) {
        return this.indexOf(value) >= 0;
    }

    /**
     * Returns whether each value in the given iterable can also be found in this container.
     *
     * @param values An iterable of {@link JsonValue values} to be queried against.
     * @return <code>true</code>, only if <b>exact</b> matches are found for each value.
     * @see #contains(JsonValue)
     */
    public boolean containsAll(final Iterable<JsonValue> values) {
        return StreamSupport.stream(values.spliterator(), false).anyMatch(this::contains);
    }

    /**
     * Exposes a {@link View view} of the values in this container.
     *
     * <p>This is an {@link JsonReference#get accessing} operation.
     *
     * @return An {@link AccessingView accessing view} which may be iterated or streamed.
     */
    public View<JsonValue> values() {
        if (this.accessor == null) {
            return this.accessor = new AccessingView();
        }
        return this.accessor;
    }

    /**
     * Variant of {@link #values} intended for reflective purposes. This may be needed for
     * any user wishing to track which values do and do not get used by their application.
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @return An {@link AccessingView accessing view} which may be iterated or streamed.
     */
    public View<JsonValue> visitAll() {
        if (this.visitor == null) {
            return this.visitor = new VisitingView();
        }
        return this.visitor;
    }

    /**
     * Recursively executes some action for each value in this container. In other words, for
     * any container nested <em>inside</em> of this container, each of its elements will also
     * be exposed to the consumer.
     *
     * <p>Because this method exposes references to the consumer instead of raw values, this
     * means the data may be mutated in place at any level in the container. For example:
     *
     * <pre>{@code
     *   // Transform numbers into strings of numbers.
     *   container.forEachRecursive(ref -> {
     *      if (ref.isNumber()) {
     *          ref.apply(JsonValue::intoString);
     *      }
     *   });
     * }</pre>
     *
     * <p>This is a {@link JsonReference#getOnly visiting} operation.
     *
     * @param fn An action to execute for each value in this container.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer forEachRecursive(final Consumer<JsonReference> fn) {
        this.references.forEach(ref -> {
            fn.accept(ref);
            if (ref.getOnly().isContainer()) {
                ref.getOnly().asContainer().forEachRecursive(fn);
            }
        });
        return this;
    }

    /**
     * Removes every reference from this container. The container will be empty after this
     * operation completes.
     *
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer clear() {
        this.references.clear();
        return this;
    }

    /**
     * Removes a single reference from this container if its contents match the given value.
     *
     * @param value The value being compared against.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer remove(final JsonValue value) {
        final int index = this.indexOf(value);
        if (index >= 0) {
            this.references.remove(index);
        }
        return this;
    }

    /**
     * Removes a collection of references from this container.
     *
     * @param values An iterable providing values to be removed.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer removeAll(final Iterable<JsonValue> values) {
        values.forEach(this::remove);
        return this;
    }

    /**
     * Recursively updates the access flags for every reference in this container.
     *
     * @param accessed Whether each reference should be marked as accessed.
     * @return <code>this</code>, for method chaining.
     */
    public JsonContainer setAllAccessed(final boolean accessed) {
        return this.forEachRecursive(ref -> ref.setAccessed(accessed));
    }

    /**
     * Gets the index of the first matching element in the container, or else -1.
     *
     * @param value The value being compared against.
     * @return The index of the first matching element, or else -1.
     */
    public int indexOf(final JsonValue value) {
        for (int i = 0; i < this.references.size(); i++) {
            if (this.references.get(i).getOnly().matches(value)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converts this container into a list of type <code>T</code>.
     *
     * @param mapper A mapper for converting {@link JsonValue JSON values} into <code>T</code>.
     * @param <T>    The type of list being returned by this method.
     * @return A list containing the transformed values.
     */
    public <T> List<T> toList(final Function<JsonValue, T> mapper) {
        return this.values().stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Override indicating that JSON containers are <em>not</em> primitive types.
     *
     * @return <code>false</code>, always.
     */
    @Override
    public final boolean isPrimitive() {
        return false;
    }

    /**
     * Override indicating that all {@link JsonContainer} implementations <em>are</em> containers.
     *
     * @return <code>true</code>, always.
     */
    @Override
    public final boolean isContainer() {
        return true;
    }

    /**
     * Override allowing this type to be cast into a JSON container.
     *
     * @return <code>this</code>, always.
     */
    @Override
    public JsonContainer asContainer() {
        return this;
    }

    /**
     * Allows any {@link JsonContainer} type to be coerced into its first value.
     *
     * @return The first element in this container, or else 0.
     */
    @Override
    public double intoDouble() {
        return this.values().stream().mapToDouble(JsonValue::intoDouble).sum();
    }

    /**
     * Allows any {@link JsonContainer} type to be coerced into its first value.
     *
     * @return The first element in this container, or else false.
     */
    public boolean intoBoolean() {
        return !this.isEmpty() && this.values().stream().allMatch(JsonValue::intoBoolean);
    }

    /**
     * Override returning this value directly instead of constructing a new container.
     *
     * @return <code>this</code>, always
     */
    @Override
    public JsonContainer intoContainer() {
        return this;
    }

    /**
     * Generates a copy of every reference in this container given a series of copy options.
     *
     * @param options Any {@link JsonCopy} flags passed from {@link #copy}.
     * @return Either {@link #references} or a clone of it.
     */
    protected List<JsonReference> copyReferences(final int options) {
        return copyReferences(this.references, options);
    }

    protected static List<JsonReference> copyReferences(
            final List<JsonReference> references, final int options) {
        final boolean tracking = (options & JsonCopy.TRACKING) == JsonCopy.TRACKING;
        final boolean recursive = (options & JsonCopy.RECURSIVE) == JsonCopy.RECURSIVE;
        final boolean containers = (options & JsonCopy.CONTAINERS) == JsonCopy.CONTAINERS;

        if (recursive || containers) {
            final List<JsonReference> copy = new ArrayList<>(references.size());
            for (final JsonReference reference : references) {
                if (recursive || reference.getOnly().isContainer()) {
                    copy.add(reference.copy(tracking).applyOnly(v -> v.copy(options)));
                } else {
                    copy.add(reference);
                }
            }
            return copy;
        }
        return references;
    }

    /**
     * Override indicating that subclasses must still implement this method.
     *
     * @param other The value being compared to.
     * @return <code>true</code>, if the two values match.
     */
    @Override
    public abstract boolean matches(final JsonValue other);

    /**
     * Generates an immutable view of this container in which values may not be added
     * or replaced.
     *
     * @return The frozen equivalent of this container.
     */
    public JsonContainer freeze() {
        return this.freeze(false);
    }

    /**
     * Generates an immutable view of this container in which values may not be added
     * or replaced.
     *
     * @param recursive Whether to freeze this container recursively.
     * @return The frozen equivalent of this container.
     */
    public abstract JsonContainer freeze(final boolean recursive);

    /**
     * Generates a list of every possible JSON path string relative to this container.
     *
     * @return A list of JSON path strings for every possible value.
     */
    public List<String> getPaths() {
        return this.getPaths(PathFilter.ALL);
    }

    /**
     * Generates a list of JSON path strings indicating which paths have or have not
     * been used.
     *
     * @param filter A filter describing which paths to acquire.
     * @return A list of JSON path strings for each used or unused value.
     */
    public abstract List<String> getPaths(final PathFilter filter);

    /**
     * Generates an immutable list of the references in this container.
     *
     * @param recursive Whether to freeze references recursively.
     * @return The frozen list.
     */
    protected List<JsonReference> freezeReferences(final boolean recursive) {
        return this.references.stream()
            .map(ref -> {
                final JsonReference clone = ref.copy(true);
                if (recursive && ref.getOnly().isContainer()) {
                    ref.applyOnly(value -> value.asContainer().freeze(true));
                }
                return clone.freeze();
            }).toList();
    }

    /**
     * Returns the result of the given filter, ignoring any syntax exceptions thrown
     * in the process.
     *
     * @param value  The value being mapped.
     * @param filter A mapper transforming the value into something else.
     * @param <T>    The return type.
     * @return The output of <code>filter</code>, or else <code>null</code>.
     */
    protected static <T> @Nullable T mapSuppressing(final JsonValue value, final Function<JsonValue, T> filter) {
        try {
            return filter.apply(value);
        } catch (final UnsupportedOperationException | SyntaxException ignored) {}
        return null;
    }

    /**
     * A {@link View view} of this container which {@link JsonReference#get accesses}
     * each value as it gets returned to the consumer. This implies that every value
     * in the container is needed (i.e. not technically <em>ignored</em>) by the
     * application, which is significant for diagnostic purposes.
     */
    private class AccessingView implements View<JsonValue> {
        @Override
        public @NotNull Iterator<JsonValue> iterator() {
            return new AccessingValueIterator();
        }
    }

    /**
     * Counterpart to {@link AccessingView} used for the actual iteration of JSON values.
     */
    private class AccessingValueIterator implements Iterator<JsonValue> {
        final Iterator<JsonReference> references = JsonContainer.this.references.iterator();

        @Override
        public boolean hasNext() {
            return this.references.hasNext();
        }

        @Override
        public JsonValue next() {
            return this.references.next().get();
        }

        @Override
        public void remove() {
            this.references.remove();
        }
    }

    /**
     * A {@link View view} of this container which {@link JsonReference#getOnly visits}
     * each value as it gets returned to the consumer. This implies that the actions
     * are intended for the purpose of reflecting on the values in the container. For
     * example, to search for a value or reformat the container.
     */
    private class VisitingView implements View<JsonValue> {
        @Override
        public @NotNull Iterator<JsonValue> iterator() {
            return new VisitingValueIterator();
        }
    }

    /**
     * Counterpart to {@link VisitingView} used for the actual iteration of JSON values.
     */
    private class VisitingValueIterator implements Iterator<JsonValue> {
        final Iterator<JsonReference> references = JsonContainer.this.references.iterator();

        @Override
        public boolean hasNext() {
            return this.references.hasNext();
        }

        @Override
        public JsonValue next() {
            return this.references.next().getOnly();
        }

        @Override
        public void remove() {
            this.references.remove();
        }
    }

    /**
     * An accessor to this container which may be {@link Iterator iterated} or
     * {@link Stream streamed}. Its main purpose is not to transform this container,
     * but to <em>inspect</em> its contents. In other words, to <em>view</em> them.
     *
     * @param <T> The type of value being exposed by the view.
     */
    public interface View<T> extends Iterable<T> {
        default Stream<T> stream() {
            return StreamSupport.stream(this.spliterator(), false);
        }
    }
}
