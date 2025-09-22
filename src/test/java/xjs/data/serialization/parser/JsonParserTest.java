package xjs.data.serialization.parser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.writer.JsonWriter;
import xjs.data.serialization.writer.JsonWriterOptions;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class JsonParserTest extends CommonParserTest {

    @BeforeAll
    static void setup() {
        JsonContext.setEol("\n");
    }

    @AfterAll
    static void teardown() {
        JsonContext.setEol(System.lineSeparator());
    }

    @Test
    public void parse_doesNotTolerate_trailingCommas() {
        assertThrows(SyntaxException.class,
            () -> this.parse("[1,2,3,]"));
    }

    @Test
    public void parse_doesNotTolerate_leadingCommas() {
        assertThrows(SyntaxException.class,
            () -> this.parse("[,1,2,3]"));
    }

    @Test
    public void parse_doesNotTolerate_unquotedStrings() {
        assertThrows(SyntaxException.class,
            () -> this.parse("{\"\":hello}"));
    }

    @Test
    public void parse_doesNotTolerate_nonStringKeys() {
        assertThrows(SyntaxException.class,
            () -> this.parse("{hello:\"world\"}"));
    }

    @Test
    public void parse_thenRewrite_preservesComplexFormatting() throws IOException {
        final String expected = """
            {
            
              "1":
                1,
              "2":
            
                "2",
              "z":
            
                "y",
            
              "a": [
                3, 4,
                { "5": 5, "6": 6 },
                []
            
            
            
              ],
            
              "a1":
                "block value",
            
              "enter": {
                "level": {
                  "two": [
                    "yes",
                    "yes"
                  ]
                }
              },
            
              "b": "})](*&(*%#&)!",
              "c": { "": "" },
              "d": [ "", "", "" ]
            }""";
        assertEquals(expected, write(new JsonParser(expected).parse()));
    }

    @Override
    protected JsonValue parse(final String json) throws IOException {
        return new JsonParser(json).parse();
    }

    private static String write(final JsonValue value) {
        return write(value, new JsonWriterOptions());
    }

    private static String write(final JsonValue value, final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        final JsonWriter writer =
            options != null ? new JsonWriter(sw, options) : new JsonWriter(sw, false);
        try {
            writer.write(value);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }
}
