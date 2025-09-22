package xjs.data.serialization.parser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;

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

    @Override
    protected JsonValue parse(final String json) throws IOException {
        return new JsonParser(PositionTrackingReader.fromString(json)).parse();
    }
}
