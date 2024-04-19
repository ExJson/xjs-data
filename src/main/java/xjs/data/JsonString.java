package xjs.data;

public class JsonString extends JsonValue {

    private final String value;
    private StringType type;

    public JsonString(final String value) {
        this(value, StringType.NONE);
    }

    public JsonString(final String value, final StringType type) {
        this.value = value;
        this.type = type;
    }

    /**
     * Indicates the syntax used when declaring or reprinting this string.
     *
     * @return The type of string, i.e. single, double, or triple quoted.
     */
    public StringType getStringType() {
        return this.type;
    }

    /**
     * Updates the syntax used when reprinting this string.
     *
     * @param type The new string syntax.
     * @return <code>this</code>, for method chaining.
     */
    public JsonString setStringType(final StringType type) {
        this.type = type;
        return this;
    }

    /**
     * Override allowing this method to preserve {@link #type} when copying
     * metadata.
     *
     * @param other Any other value being copied out of.
     * @return <code>this</code>, for method chaining.
     */
    @Override
    public JsonString setDefaultMetadata(final JsonValue other) {
        if (other instanceof JsonString) {
            final JsonString s = (JsonString) other;
            if (this.type == StringType.NONE) this.type = s.type;
        }
        return (JsonString) super.setDefaultMetadata(other);
    }

    @Override
    public JsonType getType() {
        return JsonType.STRING;
    }

    @Override
    public String unwrap() {
        return this.value;
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public String asString() {
        return this.value;
    }

    @Override // tbd
    public double intoDouble() {
        return this.value.length();
    }

    @Override
    public JsonString copy(final int options) {
        final JsonString copy = new JsonString(this.value);
        if ((options & JsonCopy.FORMATTING) == JsonCopy.FORMATTING) {
            copy.type = this.type;
        }
        return withMetadata(copy, this, options);
    }

    @Override
    protected boolean matchesMetadata(final JsonValue other) {
        if (other instanceof JsonString) {
            return this.type.equals(((JsonString) other).type)
                && super.matchesMetadata(other);
        }
        return false;
    }
}
