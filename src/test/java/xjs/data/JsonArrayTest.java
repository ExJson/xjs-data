package xjs.data;

import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonArrayTest {

    @Test
    public void add_insertsValue() {
        final JsonArray array = new JsonArray();
        array.add(1234);
        assertEquals(1234, array.get(0).asInt());
    }

    @Test
    public void add_doesNotTrackAccess() {
        final JsonArray array = new JsonArray();
        array.add(1234);
        assertFalse(array.getReference(0).isAccessed());
    }

    @Test
    public void addReference_insertsReference() {
        final JsonArray array = new JsonArray();
        final JsonReference reference = new JsonReference(Json.value(1234));
        array.addReference(reference);
        assertSame(reference, array.getReference(0));
    }

    @Test
    public void addReference_doesNotTrackAccess() {
        final JsonArray array = new JsonArray();
        final JsonReference reference = new JsonReference(Json.value(1234));
        array.addReference(reference);
        assertFalse(array.getReference(0).isAccessed());
    }

    @Test
    public void set_updatesValue() {
        final JsonArray array = new JsonArray().add(1);
        array.set(0, 2);
        assertTrue(array.getReference(0).isAccessed());
    }

    @Test
    public void set_preservesMetadata() {
        final JsonArray array =
            new JsonArray().add(Json.value(1).setLinesAbove(1));
        array.set(0, 2);
        assertEquals(1, array.get(0).getLinesAbove());
    }

    @Test
    public void set_canUpdateMetadata() {
        final JsonArray array =
            new JsonArray().add(Json.value(1).setLinesAbove(1));
        array.set(0, Json.value(2).setLinesAbove(0));
        assertEquals(0, array.get(0).getLinesAbove());
    }

    @Test
    public void set_doesNotTolerateInsertion() {
        final JsonArray array = new JsonArray();
        assertThrows(IndexOutOfBoundsException.class,
            () -> array.set(0, 2));
    }

    @Test
    public void addAll_addsFromAnyContainer() {
        final JsonArray array = new JsonArray();
        array.addAll(new JsonObject().add("", 1));
        assertTrue(array.contains(1));
    }

    @Test
    public void frozenArray_isImmutable() {
        final JsonArray array = new JsonArray().add(1).freeze(false);

        assertThrows(UnsupportedOperationException.class, () -> array.add(2));
        assertThrows(UnsupportedOperationException.class, () -> array.set(0, false));
    }
}
