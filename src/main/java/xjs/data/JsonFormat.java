package xjs.data;

public enum JsonFormat {

    /**
     * Prints unformatted, regular JSON with no whitespace.
     *
     * <p>No formatting options will be preserved.
     */
    JSON,

    /**
     * Pretty prints regular JSON with whitespace.
     *
     * <p>Some formatting options will be preserved.
     */
    JSON_FORMATTED,

    /**
     * Prints unformatted DJS with minimal whitespace.
     *
     * <p>No formatting options will be preserved.
     */
    DJS,

    /**
     * Pretty prints DJS with whitespace.
     *
     * <p>Some formatting options will be preserved.
     */
    DJS_FORMATTED
}
