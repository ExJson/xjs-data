package xjs.data;

import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonObject;
import xjs.data.JsonValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonValueTest {

    @Test
    public void valueOf_wrapsInteger() {
        assertEquals(12345678901L, Json.value(12345678901L).asLong());
    }

    @Test
    public void valueOf_wrapsDecimal() {
        assertEquals(1.2345678901, Json.value(1.2345678901).asDouble());
    }

    @Test
    public void valueOf_wrapsString() {
        assertEquals("Hello, World!", Json.value("Hello, World!").asString());
    }

    @Test
    public void valueOf_wrapsBoolean() {
        assertTrue(Json.value(true).asBoolean());
    }

    @Test
    public void valueOf_toleratesNullValues() {
        assertEquals(JsonLiteral.jsonNull(), Json.value(null));
    }

    @Test
    public void valueOf_returnsPrimitiveWrappers() {
        assertTrue(Json.value("value").isPrimitive());
    }

    @Test
    public void nonnull_wrapsWhenNull() {
        assertEquals(JsonLiteral.jsonNull(), Json.nonnull(null));
    }

    @Test
    public void nonnull_doesNotWrapWhenNonnull() {
        final JsonValue value = Json.value("value");
        assertSame(value, Json.nonnull(value));
    }

    @Test
    public void asType_doesNotSupportConversion() {
        assertThrows(UnsupportedOperationException.class,
            () -> Json.value("inconvertible").asInt());
    }

    @Test
    public void intoType_doesSupportTypeConversion() {
        assertEquals("12345".length(), Json.value("12345").intoInt());
    }

    @Test
    public void intoArray_generatesArray() {
        assertEquals(new JsonArray().add(1), Json.value(1).intoArray());
    }

    @Test
    public void intoObject_generatesObject() {
        assertEquals(new JsonObject().add("value", 1), Json.value(1).intoObject());
    }
}
