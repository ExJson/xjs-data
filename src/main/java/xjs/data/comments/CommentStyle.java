package xjs.data.comments;

import xjs.data.JsonValue;

/**
 * This class represents the various comment styles that can be used when adding
 * new comments to a {@link JsonValue}.
 */
public enum CommentStyle {

    /**
     * Hash style comments, indicated by placing a <code>#</code> symbol at the
     * start of the comment, followed by the beginning of each new line.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   # Hash comment
     *   key: value
     *
     * }</pre>
     */
    HASH("#", false),

    /**
     * The C style line comment, indicated by placing a <code>//</code> at the
     * start of the comment, followed by the beginning of each new line.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   // Line comment
     *   key: value
     *
     * }</pre>
     */
    LINE("//", false),

    /**
     * A block style comment, indicated by placing a <code>/*</code> at the
     * start of the comment, followed by a <code>* /</code> (with no spaces) at
     * the very end of the comment.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   /*
     *     Block comment
     *    * /
     *   key: value
     *
     * }</pre>
     */
    BLOCK("/*", true),

    /**
     * A variant of {@link #LINE} written with a third slash (<code>/</code>) at
     * the beginning of the comment.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   /// Line document
     *   key: value
     *
     * }</pre>
     */
    LINE_DOC("///", false),

    /**
     * A variant of {@link #BLOCK} written with a second asterisk (<code>*</code>)
     * at the beginning of the comment.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   /**
     *    * Multiline document
     *    * /
     *   key: value
     *
     * }</pre>
     */
    MULTILINE_DOC("/**", true);

    private final String prefix;
    private final boolean multiline;

    CommentStyle(final String prefix, final boolean multiline) {
        this.prefix = prefix;
        this.multiline = multiline;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public boolean isMultiline() {
        return this.multiline;
    }
}
