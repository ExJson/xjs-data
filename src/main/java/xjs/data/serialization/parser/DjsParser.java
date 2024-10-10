package xjs.data.serialization.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.data.comments.CommentType;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonLiteral;
import xjs.data.JsonNumber;
import xjs.data.JsonObject;
import xjs.data.JsonString;
import xjs.data.JsonValue;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.DjsTokenizer;
import xjs.data.serialization.token.NumberToken;
import xjs.data.serialization.token.StringToken;
import xjs.data.serialization.token.SymbolToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;
import xjs.data.serialization.util.PositionTrackingReader;

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
        this(DjsTokenizer.stream(new FileInputStream(file)));
    }

    /**
     * Constructs the parser from raw text data in DJS format.
     *
     * @param text The JSON text in DJS format.
     */
    public DjsParser(final String text) {
        this(DjsTokenizer.stream(text));
    }

    /**
     * Constructs the parser from any {@link PositionTrackingReader}.
     *
     * @param reader The source of DJS data.
     */
    public DjsParser(final PositionTrackingReader reader) throws IOException {
        this(DjsTokenizer.stream(reader));
    }

    /**
     * Constructs the parser from a know set of tokens in DJS format.
     *
     * @param root The root token container.
     */
    public DjsParser(final TokenStream root) {
        super(root);
    }

    @Override
    public @NotNull JsonValue parse() {
        if (this.root.type() == TokenType.OPEN) {
            this.read();
        }
        this.readWhitespace();
        if (this.isEndOfContainer() || this.isOpenRoot()) {
            return this.readOpenRoot();
        }
        return this.readClosedRoot();
    }

    protected boolean isOpenRoot() {
        final TokenType type = this.current.type();
        if (type == TokenType.SYMBOL) { // punctuation
            return false;
        }
        final Token peek = this.peekWhitespace();
        if (peek == null) {
            return false;
        }
        return peek.isSymbol(':');
    }

    protected @Nullable Token peekWhitespace() {
        Token peek = this.iterator.peek();
        int peekAmount = 1;
        while (peek != null) {
            switch (peek.type()) {
                case BREAK:
                case COMMENT:
                    peek = this.iterator.peek(++peekAmount);
                    break;
                default:
                    return peek;
            }
        }
        return null;
    }

    protected JsonObject readOpenRoot() {
        final JsonObject object = new JsonObject();
        this.readAboveOpenRoot(object);
        while (true) {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                break;
            }
            this.readNextMember(object);
        }
        if (!object.isEmpty()) {
            final JsonValue top = object.get(0);
            if (top.getLinesAbove() == 0) {
                // allow these to auto-format with other writers
                top.setLinesAbove(-1);
            }
        }
        this.readBottom();
        return this.takeFormatting(object);
    }

    protected JsonValue readClosedRoot() {
        this.readAbove();
        final JsonValue result = this.readValue();
        this.readAfter();
        this.readBottom();
        return this.takeFormatting(result);
    }

    protected JsonValue readValue() {
        if (this.current.isSymbol('{')) {
            return this.readObject();
        } else if (this.current.isSymbol('[')) {
            return this.readArray();
        }
        final JsonValue value = this.readSingle();
        this.read();
        return value;
    }

    protected JsonObject readObject() {
        final JsonObject object = new JsonObject();
        if (!this.open('{', '}')) {
            return this.close(object, '}');
        }
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer('}')) {
                return this.close(object, '}');
            }
        } while (this.readNextMember(object));
        return this.close(object, '}');
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
        final TokenType type = t.type();
        if (this.isLegalKeyType(type)) {
            final Token peek = this.peekWhitespace();
            if (peek != null && this.isLegalKeyType(peek.type())) {
                // purely to provide useful errors (hjson can be tricky)
                throw this.whitespaceInKey();
            }
            this.read();
            return t.parsed();
        } else if (t.isSymbol(':')) {
            throw this.emptyKey();
        } else if (t.hasText()) {
            throw this.illegalToken(t.parsed());
        } else if (t instanceof SymbolToken s) {
            throw this.punctuationInKey(s.symbol);
        }
        throw this.illegalToken(type.name());
    }

    protected JsonArray readArray() {
        final JsonArray array = new JsonArray();
        if (!this.open('[', ']')) {
            return this.close(array, ']');
        }
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer(']')) {
                return this.close(array, ']');
            }
        } while (this.readNextElement(array));
        return this.close(array, ']');
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
        if (!t.hasText()) {
            if (t.isSymbol(',')) {
                throw this.leadingDelimiter();
            } else if (t instanceof SymbolToken s) {
                throw this.punctuationInValue(s.symbol);
            } else if (this.isEndOfContainer()) {
                throw this.endOfContainer();
            }
            throw this.unexpected(t.type().name());
        }
        final String text = t.parsed();
        return switch (text) {
            case "infinity" -> new JsonNumber(Double.POSITIVE_INFINITY);
            case "-infinity" -> new JsonNumber(Double.NEGATIVE_INFINITY);
            case "true" -> JsonLiteral.jsonTrue();
            case "false" -> JsonLiteral.jsonFalse();
            case "null" -> JsonLiteral.jsonNull();
            case "" -> throw this.expected("tokens");
            default -> throw this.illegalToken(text);
        };
    }

    protected boolean isLegalKeyType(final TokenType type) {
        return type == TokenType.STRING || type == TokenType.WORD || type == TokenType.NUMBER;
    }

    protected SyntaxException emptyKey() {
        return this.expected("key (for an empty key name use quotes)");
    }

    protected SyntaxException whitespaceInKey() {
        return this.unexpected("whitespace in key (use quotes to include)");
    }

    protected SyntaxException punctuationInKey(final char c) {
        return this.unexpected("punctuation ('" + c + "') in key (use quotes to include)");
    }

    protected SyntaxException leadingDelimiter() {
        return this.unexpected("leading delimiter (use quotes to include): ','");
    }

    protected SyntaxException punctuationInValue(final char c) {
        return this.unexpected("punctuation ('" + c + "') in value (use quotes to include)");
    }

    protected SyntaxException endOfContainer() {
        return this.unexpected("end of container when expecting a value (use empty double quotes for empty string)");
    }

    @Override
    public void close() throws IOException {
        this.root.close();
    }
}
