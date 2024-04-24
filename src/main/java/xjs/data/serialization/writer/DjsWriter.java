package xjs.data.serialization.writer;

import xjs.data.JsonValue;
import xjs.data.StringType;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class DjsWriter extends CommentedElementWriter {

    public DjsWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public DjsWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public DjsWriter(final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
    }

    public DjsWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    protected void write() throws IOException {
        final JsonValue value = this.current();
        if (this.omitRootBraces
                && value.isObject()
                && !value.asObject().isEmpty()) {
            this.writeOpenRoot();
        } else {
            this.writeClosedRoot();
        }
    }

    protected void writeOpenRoot() throws IOException {
        this.level = -1;
        this.writeAbove();
        this.open((char) 0);
        while (this.current != null) {
            this.writeNextMember();
            this.next();
        }
        this.close((char) 0);
        this.writeFooter();
    }

    protected void writeClosedRoot() throws IOException {
        this.writeAbove();
        this.writeValue();
        this.writeAfter();
        this.writeFooter();
    }

    protected void writeValue() throws IOException {
        final JsonValue value = this.current();

        switch (value.getType()) {
            case OBJECT -> this.writeObject();
            case ARRAY -> this.writeArray();
            case NUMBER -> this.writeNumber(value.asDouble());
            case STRING -> this.writeString(value);
            default -> this.tw.write(value.toString());
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
        this.writeKey();
        this.tw.write(':');
        this.writeBetween();
        this.writeValue();
        this.delimit();
        this.writeAfter();
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
        this.writeAfter();
    }

    @Override
    protected void delimit() throws IOException {
        if (this.peek != null) {
            if (!this.format) {
                this.tw.write(',');
            } else if (this.allowCondense && this.getLinesAbove(this.peek()) == 0) {
                this.tw.write(',');
                this.tw.write(this.separator);
            }
        }
    }

    protected void writeKey() throws IOException {
        final String key = this.key();
        this.writeString(key, this.getKeyType(key));
    }

    protected void writeString(final JsonValue value) throws IOException {
        this.writeString(value.asString(), this.getStringType(value));
    }

    protected void writeString(
            final String value, final StringType type) throws IOException {
        switch (type) {
            case SINGLE -> this.writeQuoted(value, '\'');
            case DOUBLE -> this.writeQuoted(value, '"');
            case MULTI -> this.writeMulti(value);
            case IMPLICIT -> this.tw.write(value); // must be a key
            default -> throw new IllegalStateException("unreachable");
        }
    }

    protected StringType getKeyType(final String key) {
        return StringType.selectKey(key);
    }

    protected StringType getStringType(final JsonValue value) {
        final StringType type = StringType.fromValue(value);
        return this.isLegalStringType(type) ? type : StringType.selectValue(value.asString());
    }

    protected boolean isLegalStringType(final StringType type) {
        return type == StringType.DOUBLE || type == StringType.SINGLE || type == StringType.MULTI;
    }
}
