package xjs.data.serialization;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.data.Json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonContextTest {

    @BeforeAll
    static void setup() {
        JsonContext.setEol("\n");
    }

    @AfterAll
    static void tearDown() {
        JsonContext.setEol(System.lineSeparator());
    }

    @Test
    public void getParser_canReadJson_fromString() {
        final var data = """
            { "a": 1, "b": 2 }
            """;
        final var expected = Json.object().add("a", 1).add("b", 2);
        final var actual = JsonContext.getParser("json").parse(data);
        assertEquals(expected.unformatted(), actual.unformatted());
    }

    @Test
    public void getParser_canReadJson_fromInputStream() throws IOException {
        final var data = getIsFromString("""
            { "a": 1, "b": 2 }
            """);
        final var expected = Json.object().add("a", 1).add("b", 2);
        final var actual = JsonContext.getParser("json").parse(data);
        assertEquals(expected.unformatted(), actual.unformatted());
    }

    @Test
    public void getParser_canReadDjs_fromString() {
        final var data = """
            { a: 1, b: 2 }
            """;
        final var expected = Json.object().add("a", 1).add("b", 2);
        final var actual = JsonContext.getParser("djs").parse(data);
        assertEquals(expected.unformatted(), actual.unformatted());
    }

    @Test
    public void getParser_canReadDjs_fromInputStream() throws IOException {
        final var data = getIsFromString("""
            { a: 1, b: 2 }
            """);
        final var expected = Json.object().add("a", 1).add("b", 2);
        final var actual = JsonContext.getParser("djs").parse(data);
        assertEquals(expected.unformatted(), actual.unformatted());
    }

    @Test
    public void getWriter_canWriteJson_toString() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var expected = """
            {
              "a": 1,
              "b": 2
            }""";
        final var actual = JsonContext.getWriter("json").stringify(data);
        assertEquals(expected, actual);
    }

    @Test
    public void getWriter_canWriteDjs_toString() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var expected = """
            a: 1
            b: 2""";
        final var actual = JsonContext.getWriter("djs").stringify(data);
        assertEquals(expected, actual);
    }

    private static InputStream getIsFromString(String data) {
        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }
}
