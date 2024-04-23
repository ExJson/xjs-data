package xjs.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonReferenceTest {

    @Test
    public void new_wrapsValue() {
        final JsonValue value = Json.value(1234);
        assertSame(value, new JsonReference(value).get());
    }

    @Test
    public void new_toleratesNullValues() {
        assertEquals(JsonLiteral.jsonNull(), new JsonReference(null).get());
    }

    @Test
    public void referent_cannotBeNull() {
        assertNotNull(new JsonReference(null).set(null).get());
    }

    @Test
    public void get_tracksAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.get();
        assertTrue(reference.isAccessed());
    }

    @Test
    public void visit_doesNotTrackAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.getOnly();
        assertFalse(reference.isAccessed());
    }

    @Test
    public void set_tracksAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.set(null);
        assertTrue(reference.isAccessed());
    }

    @Test
    public void mutate_doesNotTrackAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.setOnly(null);
        assertFalse(reference.isAccessed());
    }

    @Test
    public void update_transformsValue() {
        final JsonReference reference = new JsonReference(null);
        reference.apply(JsonValue::intoArray);
        assertTrue(reference.get().isArray());
    }

    @Test
    public void update_tracksAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.apply(JsonValue::intoArray);
        assertTrue(reference.isAccessed());
    }

    @Test
    public void apply_transformsValue() {
        final JsonReference reference = new JsonReference(null);
        reference.applyOnly(JsonValue::intoArray);
        assertTrue(reference.get().isArray());
    }

    @Test
    public void apply_doesNotTrackAccess() {
        final JsonReference reference = new JsonReference(null);
        reference.applyOnly(JsonValue::intoArray);
        assertFalse(reference.isAccessed());
    }

    @Test
    public void clone_createsNewInstance() {
        final JsonReference reference = new JsonReference(null);
        assertNotSame(reference, reference.copy(false));
    }

    @Test
    public void clone_copiesAccess() {
        final JsonReference reference =
            new JsonReference(null).setAccessed(true);

        final JsonReference clone = reference.copy(true);
        assertEquals(reference.isAccessed(), clone.isAccessed());
    }

    @Test
    public void frozenReference_isImmutable() {
        final JsonReference reference = new JsonReference(null).freeze();

        assertThrows(UnsupportedOperationException.class, () -> reference.set(Json.value(1)));
    }
}
