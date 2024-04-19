package xjs.data.transform;

import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonObject;
import xjs.data.JsonReference;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonCollectorsTest {

    @Test
    public void value_collectsArrayFromValues() {
        final JsonArray collected =
            Stream.of(1, 2, 3)
                .map(Json::value)
                .collect(JsonCollectors.value());
        assertEquals(new JsonArray().add(1).add(2).add(3), collected);
    }

    @Test
    public void reference_collectsArrayFromReferences() {
        final JsonArray collected =
            Stream.of(new JsonReference(null))
                .collect(JsonCollectors.reference());
        assertEquals(new JsonArray().add((String) null), collected);
    }

    @Test
    public void reference_preservesAccess() {
        final JsonArray collected =
            Stream.of(new JsonReference(null).setAccessed(true))
                .collect(JsonCollectors.reference());
        assertTrue(collected.getReference(0).isAccessed());
    }

    @Test
    public void access_collectsFromView() {
        final JsonArray collected =
            new JsonObject().add("value", 1234)
                .stream()
                .collect(JsonCollectors.element());
        assertEquals(new JsonArray().add(1234), collected);
    }

    @Test
    public void toObject_collectsWithMappers() {
        final JsonObject collected =
            Map.of("1", 1, "2", 2, "3", 3)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(JsonCollectors.toObject(Json::value));
        assertEquals(new JsonObject().add("1", 1).add("2", 2).add("3", 3), collected);
    }
}
