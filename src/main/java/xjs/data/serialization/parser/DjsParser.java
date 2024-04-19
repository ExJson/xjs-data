package xjs.data.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.data.comments.CommentType;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonLiteral;
import xjs.data.JsonObject;
import xjs.data.JsonString;
import xjs.data.JsonValue;
import xjs.data.serialization.token.ContainerToken;
import xjs.data.serialization.token.NumberToken;
import xjs.data.serialization.token.StringToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenType;
import xjs.data.serialization.token.Tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Parses a stream of {@link Token tokens} in DJS format into a {@link
 * JsonObject JSON object}.
 */
public class DjsParser extends CommentedTokenParser {

    /**
     * Constructs the parser when given a file in DJS format.
     *
     * @param file The file containing DJS data.
     * @throws IOException If an error occurs when reading the file.
     */
    public DjsParser(final File file) throws IOException {
        this(Tokenizer.containerize(new FileInputStream(file)));
    }

    /**
     * Constructs the parser from raw text data in DJS format.
     *
     * @param text The JSON text in DJS format.
     */
    public DjsParser(final String text) {
        this(Tokenizer.containerize(text));
    }

    /**
     * Constructs the parser from a know set of tokens in DJS format.
     *
     * @param root The root token container.
     */
    public DjsParser(final ContainerToken root) {
        super(root);
    }

    @Override
    public @NotNull JsonValue parse() {
        final ContainerToken rootContainer = (ContainerToken) this.root;
        if (rootContainer.lookup(':', false) != null) {
            return this.readOpenRoot();
        }
        return this.readClosedRoot();
    }

    protected JsonObject readOpenRoot() {
        final JsonObject object = new JsonObject();
        this.read();
        this.readAboveOpenRoot(object);
        while (true) {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                break;
            }
            this.readNextMember(object);
        }
        this.readBottom();
        return this.takeFormatting(object);
    }

    protected JsonValue readClosedRoot() {
        if (this.current.type() == TokenType.OPEN) {
            this.read();
        }
        this.readAbove();
        final JsonValue result = this.readValue();

        this.readAfter();
        this.readBottom();
        return this.takeFormatting(result);
    }

    protected JsonValue readValue() {
        switch (this.current.type()) {
            case BRACKETS:
                return this.readArray();
            case BRACES:
                return this.readObject();
            default:
                final JsonValue value = this.readSingle();
                this.read();
                return value;
        }
    }

    protected JsonObject readObject() {
        final JsonObject object = new JsonObject();
        if (!this.open()) {
            return this.close(object);
        }
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                return this.close(object);
            }
        } while (this.readNextMember(object));
        return this.close(object);
    }

    protected boolean readNextMember(final JsonObject object) {
        this.setAbove();

        final String key = this.readKey();

        this.readBetween(':');

        final JsonValue value = this.readValue();
        object.add(key, value);

        final boolean delimiter = this.readDelimiter();
        this.takeFormatting(value);
        return delimiter;
    }

    protected String readKey() {
        final Token t = this.current;
        if (t instanceof StringToken) {
            this.read();
            return t.parsed();
        } else if (t.type() == TokenType.WORD || t.type() == TokenType.NUMBER) {
            this.read();
            return this.iterator.getText(t.start(), t.end());
        } else if (t.isSymbol(':')) {
            throw this.expected("key before ':'");
        }
        throw this.illegalToken(this.iterator.getText(t.start(), t.end()));
    }

    protected JsonArray readArray() {
        final JsonArray array = new JsonArray();
        if (!this.open()) {
            return this.close(array);
        }
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                return this.close(array);
            }
        } while (this.readNextElement(array));
        return this.close(array);
    }

    protected boolean readNextElement(final JsonArray array) {
        this.setAbove();

        final JsonValue value = this.readValue();
        array.add(value);

        final boolean delimiter = this.readDelimiter();
        this.takeFormatting(value);
        return delimiter;
    }

    protected boolean readDelimiter() {
        this.readLineWhitespace();
        if (this.readIf(',')) {
            this.readLineWhitespace();
            this.readNl();
            this.setComment(CommentType.EOL);
            return true;
        } else if (this.readNl()) {
            this.setComment(CommentType.EOL);
            this.readWhitespace(false);
            this.readIf(',');
            return true;
        } else if (this.isEndOfText()) {
            this.setComment(CommentType.EOL);
        }
        return false;
    }

    protected JsonValue readSingle() {
        final Token t = this.current;
        if (t instanceof NumberToken n) {
            return Json.value(n.number);
        } else if (t instanceof StringToken s) {
            return new JsonString(s.parsed(), s.stringType());
        }
        final String text = this.iterator.getText(t.start(), t.end());
        return switch (text) {
            case "true" -> JsonLiteral.jsonTrue();
            case "false" -> JsonLiteral.jsonFalse();
            case "null" -> JsonLiteral.jsonNull();
            case "," -> throw this.unexpected("leading delimiter: ','");
            case "" -> throw this.expected("tokens");
            default -> throw this.illegalToken(text);
        };
    }

    @Override
    public void close() throws IOException {
        this.iterator.getParent().close();
    }
}
