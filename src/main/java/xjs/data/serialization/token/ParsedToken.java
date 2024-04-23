package xjs.data.serialization.token;

/**
 * Represents any token that has already been parsed as text.
 */
public class ParsedToken extends Token {

    /**
     * The text represented by this token. Authors will need
     * to access this field directly to analyze the token.
     */
    protected String parsed;

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start  The inclusive start index of this token.
     * @param end    The exclusive end index of this token.
     * @param line   The inclusive line number of this token.
     * @param offset The column of the start index.
     * @param type   The type of string represented by the token.
     * @param parsed The un-escaped, parsed text.
     */
    public ParsedToken(
            final int start, final int end, final int line, final int offset,
            final TokenType type, final String parsed) {
        super(start, end, line, offset, type);
        this.parsed = parsed;
    }

    /**
     * Constructs a new Token object to be placed on an AST.
     *
     * @param start    The inclusive start index of this token.
     * @param end      The exclusive end index of this token.
     * @param line     The inclusive line number of this token.
     * @param lastLine The inclusive line number at the end of this token.
     * @param offset   The column of the start index.
     * @param type     The type of string represented by the token.
     * @param parsed   The un-escaped, parsed text.
     */
    public ParsedToken(
            final int start, final int end, final int line, final int lastLine,
            final int offset, final TokenType type, final String parsed) {
        super(start, end, line, lastLine, offset, type);
        this.parsed = parsed;
    }

    /**
     * Constructs a new parsed token with effectively no scope.
     *
     * @param type   The type of token represented by the text.
     * @param parsed The parsed or literal text of the token.
     */
    public ParsedToken(final TokenType type, final String parsed) {
        super(type);
        this.parsed = parsed;
    }

    @Override
    public String parsed(final CharSequence reference) {
        return this.parsed();
    }

    @Override
    public String parsed() {
        return this.parsed;
    }

    @Override
    public boolean hasText() {
        return true;
    }

    @Override
    public ParsedToken intoParsed(final CharSequence reference) {
        return this;
    }

    @Override
    public boolean isText(final CharSequence reference, final String text) {
        return this.parsed().equals(text);
    }

    @Override
    public boolean isText(final String text) {
        return this.parsed().equals(text);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof ParsedToken pt) {
            return this.parsed.equals(pt.parsed) && this.spanEquals(pt);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.type + "(start:" + this.start + ",end:" + this.end + ",line:"
            + this.line + ",lastLine:" + lastLine + ",offset:" + this.offset
            + ",parsed:'" + this.parsed + "')";
    }
}
