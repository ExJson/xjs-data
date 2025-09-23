package xjs.data.serialization;

import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.StringType;
import xjs.data.serialization.token.StringToken;
import xjs.data.serialization.token.SymbolToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonContextTest {
    private final FileSystem fs = Jimfs.newFileSystem();

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
    public void getWriter_canWriteJson_toString() {
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
    public void getWriter_canWriteDjs_toString() {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var expected = """
            a: 1
            b: 2""";
        final var actual = JsonContext.getWriter("djs").stringify(data);
        assertEquals(expected, actual);
    }

    @Test
    public void getParser_canReadJson_fromPath() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var path = this.fs.getPath("file.json");
        try (final var writer = Files.newBufferedWriter(path)) {
            writer.write(data.toString("json"));
        }
        final var json = JsonContext.getParser("json").parse(path);
        assertTrue(data.matches(json));
    }

    @Test
    public void getParser_canReadDjs_fromPath() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var path = this.fs.getPath("file.djs");
        try (final var writer = Files.newBufferedWriter(path)) {
            writer.write(data.toString("djs"));
        }
        final var json = JsonContext.getParser("djs").parse(path);
        assertTrue(data.matches(json));
    }

    @Test
    public void getWriter_canWriteJson_toPath() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var path = this.fs.getPath("file2.json");
        JsonContext.getWriter("json").write(path, data);
        final var json = Files.readString(path);
        assertEquals(data.toString("json"), json);
    }

    @Test
    public void getWriter_canWriteDjs_toPath() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var path = this.fs.getPath("file2.djs");
        JsonContext.getWriter("djs").write(path, data);
        final var json = Files.readString(path);
        assertEquals(data.toString("djs"), json);
    }

    @Test
    public void getWriter_whenGivenWriterDirectly_doesNotCloseWriter() throws IOException {
        final var data = Json.object().add("a", 1).add("b", 2);
        final var path = this.fs.getPath("file3.json");
        try (final var writer = Files.newBufferedWriter(path)) {
            JsonContext.getWriter("json").write(writer, data);
            writer.write("additional text");
        }
        assertEquals(data.toString("json") + "additional text", Files.readString(path));
    }

    @Test
    public void getTokenizer_canReadJsonTokens_fromPath() throws IOException {
        final var data = "{\"hello\":\"world\"}";
        final var path = this.fs.getPath("file3.djs");
        Files.writeString(path, data);
        try (final var stream = JsonContext.getTokenizer("djs").stream(path)) {
            stream.preserveOutput().readToEnd();
            assertEquals(
                container(
                    data,
                    symbol('{', 0, 1),
                    string("hello", 1, 8),
                    symbol(':', 8, 9),
                    string("world", 9, 16),
                    symbol('}', 16, 17)
                ),
                stream);
        }
    }

    @Test
    public void getTokenizer_canReadDjsTokens_fromPath() throws IOException {
        final var data = "{hello,world}";
        final var path = this.fs.getPath("file3.djs");
        Files.writeString(path, data);
        try (final var stream = JsonContext.getTokenizer("djs").stream(path)) {
            stream.preserveOutput().readToEnd();
            assertEquals(
                container(
                    data,
                    symbol('{', 0, 1),
                    word(1, 6),
                    symbol(',', 6, 7),
                    word(7, 12),
                    symbol('}', 12, 13)
                ),
                stream);
        }
    }

    private static InputStream getIsFromString(String data) {
        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }

    private static Token string(final String parsed, final int s, final int e) {
        return new StringToken(s, e, 0, 0, s, StringType.DOUBLE, parsed);
    }

    private static Token word(final int s, final int e) {
        return new Token(s, e, 0, s, TokenType.WORD);
    }

    private static Token symbol(final char symbol, final int s, final int e) {
        return new SymbolToken(s, e, 0, s, symbol);
    }

    private static Token container(final String data, final Token... tokens) {
        return new TokenStream(0, data.length(), 0, 0, 0, TokenType.OPEN, List.of(tokens));
    }
}
