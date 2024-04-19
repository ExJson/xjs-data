package xjs.data.comments;

import xjs.data.JsonValue;

/**
 * This class represents the specific type of comment to be used in conjunction with a {@link JsonValue}.
 * Comments can be placed by calling {@link CommentHolder#set(CommentType, CommentStyle, String)}
 * or another such variant.
 */
public enum CommentType {

    /**
     * Indicates that a comment precedes the value that it is paired with, to be placed one line
     * before the value. In the case of parent or root objects, this type indicates that the comment
     * is a header inside the json file.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   // Header comment
     *   key: value
     *
     * }</pre>
     *
     * <p>Header comments are assumed to have newline character immediately following. For this reason,
     * regardless of whether a newline character is found, <b>the first newline following a header
     * comment will not be included in the comment data.</b>
     */
    HEADER,

    /**
     * Indicates that a comment follows the value that it is paired with, to be placed at the end
     * of the line.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   key: value // EOL comment
     *
     * }</pre>
     *
     * <p>Note that while EOL comments <em>do</em> often stretch to the end of the line, they are
     * <b>not</b> assumed to cover the full line. For this reason, <b>all newline characters following
     * an EOL will be included in the comment data.</b>
     */
    EOL,

    /**
     * Indicates that a comment is to be written <b>at the very bottom of a file</b>. For example, in
     * the case of parent or root objects, this type indicates that the comment is a footer at the
     * very bottom of the json file.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   {
     *     key: value
     *   }
     *   // Footer comment
     *
     * }</pre>
     *
     * <p>Note that the current {@link JsonValue JSON value} data structure cannot capture any newline
     * characters, so <b>any newline characters preceding this comment will be included above it.</b>
     */
    FOOTER,

    /**
     * Indicates that a comment is to after a key, but before the value in a JSON object member.
     * This is most commonly seen when a value is written on the following line after its key.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   key:
     *     // Value comment
     *     value
     *
     * }</pre>
     *
     * <p>As with {@link #EOL} comments, Value comments do not have any requirements for newline
     * characters before or after. <b>Any newline characters <em>after</em> a comment at this
     * position will be appended to it.</b> However, most writers will automatically insert one
     * line after this comment if any newlines are printed before it.
     */
    VALUE,

    /**
     * Indicates that a comment falls anywhere else in association with this value. This usually
     * implies that the value is inside an empty object or array.
     *
     * <p>For example,
     *
     * <pre>{@code
     *
     *   key: [
     *     // Interior comment
     *   ]
     *
     * }</pre>
     *
     * <p>As with {@link #EOL} comments, Interior comments do not have any requirements for newline
     * characters before or after. <b>Any newline characters <em>after</em> a comment at this
     * position will be appended to it.</b> However, most writers will automatically insert one
     * line after this comment if any newlines are printed before it.
     */
    INTERIOR
}
