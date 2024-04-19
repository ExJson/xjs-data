package xjs.data.comments;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility layer and container housing comments for any JSON value. This object exposes the position,
 * type, message, and entire formatting of each comment paired with a single JSON value.
 *
 * <p>Callers should <b>not expect</b> comment data to be stored as string values in the future, as
 * this implementation may change.
 */
public class CommentHolder {
    private static final CommentData EMPTY_COMMENT = CommentData.immutable();

    private final Map<CommentType, CommentData> map = new HashMap<>();

    /**
     * Indicates whether <b>any</b> comment is present within this container.
     *
     * @return <code>true</code> if any comment is present.
     */
    public boolean hasAny() {
        return !this.map.isEmpty();
    }

    /**
     * Indicates whether this container houses a specific type of container.
     *
     * @param type The type of comment being queried against.
     * @return <code>true</code> if this type of comment is present.
     */
    public boolean has(final CommentType type) {
        return this.map.containsKey(type);
    }

    /**
     * Returns the <b>message</b> present in whichever comment is being queried.
     *
     * <p>For example, a holder containing this exact comment data:
     *
     * <pre>{@code
     *   "// this is a comment"
     * }</pre>
     *
     * <p>Will return this exact string:
     *
     * <pre>{@code
     *   "this is a comment"
     * }</pre>
     *
     * @param type The type of comment being queried.
     * @return The message of this comment, or else <code>""</code>.
     */
    public String get(final CommentType type) {
        return this.getData(type).toString();
    }

    /**
     * Returns the entire contents of whichever comment is being queried. The return value of this
     * method includes all available formatting, including the comment indicators and any additional
     * lines after the comment message.
     *
     * <p>For example, a holder containing this exact data:
     *
     * <pre>{@code
     *   """
     *   // comment
     *
     *   # comment
     *   """
     * }</pre>
     *
     * <p>Will return the data exactly.
     *
     * @param type The type of comment being queried.
     * @return The entire comment data being stored by this container, or else <code>""</code>.
     */
    public CommentData getData(final CommentType type) {
        return this.map.getOrDefault(type, EMPTY_COMMENT);
    }

    /**
     * Variant of {@link #getData} returning an appendable value if absent.
     *
     * @param type The type of comment being queried.
     * @return The entire comment data being stored by this container.
     */
    public CommentData getOrCreate(final CommentType type) {
        return this.map.computeIfAbsent(type, t -> new CommentData());
    }

    /**
     * Sets the message and style for a given comment position.
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being created.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder set(final CommentType type, final CommentStyle style, final String text) {
        final CommentData data = new CommentData();
        data.append(new Comment(style, text));
        return this.setData(type, data);
    }

    /**
     * Variant of {@link #set(CommentType, CommentStyle, String)} which appends a number of empty
     * lines at the end of the data.
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being created.
     * @param lines The number of <em>empty</em> lines at the end of this message.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder set(final CommentType type, final CommentStyle style, final String text, final int lines) {
        final CommentData data = new CommentData();
        data.append(new Comment(style, text));
        data.append(lines);
        return this.setData(type, data);
    }

    /**
     * Appends an additional comment <em>after</em> the existing comment at this position.
     *
     * <p>For example, if this container is already housing the following comment:
     *
     * <pre>{@code
     *   # line 1
     * }</pre>
     *
     * <p>If the following comment is appended:
     *
     * <pre>{@code
     *   CommentStyle.HASH, "line 2"
     * }</pre>
     *
     * <p>The data will be updated as follows:
     *
     * <pre>{@code
     *   # line 1
     *   # line 2
     * }</pre>
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being appended.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder append(final CommentType type, final CommentStyle style, final String text) {
        final CommentData data = this.getOrCreate(type);
        data.append(1);
        data.append(new Comment(style, text));
        return this;
    }

    /**
     * Appends raw comment data of each position into this holder.
     *
     * @param comments Another comment holder containing any number of comments.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder appendAll(final CommentHolder comments) {
        comments.map.forEach((type, otherData) ->
            this.getOrCreate(type).append(otherData));
        return this;
    }

    /**
     * Prepends an additional comment <em>before</em> the existing comment at this position.
     *
     * @param type  The type of comment being set in this container.
     * @param style The style of comment (e.g. hash or line).
     * @param text  The message of the comment being prepended.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder prepend(final CommentType type, final CommentStyle style, final String text) {
        final CommentData data = this.getOrCreate(type);
        data.prepend(1);
        data.prepend(new Comment(style, text));
        return this;
    }

    /**
     * Sets the raw comment data for the given position. Note that the comment must be
     * syntactically valid or else it will produce a <b>silent error</b> when serialized.
     *
     * @param type The type of comment being set in this container.
     * @param data The raw, <b>already formatted</b> data being placed on the corresponding value.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder setData(final CommentType type, final CommentData data) {
        this.map.put(type, data);
        return this;
    }

    /**
     * Adds a number of additional, <em>empty</em> lines after the comment in its current state.
     *
     * @param type  The type comment being appended to.
     * @param lines The number of <em>empty</em> lines after the existing content.
     * @return <code>this</code>, for method chaining.
     */
    public CommentHolder setLinesAfter(final CommentType type, final int lines) {
        this.map.computeIfAbsent(type, t -> new CommentData()).setLinesAfter(lines);
        return this;
    }

    /**
     * Generates a clone of this object, housing the same comments.
     *
     * @return A clone of this object.
     */
    public CommentHolder copy() {
        final CommentHolder copy = new CommentHolder();
        this.map.forEach((type, data) -> copy.map.put(type, data.copy()));
        return copy;
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof CommentHolder) {
            return this.map.equals(((CommentHolder) o).map);
        }
        return false;
    }
}
