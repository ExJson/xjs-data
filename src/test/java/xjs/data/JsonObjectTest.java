package xjs.data;

import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.JsonObject;
import xjs.data.JsonValue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonObjectTest {

    @Test
    public void add_insertsValue() {
        final JsonObject object = new JsonObject();
        object.add("value", 1234);
        final JsonValue value = object.get("value");
        assertNotNull(value);
        assertEquals(1234, value.asInt());
    }

    @Test
    public void add_doesNotTrackAccess() {
        final JsonObject object = new JsonObject();
        object.add("value", 1234);
        final JsonReference reference = object.getReference("value");
        assertNotNull(reference);
        assertFalse(reference.isAccessed());
    }

    @Test
    public void addReference_insertsReference() {
        final JsonObject object = new JsonObject();
        final JsonReference reference = new JsonReference(Json.value(1234));
        object.addReference("value", reference);
        assertSame(reference, object.getReference("value"));
    }

    @Test
    public void addReference_doesNotTrackAccess() {
        final JsonObject object = new JsonObject();
        final JsonReference reference = new JsonReference(Json.value(1234));
        object.addReference("value", reference);
        assertFalse(reference.isAccessed());
    }

    @Test
    public void set_updatesValue() {
        final JsonObject object = new JsonObject().add("value", 1);
        object.set("value", 2);
        final JsonReference reference = object.getReference("value");
        assertNotNull(reference);
        assertTrue(reference.isAccessed());
    }

    @Test
    public void set_preservesMetadata() {
        final JsonObject object =
            new JsonObject().add("0", Json.value(1).setLinesAbove(1));
        object.set("0", 2);
        final JsonValue value = object.get("0");
        assertNotNull(value);
        assertEquals(1, value.getLinesAbove());
    }

    @Test
    public void set_canUpdateMetadata() {
        final JsonObject object =
            new JsonObject().add("0", Json.value(1).setLinesAbove(1));
        object.set("0", Json.value(2).setLinesAbove(0));
        final JsonValue value = object.get("0");
        assertNotNull(value);
        assertEquals(0, value.getLinesAbove());
    }

    @Test
    public void set_toleratesInsertion() {
        final JsonObject object = new JsonObject();
        assertDoesNotThrow(() -> object.set("value", 2));
    }

    @Test
    public void addAll_addsValuesFromOtherObject() {
        final JsonObject object = new JsonObject();
        object.addAll(new JsonObject().add("", 1));
        assertTrue(object.contains(1));
    }

    @Test
    public void frozenObject_isImmutable() {
        final JsonObject object = (JsonObject) new JsonObject().add("key", "value").freeze();

        assertThrows(UnsupportedOperationException.class, () -> object.add("k2", "v2"));
        assertThrows(UnsupportedOperationException.class, () -> object.set("key", false));
    }
}
