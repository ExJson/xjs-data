package xjs.data.serialization.writer;

import org.jetbrains.annotations.Nullable;
import xjs.data.JsonValue;

import java.io.IOException;
import java.io.Writer;

public class JsonWriter extends ElementWriter {

    public JsonWriter(final Writer writer, final @Nullable JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    protected void write() throws IOException {
        this.writeAbove();
        this.writeValue();
    }

    protected void writeValue() throws IOException {
        final JsonValue value = this.current();

        switch (value.getType()) {
            case OBJECT:
                this.writeObject();
                break;
            case ARRAY:
                this.writeArray();
                break;
            case NUMBER:
                this.writeNumber(value.asDouble());
                break;
            case STRING:
                this.writeQuoted(value.asString(), '"');
                break;
            default:
                this.tw.write(value.toString());
        }
    }

    protected void writeObject() throws IOException {
        this.open('{');
        while (this.current != null) {
            this.writeNextMember();
            this.next();
        }
        this.close('}');
    }

    protected void writeNextMember() throws IOException {
        this.writeAbove();
        this.writeQuoted(this.key(), '"');
        this.tw.write(':');
        this.writeBetween();
        this.writeValue();
        this.delimit();
    }

    protected void writeArray() throws IOException {
        this.open('[');
        while (this.current != null) {
            this.writeNextElement();
            this.next();
        }
        this.close(']');
    }

    protected void writeNextElement() throws IOException {
        this.writeAbove();
        this.writeValue();
        this.delimit();
    }
}
