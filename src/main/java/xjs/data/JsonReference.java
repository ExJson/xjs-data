package xjs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * The JSON reference is an accessor to a {@link JsonValue} which can be made
 * available to multiple containers. It is primarily intended to track use of
 * JSON data for analysis, but can also finely control which members can and
 * cannot be updated by external callers.
 *
 * <p>For example, to increment each number in a {@link JsonArray JSON array}:
 *
 * <pre>{@code
 *   array.references()
 *     .stream()
 *     .filter(ref ->
 *       ref.visit().isNumber())
 *     .forEach(ref ->
 *       ref.apply(n -> n.asDouble() + 1));
 * }</pre>
 *
 * <p>To share references between containers, thus synchronizing changes between
 * them:
 *
 * <pre>{@code
 *   final JsonArray a1 = Json.array(1, 2, 3);
 *   final JsonArray a2 = Json.array(4, 5, 6);
 *
 *   a1.setReference(0, a2.getReference(0));
 *   a1.set(0, 7);
 *
 *   assert Json.array(7, 2, 3).equals(a1);
 *   assert Json.array(7, 5, 6).equals(a2);
 * }</pre>
 */
public class JsonReference {

    protected JsonValue referent;
    protected boolean accessed;
    protected boolean mutable;

    /**
     * Construct a new reference when given a value to wrap.
     *
     * <p>Note that this value may be <code>null</code>, in which case it will
     * simply be wrapped as {@link JsonLiteral#jsonNull}.
     *
     * @param referent The value being wrapped.
     */
    public JsonReference(final @Nullable JsonValue referent) {
        this.referent = Json.nonnull(referent);
        this.accessed = false;
        this.mutable = true;
    }

    /**
     * Returns the value being wrapped by this object.
     *
     * <p>Calling this method implies that the referent is required by the application
     * in some way. For example, to be {@link JsonValue#unwrap unwrapped} and treated
     * as raw data, <em>not</em> JSON data. The alternative would be to {@link #getOnly}
     * the data, which implies that we are using it to update formatting or simply
     * find a value which we <em>do</em> need.
     *
     * <p>We call this an <em>"accessing"</em> operation.
     *
     * <p>Quite literally, this means that the value will be flagged as "accessed,"
     * which can be reflected upon at a later time to provide diagnostics to the end
     * user or to investigate potential optimizations regarding unused values.
     *
     * @return The referent
     */
    public @NotNull JsonValue get() {
        this.accessed = true;
        return this.getOnly();
    }

    /**
     * Returns the value being wrapped by this object.
     *
     * <p>Calling this method does not imply that the referent is required by the
     * application. Instead, the value is being used for the purpose of <em>reflection
     * </em>. For example, to inspect or update formatting options or scan for matching
     * values. Philosophically speaking, this means that the value could be removed
     * without changing the behavior of the application in any significant way.
     *
     * <p>We call this a <em>"visiting"</em> operation.
     *
     * <p>Literally speaking, this operation avoids updating this value's access flags,
     * which means the value will still be in an "unused" state. This can be reflected
     * upon at a later time to provide diagnostics to the end user or to investigate
     * potential optimizations regarding unused values.
     *
     * @return The referent
     */
    public JsonValue getOnly() {
        return this.referent;
    }

    /**
     * Points this reference toward a different {@link JsonValue}.
     *
     * <p>This is an {@link #get accessing} operation.
     *
     * @param referent The new referent being wrapped by this object.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @see #get
     */
    public JsonReference set(final @Nullable JsonValue referent) {
        this.accessed = true;
        return this.setOnly(referent);
    }

    /**
     * Visiting counterpart of {@link #set}.
     *
     * <p>This is a {@link #getOnly visiting} operation.
     *
     * @param referent The new referent being wrapped by this object.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @see #getOnly
     */
    public JsonReference setOnly(final @Nullable JsonValue referent) {
        this.checkMutable();
        this.referent = Json.nonnull(referent);
        return this;
    }

    /**
     * Applies the given transformation to the referent of this object.
     *
     * <p>This is an {@link #get accessing} operation.
     *
     * @param updater An expression transforming the wrapped {@link JsonValue}.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @see #get
     */
    public JsonReference apply(final Function<JsonValue, Object> updater) {
        this.accessed = true;
        return this.applyOnly(updater);
    }

    /**
     * Visiting counterpart of {@link #apply}.
     *
     * <p>This is a {@link #getOnly visiting} operation.
     *
     * @param updater An expression transforming the wrapped {@link JsonValue}.
     * @return <code>this</code>, for method chaining.
     * @throws UnsupportedOperationException If this reference is immutable.
     * @see #getOnly
     */
    public JsonReference applyOnly(final Function<JsonValue, Object> updater) {
        this.checkMutable();
        final Object result = updater.apply(this.referent);
        if (result instanceof JsonValue) {
            this.referent = (JsonValue) result;
        } else {
            this.referent = Json.any(result).setDefaultMetadata(this.referent);
        }
        return this;
    }

    /**
     * Indicates whether this reference has been {@link #get accessed}.
     *
     * @return <code>true</code>, if the value has been accessed.
     */
    public boolean isAccessed() {
        return this.accessed;
    }

    /**
     * Overrides the access flag for this reference.
     *
     * @param accessed Whether the value has been {@link #get accessed}.
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference setAccessed(final boolean accessed) {
        this.accessed = accessed;
        return this;
    }

    /**
     * Indicates whether this reference may be updated.
     *
     * @return <code>true</code>, if the reference may be updated.
     */
    public boolean isMutable() {
        return this.mutable;
    }

    /**
     * Freezes this reference into an immutable state.
     *
     * <p><b>This operation is permanent</b>.
     *
     * @return <code>this</code>, for method chaining.
     */
    public JsonReference freeze() {
        this.mutable = false;
        return this;
    }

    private void checkMutable() {
        if (!this.mutable) {
            throw new UnsupportedOperationException("Reference is immutable: " + this);
        }
    }

    /**
     * Generates a mutable clone of this reference.
     *
     * @param trackAccess Whether to additionally persist access tracking.
     * @return A copy of this reference.
     */
    public JsonReference copy(final boolean trackAccess) {
        final JsonReference clone = new JsonReference(this.referent);
        return trackAccess ? clone.setAccessed(this.accessed) : clone;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.referent.hashCode();
        if (this.accessed) result *= 17;
        if (this.mutable) result *= 31;
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof final JsonReference other) {
            return this.referent.equals(other.referent)
                && this.accessed == other.accessed
                && this.mutable == other.mutable;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.referent.toString();
    }
}
