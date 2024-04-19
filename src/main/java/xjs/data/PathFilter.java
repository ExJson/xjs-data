package xjs.data;

/**
 * A filter used for highlighting diagnostic paths inside a {@link JsonContainer}.
 */
public enum PathFilter {

    /**
     * Selects the paths of any values which have been {@link JsonReference#get accessed}.
     */
    USED,

    /**
     * Selects the paths of any values which have not been {@link JsonReference#get accessed}.
     */
    UNUSED,

    /**
     * Selects all possible paths inside a {@link JsonContainer}.
     */
    ALL;

    /**
     * Indicates whether the input value should be included in the path output.
     *
     * @param reference The reference being inspected.
     * @return <code>true</code>, if this matches the filter.
     */
    public boolean test(final JsonReference reference) {
        return this == ALL || this == USED == reference.isAccessed();
    }
}
