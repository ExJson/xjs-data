package xjs.data;

public class JsonLiteral extends JsonValue {

    private final Value value;

    private JsonLiteral(final Value value) {
        this.value = value;
    }

    public static JsonLiteral jsonTrue() {
        return new JsonLiteral(Value.TRUE);
    }

    public static JsonLiteral jsonFalse() {
        return new JsonLiteral(Value.FALSE);
    }

    public static JsonLiteral jsonNull() {
        return new JsonLiteral(Value.NULL);
    }

    @Override
    public JsonType getType() {
        if (this.value == Value.NULL) {
            return JsonType.NULL;
        }
        return JsonType.BOOLEAN;
    }

    @Override
    public Object unwrap() {
        if (this.value == Value.TRUE) return true;
        if (this.value == Value.FALSE) return false;
        return null;
    }

    @Override
    public boolean isBoolean() {
        return this.value != Value.NULL;
    }

    @Override
    public boolean isTrue() {
        return this.value == Value.TRUE;
    }

    @Override
    public boolean isFalse() {
        return this.value == Value.FALSE;
    }

    @Override
    public boolean isNull() {
        return this.value == Value.NULL;
    }

    @Override
    public boolean asBoolean() {
        return switch (this.value) {
            case TRUE -> true;
            case FALSE -> false;
            default -> super.asBoolean();
        };
    }

    @Override
    public double intoDouble() {
        return this.isTrue() ? 1.0 : 0.0;
    }

    @Override
    public JsonLiteral copy(final int options) {
        return withMetadata(new JsonLiteral(this.value), this, options);
    }

    @Override
    public String toString() {
        return this.value.token;
    }

    private enum Value {
        TRUE,
        FALSE,
        NULL;

        final String token = this.name().toLowerCase();
    }
}
