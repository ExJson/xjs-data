package xjs.data.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonLiteral;
import xjs.data.JsonObject;
import xjs.data.JsonString;
import xjs.data.JsonValue;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JsonParser implements ValueParser {
    private final PositionTrackingReader reader;

    public JsonParser(final String text) {
        this.reader = PositionTrackingReader.fromString(text);
    }

    public JsonParser(final File file) throws IOException {
        this.reader = PositionTrackingReader.fromIs(
            new FileInputStream(file));
    }

    public JsonParser(final File file, final int bufferSize) throws IOException {
        this.reader = PositionTrackingReader.fromIs(
            new FileInputStream(file), bufferSize, false);
    }

    public @NotNull JsonValue parse() throws IOException {
        this.reader.skipWhitespace();
        final int linesAbove = this.reader.linesSkipped;
        final JsonValue result =
            this.readValue().setLinesAbove(linesAbove);
        this.reader.skipWhitespace();
        if (!this.reader.isEndOfText()) {
            throw this.reader.unexpected();
        }
        return result;
    }

    protected JsonValue readValue() throws IOException {
        return switch (this.reader.current) {
            case 'n' -> this.readNull();
            case 't' -> this.readTrue();
            case 'f' -> this.readFalse();
            case '"' -> this.readString();
            case '[' -> this.readArray();
            case '{' -> this.readObject();
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> Json.value(this.reader.readNumber());
            default -> throw this.reader.expected("value");
        };
    }

    protected JsonValue readNull() throws IOException {
        this.reader.read();
        this.reader.expect('u');
        this.reader.expect('l');
        this.reader.expect('l');
        return JsonLiteral.jsonNull();
    }

    protected JsonValue readTrue() throws IOException {
        this.reader.read();
        this.reader.expect('r');
        this.reader.expect('u');
        this.reader.expect('e');
        return JsonLiteral.jsonTrue();
    }

    protected JsonValue readFalse() throws IOException {
        this.reader.read();
        this.reader.expect('a');
        this.reader.expect('l');
        this.reader.expect('s');
        this.reader.expect('e');
        return JsonLiteral.jsonFalse();
    }

    protected JsonValue readString() throws IOException {
        return new JsonString(this.reader.readQuoted('"'));
    }

    protected JsonArray readArray() throws IOException {
        this.reader.read();
        final JsonArray array = new JsonArray();
        this.reader.skipWhitespace();
        if (this.reader.readIf(']')) {
            return array;
        }
        do {
            this.reader.skipWhitespace(false);
            final int linesAbove = this.reader.linesSkipped;
            array.add(this.readValue().setLinesAbove(linesAbove));
            this.reader.skipWhitespace();
        } while (this.reader.readIf(','));
        if (!this.reader.readIf(']')) {
            throw this.reader.expected("',' or ']'");
        }
        return (JsonArray) array.setLinesTrailing(this.reader.linesSkipped);
    }

    protected JsonObject readObject() throws IOException {
        this.reader.read();
        final JsonObject object = new JsonObject();
        this.reader.skipWhitespace();
        if (this.reader.readIf('}')) {
            return object;
        }
        do {
            this.reader.skipWhitespace(false);
            final int linesAbove = this.reader.linesSkipped;
            final String key = this.readKey();
            this.reader.skipWhitespace();
            this.reader.expect(':');
            this.reader.skipWhitespace();
            final int linesBetween = this.reader.linesSkipped;
            object.add(key,
                this.readValue()
                    .setLinesAbove(linesAbove)
                    .setLinesBetween(linesBetween));
            this.reader.skipWhitespace();
        } while (this.reader.readIf(','));
        if (!this.reader.readIf('}')) {
            throw this.reader.expected("',' or '}'");
        }
        return (JsonObject) object.setLinesTrailing(this.reader.linesSkipped);
    }

    protected String readKey() throws IOException {
        if (this.reader.current != '"') {
            throw this.reader.expected("key");
        }
        return this.reader.readQuoted('"');
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}
