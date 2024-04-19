package xjs.data;

/**
 * A collection of flags used to configure JSON value copy operations.
 *
 * <p>This may be useful to e.g. strip diagnostics, formatting, or
 * create new containers which may be mutated without affecting their
 * sources.
 *
 * <p>For example, to generate a copy of an object which strips all
 * possible formatting and comments:
 *
 * <pre>{@code
 *   Json.parse("k:v,n:{i:o}").copy(JsonCopy.RECURSIVE);
 * }</pre>
 *
 * <p>To strip regular formatting, preserving comments:
 *
 * <pre>{@code
 *   Json.parse("k:v").copy(JsonCopy.RECURSIVE | JsonCopy.COMMENTS);
 * }</pre>
 */
public final class JsonCopy {

    /**
     * A flag for copying diagnostic access metadata.
     */
    public static final byte TRACKING = 1;

    /**
     * A flag for copying any references that point to other containers.
     */
    public static final byte CONTAINERS = 1 << 1;

    /**
     * A flag for copying <em>all</em> possible references.
     */
    public static final byte RECURSIVE = 1 << 2;

    /**
     * A flag for copying formatting metadata, such as empty lines.
     */
    public static final byte FORMATTING = 1 << 3;

    /**
     * A flag for copying comment metadata.
     */
    public static final byte COMMENTS = 1 << 4;

    /**
     * Combined flags for copying <em>all</em> metadata.
     */
    public static final byte METADATA = FORMATTING | COMMENTS;

    /**
     * Combined flags for copying all containers and their metadata.
     */
    public static final byte NEW_CONTAINERS = METADATA | CONTAINERS;

    /**
     * Combined flags for copying all references and their metadata.
     */
    public static final byte DEEP = RECURSIVE | METADATA;

    /**
     * Combined flags for copying references, metadata, and diagnostics.
     */
    public static final byte DEEP_TRACKING = DEEP | TRACKING;

    /**
     * Flag providing an explicit label for unformatted copying.
     */
    public static final byte UNFORMATTED = RECURSIVE;

    private JsonCopy() {}
}