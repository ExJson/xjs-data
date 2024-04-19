package xjs.data;

/**
 * Represents the type of value being wrapped by a {@link JsonValue}.
 *
 * <p>This enumeration is mostly useful in a <code>switch</code> statement
 * when performing a variety of actions depending on the type of value.
 *
 * <p>For example, to implement a very specific algorithm to coerce JSON
 * values into numbers:
 *
 * <pre>{@code
 *   switch(value.getType()) {
 *     case STRING: return Double.parseDouble(value.asString());
 *     case NUMBER: return value.asDouble();
 *     case BOOLEAN: return value.asBoolean() ? 1.0 : 0.0;
 *     case NULL: return 0.0;
 *     case OBJECT:
 *     case ARRAY:
 *       final JsonContainer container = value.asContainer();
 *       if (container.size() > 0 && container.get(0).isNumber()) {
 *         return container.get(0).asDouble();
 *       }
 *       return container.size();
 *     default: throw new UnsupportedOperationException();
 *   }
 * }</pre>
 */
public enum JsonType {

    /**
     * Represents a string value.
     */
    STRING,

    /**
     * Represents a number value.
     */
    NUMBER,

    /**
     * Represents a map containing many values.
     */
    OBJECT,

    /**
     * Represents an array containing many values.
     */
    ARRAY,

    /**
     * Represents a boolean value.
     */
    BOOLEAN,

    /**
     * Represents a definite <code>null</code> value.
     */
    NULL
}
